package queue.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import jdk.nashorn.internal.objects.Global;
import cluster.datastructures.BaseJob;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.Method;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;
import cluster.utils.Utils;

public class EqualShareScheduler implements Scheduler {
	private static boolean DEBUG = false;

	private String schedulePolicy;
	// Map<String, Resources> resDemandsQueues = null;

	static Resource clusterTotCapacity = null;

	// implementation idea:
	// 1. for every queue, compute it's total resource demand vector

	public EqualShareScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "EC";
	}

	@Override
	public void computeResShare() {

		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}

		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobArrivalComparator());
		}
		
		// equal share all resources
		equallyAllocate(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
	}
	
	public static void equallyAllocate(Resource resCapacity, List<JobQueue> runningQueues) {
    // init
    Resource consumedRes = new Resource();
    Resource[] userDominantShareArr = new Resource[runningQueues.size()];
    
    int bottleneck = 0;
    int i = 0;
    double[] auxilaryShare = new double[runningQueues.size()];
    int bottleneckRes = 0; 
    for (JobQueue queue : runningQueues) {
      Resource normalizedShare = Resources.divideVector(queue.getResourceUsage(),
          Simulator.cluster.getClusterMaxResAlloc());
      auxilaryShare[i] = 0.0;
      userDominantShareArr[i] = Resources.divide(normalizedShare, queue.getWeight());
      i++;
    }
    
    while (true) {
      // step 1: pick user i with lowest equal fair share
      
      int sMinIdx = Utils.getMinValIdx(userDominantShareArr,bottleneck);
      
      if (sMinIdx < 0) {
        // There are more resources than demand.
        break;
      }
      // D_i demand for the next task
      JobQueue q = runningQueues.get(sMinIdx);
      BaseJob unallocJob = q.getUnallocRunningJob();
      if (unallocJob == null) {
        userDominantShareArr[sMinIdx].resources[bottleneck]= Double.MAX_VALUE;
        continue;
      }

      int taskId = unallocJob.getCommingTaskId();
      Resource allocRes = unallocJob.rsrcDemands(taskId);
      // Like Yarn, assign one single container for the task
      // step 3: if fit, C+D_i <= R, allocate
      Resource temp = Resources.sumRound(consumedRes, allocRes);
      if (resCapacity.greaterOrEqual(temp)) {
        consumedRes = temp;
        q.setRsrcQuota(Resources.sum(q.getRsrcQuota(), q.nextTaskRes()));
        boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
            unallocJob.duration(taskId), allocRes);
        if (assigned) {
          // update userDominantShareArr
          double maxRes = q.getResourceUsage().max()/Globals.CAPACITY;
          userDominantShareArr[sMinIdx].resources[bottleneck] = maxRes / q.getWeight();
          
          if (unallocJob.jobStartRunningTime<0){
            unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
          }
        } else {
          Output.debugln(DEBUG, "[EqualShareScheduler] Cannot assign resource to the task" + taskId
              + " of Job " + unallocJob.dagId + " " + allocRes);
          userDominantShareArr[sMinIdx].resources[bottleneck] = Double.MAX_VALUE;
        }

      } else {
        userDominantShareArr[sMinIdx].resources[bottleneck] = Double.MAX_VALUE;
        // do not allocate to this queue any more
        // break;
      }
    }
  }

	
	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
