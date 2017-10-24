package cluster.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.datastructures.MLJob;
import cluster.simulator.Simulator;
import queue.schedulers.DRFScheduler;
import queue.schedulers.EqualShareScheduler;
import queue.schedulers.MaxMinMemScheduler;
import queue.schedulers.PricingScheduler;
import queue.schedulers.Scheduler;
import queue.schedulers.SpeedUpScheduler;
import cluster.simulator.Main.Globals;

public class QueueScheduler {
  private final static boolean DEBUG = true;

  public Scheduler scheduler;

  public QueueScheduler() {
    switch (Globals.QUEUE_SCHEDULER) {
    case ES:
      scheduler = new EqualShareScheduler();
      break;
    case DRF:
        scheduler = new DRFScheduler();
        break;
    case MaxMinMem:
      scheduler = new MaxMinMemScheduler();
      break;
    case SpeedUp:
      scheduler = new SpeedUpScheduler();
      break;
    case Pricing:
        scheduler = new PricingScheduler();
        break;
    default:
      System.err.println("Unknown sharing policy");
    }
  }

  public void schedule() {
    // compute how much share each queue should get
    scheduler.computeResShare();
  }

  public void adjustShares() {
    List<Integer> unhappyDagsIds = new ArrayList<Integer>();

    final Map<Integer, Resource> unhappyDagsDistFromResShare = new HashMap<Integer, Resource>();
    for (BaseJob dag : Simulator.runningJobs) {
      if (!dag.rsrcQuota.distinct(dag.getRsrcInUse())) {
        continue;
      }

      if (dag.getRsrcInUse().greaterOrEqual(dag.rsrcQuota)) {
        // TODO: do we need to deal with this case: this dag has more resources than fairshare.
      } else {
        Resource farthestFromShare = Resources.subtract(dag.rsrcQuota, dag.getRsrcInUse());
        unhappyDagsIds.add(dag.dagId);
        unhappyDagsDistFromResShare.put(dag.dagId, farthestFromShare);
      }
    }
    Collections.sort(unhappyDagsIds, new Comparator<Integer>() {
      public int compare(Integer arg0, Integer arg1) {
        Resource val0 = unhappyDagsDistFromResShare.get(arg0);
        Resource val1 = unhappyDagsDistFromResShare.get(arg1);
        return val0.compareTo(val1);
      }
    });

    // now try to allocate the available resources to dags in this order
    Resource availRes = Resources.clone(Simulator.cluster.getClusterResAvail());

    for (int dagId : unhappyDagsIds) {
      if (!availRes.greater(new Resource(0.0))) break;

      MLJob dag = Simulator.getDag(dagId);

      Resource rsrcReqTillShare = unhappyDagsDistFromResShare.get(dagId);

      if (availRes.greaterOrEqual(rsrcReqTillShare)) {
        availRes.subtract(rsrcReqTillShare);
      } else {
        Resource toGive = Resources.piecewiseMin(availRes, rsrcReqTillShare);
        dag.rsrcQuota.copy(toGive);
        availRes.subtract(toGive);
      }
    }
  }

  public static void allocateResToQueue(JobQueue q, double[] shares) {
    Resource computedShares = new Resource(shares);
    Resource allocRes = q.getResourceUsage();
    boolean jobAvail = true;
    int remainJobs = q.runningJobsSize();
    while (jobAvail) {
      BaseJob unallocJob = q.getUnallocRunningJob();
      if (unallocJob == null) {
        jobAvail = false;
        break;
      }

      boolean isResAvail = true;
      while (isResAvail) {
        // allocate resource to each task.
        int taskId = unallocJob.getCommingTaskId();
        if (taskId < 0) break;

        Resource remainingRes = Resources.subtract(computedShares, allocRes);

        InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
        Resource gDemand = demand.convertToGPUDemand();
        Resource cDemand = demand.convertToCPUDemand();
        Resource taskDemand = null;
        boolean isCPU = false;
        if (!gDemand.fitsIn(remainingRes)) {
          if (!cDemand.fitsIn(remainingRes)) {
            isResAvail = false;
            jobAvail = false;
            break;
          } else {
            taskDemand = cDemand;
            isCPU = true;
          }
        } else {
          taskDemand = gDemand;
        }

        boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
            unallocJob.duration(taskId), taskDemand);

        if (assigned) {
          allocRes = Resources.sum(allocRes, taskDemand);
          if (isCPU) {
            unallocJob.isCPUUsages.put(taskId, true);
          }

          if (unallocJob.jobStartRunningTime < 0) {
            unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
          }
        } else {
          isResAvail = false;
          jobAvail = false;
          break;
        }
      }
    }
  }

}
