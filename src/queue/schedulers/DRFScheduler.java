package queue.schedulers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;
import cluster.utils.Utils;

public class DRFScheduler implements Scheduler {
	private static boolean DEBUG = false;

	private String schedulePolicy;
	// Map<String, Resources> resDemandsQueues = null;

	static Resource clusterTotCapacity = null;

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
			Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobArrivalComparator());
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
    auxilaryShare[i] = 0.0;
      userDominantShareArr[i] = normalizedShare.max() / queue.getWeight()
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
      BaseJob unallocJob = q.getUnallocRunningJob();
      if (unallocJob == null) {
        userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
        // do not allocate to this queue any more
        continue;
      }

      int taskId = unallocJob.getCommingTaskId();
      Resource allocRes = unallocJob.naiveRsrcDemands(taskId);
      if(q.getBeta()>=1)
    	  allocRes.resources[0] = 0;
      else
    	  allocRes.resources[1] = 0;
      
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
          userDominantShareArr[sMinIdx] = maxRes / q.getWeight()
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
				auxilaryShare[i] = 0.0;
			userDominantShareArr[i] = normalizedShare.max() / queue.getWeight();
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
			BaseJob unallocJob = q.getUnallocRunningJob();
			if (unallocJob == null) {
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				// do not allocate to this queue any more
				continue;
			}

			int taskId = unallocJob.getCommingTaskId();
		   Resource allocRes = unallocJob.naiveRsrcDemands(taskId);
		      if(q.getBeta()>=1)
		    	  allocRes.resources[0] = 0;
		      else
		    	  allocRes.resources[1] = 0;
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
		List<BaseJob> runningJobs = new LinkedList<BaseJob>(q.getRunningJobs());
		Collections.sort(runningJobs, new JobArrivalComparator());

		for (BaseJob job : runningJobs) {
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

	/*public void fifoShareForJobs(JobQueue q, Resource availRes) {
		boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
		if (!fit) {
			Resource newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
			q.setRsrcQuota(newQuota);
		}
		q.receivedResourcesList.add(q.getRsrcQuota());

		Resource remain = q.getRsrcQuota();
		List<BaseJob> runningJobs = new LinkedList<BaseJob>(q.getRunningJobs());
		Collections.sort(runningJobs, new JobArrivalComparator());
		for (BaseJob job : runningJobs) {
			Resource rsShare = Resources.piecewiseMin(remain, job.getMaxDemand());
			job.rsrcQuota = rsShare;
			remain.subtract(rsShare);
			Output.debugln(DEBUG,
			    "[DRFScheduler] Allocated to job:" + job.dagId + " @ " + job.getQueueName() + " "
			        + job.rsrcQuota);
		}
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
	}
*/
	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
