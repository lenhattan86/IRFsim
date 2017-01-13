package queue.schedulers;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import jdk.nashorn.internal.objects.Global;
import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.Method;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;
import cluster.utils.Utils;

public class DRFScheduler implements Scheduler {
	private static boolean DEBUG = false;

	private String schedulePolicy;
	// Map<String, Resources> resDemandsQueues = null;

	static Resource clusterTotCapacity = null;

	// implementation idea:
	// 1. for every queue, compute it's total resource demand vector
	// 2. for every queue's resource demand vector, normalize every dimension
	// to the total capacity of the cluster
	// 3. scale a queue's resource demand vector if any dimension is larger than
	// total capacity of the cluster
	// 4. sum up across every dimension across all the resource demand vectors
	// 5. inverse the max sum across dimensions 1 / max_sum
	// 6. the DRF allocation for every job is computed:
	// ResourceDemandVector * 1 / max_sum

	public DRFScheduler() {

		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "DRF";
	}

	// FairShare = 1 / N across all dimensions
	// N - total number of running jobs
	@Override
	public void computeResShare() {
//
//		if (Simulator.CURRENT_TIME >= Globals.DEBUG_START && Simulator.CURRENT_TIME <= Globals.DEBUG_END) {
//			DEBUG = true;
//		} else
//			DEBUG = false;

		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}

		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			Collections.sort((List<BaseDag>) q.getRunningJobs(), new JobArrivalComparator());
		}

		onlineDRFShare(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
	}
	
	//TODO: onlineDRFShare_new is incomplete
  public static void onlineDRFShare_new(Resource resCapacity, List<JobQueue> runningQueues) {
    // init
    Resource consumedRes = Simulator.cluster.getClusterAllocatedRes();
    double[] userDominantShareArr = new double[runningQueues.size()];
    // TODO: consider the allocated share (because of no preemption).
    int i = 0;
    double[] auxilaryShare = new double[runningQueues.size()];
    for (JobQueue queue : runningQueues) {
      Resource normalizedShare = Resources.divideVector(queue.getResourceUsage(),
          clusterTotCapacity);
      if (queue.isLQ && Globals.METHOD.equals(Method.Strict))
        auxilaryShare[i] = -Double.MAX_VALUE;
      else
        auxilaryShare[i] = 0.0;
      userDominantShareArr[i] = Utils.round(normalizedShare.max() / queue.getWeight(), 2)
          + auxilaryShare[i];
      i++;
    }
    while (true) {
      // step 1: pick user i with lowest s_i
      int sMinIdx = Utils.getMinValIdx(userDominantShareArr);
      if (sMinIdx < 0) {
        // There are more resources than demand.
        break;
      }
      // D_i demand for the next task
      JobQueue q = runningQueues.get(sMinIdx);
      BaseDag unallocJob = q.getUnallocRunningJob();
      if (unallocJob == null) {
        userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
        // do not allocate to this queue any more
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
          double maxRes = Resources.divideVector(q.getResourceUsage(),
              clusterTotCapacity).max();
          userDominantShareArr[sMinIdx] = Utils.round(maxRes / q.getWeight(), 2)
              + auxilaryShare[sMinIdx];
          
          if (unallocJob.jobStartRunningTime<0){
            unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
          }
        } else {
          Output.debugln(DEBUG, "[DRFScheduler] Cannot assign resource to the task" + taskId
              + " of Job " + unallocJob.dagId + " " + allocRes);
          userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
        }

      } else {
        userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
        // do not allocate to this queue any more
        // break;
      }
    }
  }


	public static void onlineDRFShare(Resource resCapacity, List<JobQueue> runningQueues) {
		// init
		Resource consumedRes = new Resource();
		double[] userDominantShareArr = new double[runningQueues.size()];
		// TODO: consider the allocated share (because of no preemption).
		int i = 0;
		double[] auxilaryShare = new double[runningQueues.size()];
		for (JobQueue queue : runningQueues) {
			Resource normalizedShare = Resources.divideVector(queue.getResourceUsage(),
			    Simulator.cluster.getClusterMaxResAlloc());
			if (queue.isLQ && Globals.METHOD.equals(Method.Strict))
				auxilaryShare[i] = -1.0;
			else
				auxilaryShare[i] = 0.0;
			userDominantShareArr[i] = normalizedShare.max() / queue.getWeight();
			i++;
		}
		while (true) {
			// step 1: pick user i with lowest s_i
			int sMinIdx = Utils.getMinValIdx(userDominantShareArr);
			if(Globals.METHOD.equals(Method.Strict)){
				double minVal = Double.MAX_VALUE;
				int minIdx = -1;
				for (int idx=0; idx<auxilaryShare.length; idx++){
					if(auxilaryShare[idx]<0 && userDominantShareArr[idx]<minVal){
						minVal = userDominantShareArr[idx];
						minIdx = idx;
					}
				}
				if (minIdx>=0)
					sMinIdx = minIdx;
			}
			if (sMinIdx < 0) {
				// There are more resources than demand.
				break;
			}
			// D_i demand for the next task
			JobQueue q = runningQueues.get(sMinIdx);
			BaseDag unallocJob = q.getUnallocRunningJob();
			if (unallocJob == null) {
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				// do not allocate to this queue any more
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
					userDominantShareArr[sMinIdx] = maxRes / q.getWeight();
					
					if (unallocJob.jobStartRunningTime<0){
					  unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
					}
				} else {
					Output.debugln(DEBUG, "[DRFScheduler] Cannot assign resource to the task" + taskId
					    + " of Job " + unallocJob.dagId + " " + allocRes);
					userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				}

			} else {
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				// do not allocate to this queue any more
				// break;
			}
		}
	}

	public void computeDRFShare(Resource flexibleResources, List<JobQueue> runningQueues) {
		HashMap<String, Resource> resDemandsQueues = new HashMap<String, Resource>();
		double factor = 0.0;
		for (JobQueue q : runningQueues) {
			factor += q.getWeight();
		}

		for (JobQueue q : runningQueues) {
			// 1. compute it's avg. resource demand vector it not already computed
			Resource avgResDemandDag = q.getMaxDemand();
			if (!Globals.METHOD.equals(Method.Strict)) // workaround for Strict
				avgResDemandDag.divide(factor);

			// 2. normalize every dimension to the total capacity of the cluster
			avgResDemandDag.divide(flexibleResources);

			// 3. scale the resource demand vector to the max resource
			avgResDemandDag.divide(avgResDemandDag.max());
			if (!Globals.METHOD.equals(Method.Strict)) // workaround for Strict
				avgResDemandDag.multiply(q.getWeight());
			else {
				double ratio = Utils.round(q.getWeight() / factor, 3);
				avgResDemandDag.multiply(ratio);
			}

			// avgResDemandDag.round(Globals.TOLERANT_ERROR);

			Resource resDemand = Resources.piecewiseMin(q.getMaxDemand(), avgResDemandDag); // increase
			                                                                                 // utilization.

			resDemandsQueues.put(q.getQueueName(), resDemand);
		}

		// 4. sum it up across every dimension
		Resource sumDemandsRunQueues = new Resource(0.0);
		for (JobQueue q : runningQueues) {
			sumDemandsRunQueues.addWith(resDemandsQueues.get(q.getQueueName()));
		}

		// 5. find the max sum
		int maxIdx = sumDemandsRunQueues.idOfMaxResource();
		double drfShare = flexibleResources.resource(maxIdx) / sumDemandsRunQueues.max();

		for (JobQueue q : runningQueues) {
			Resource drfQuota = Resources.clone(resDemandsQueues.get(q.getQueueName()));
			drfQuota.multiply(drfShare);
			// drfQuota.round(Globals.TOLERANT_ERROR);
			q.setRsrcQuota(drfQuota);
			Output.debugln(DEBUG,
			    "[DRFScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
		}
	}

	public void fairShareForJobs(JobQueue q, Resource availRes) {
		boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
		if (!fit) {
			Resource newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
			q.setRsrcQuota(newQuota);
		}
		Output.debugln(DEBUG, "[DRFScheduler] drf share allocated to queue:" + q.getQueueName() + " "
		    + q.getRsrcQuota());
		q.receivedResourcesList.add(q.getRsrcQuota());

		Resource remain = q.getRsrcQuota();
		List<BaseDag> runningJobs = new LinkedList<BaseDag>(q.getRunningJobs());
		Collections.sort(runningJobs, new JobArrivalComparator());

		for (BaseDag job : runningJobs) {
			Resource rsShare = Resources.divide(q.getRsrcQuota(), q.runningJobsSize());
			// rsShare.floor();
			job.rsrcQuota = rsShare;
			remain.subtract(rsShare);
		}
		// shareRemainRes(q, remain);
		Output.debugln(DEBUG,
		    "[DRFScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getJobsQuota());
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
	}

	public void fifoShareForJobs(JobQueue q, Resource availRes) {
		boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
		if (!fit) {
			Resource newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
			q.setRsrcQuota(newQuota);
		}
		q.receivedResourcesList.add(q.getRsrcQuota());

		Resource remain = q.getRsrcQuota();
		List<BaseDag> runningJobs = new LinkedList<BaseDag>(q.getRunningJobs());
		Collections.sort(runningJobs, new JobArrivalComparator());
		for (BaseDag job : runningJobs) {
			Resource rsShare = Resources.piecewiseMin(remain, job.getMaxDemand());
			job.rsrcQuota = rsShare;
			remain.subtract(rsShare);
			Output.debugln(DEBUG,
			    "[DRFScheduler] Allocated to job:" + job.dagId + " @ " + job.getQueueName() + " "
			        + job.rsrcQuota);
		}
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
	}

	public void fairShare() { // backup
		Queue<JobQueue> nonAllocatedQueues = new LinkedList<JobQueue>(
		    Simulator.QUEUE_LIST.getRunningQueues());
		Resource availRes = Simulator.cluster.getClusterMaxResAlloc();
		// update the resourceShareAllocated for every running job
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			double factor = 0.0;
			for (JobQueue qTemp : nonAllocatedQueues) {
				factor += qTemp.getWeight();
			}
			Resource allocRes = Resources.divideNoRound(availRes, factor);
			allocRes = Resources.multiply(allocRes, q.getWeight());
			allocRes.floor();
			allocRes = Resources.piecewiseMin(allocRes, q.getMaxDemand());
			q.setRsrcQuota(allocRes);
			nonAllocatedQueues.remove(q);

			availRes = Resources.subtractPositivie(availRes, q.getRsrcQuota());

			q.setRsrcQuota(allocRes);
			Output
			    .debugln(DEBUG, "Allocated to queue:" + q.getQueueName() + " share:" + q.getRsrcQuota());
		}
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
