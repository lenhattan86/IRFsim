package queue.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobArrivalComparator;
import cluster.datastructures.JobLengthComparator;
import cluster.datastructures.JobProcessingTimeComparator;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.JobScheduling;
import cluster.simulator.Main.Globals.Method;
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
		
		List<JobQueue> runningQueues = Simulator.QUEUE_LIST.getRunningQueues(); // time comsuming

		int numQueuesRuning = runningQueues.size();
		if (numQueuesRuning == 0) {
			return;
		}
		
		if (Globals.JOB_SCHEDULER.equals(JobScheduling.FIFO))
			for (JobQueue q : runningQueues) {
				Collections.sort((List<BaseJob>) q.getQueuedUpJobs(), new JobArrivalComparator());
			}
		else if (Globals.JOB_SCHEDULER.equals(JobScheduling.SJF)){
			for (JobQueue q : runningQueues) {
//				Collections.sort((List<BaseJob>) q.getQueuedUpJobs(), new JobLengthComparator(2));
				Collections.sort((List<BaseJob>) q.getQueuedUpJobs(), new JobLengthComparator(0));
			}
		}
		
//		drf(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
		onlineDRFShare(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues()); 
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
			Resource availRes = Simulator.cluster.getClusterResAvail();
			// step 1: pick user i with lowest s_i
			int sMinIdx = Utils.getMinValIdx(userDominantShareArr);
			if (sMinIdx < 0) {
				// There are more resources than demand.
				break;
			}
			// D_i demand for the next task
			JobQueue q = runningQueues.get(sMinIdx);
			BaseJob unallocJob = q.getNonProfilingRunnableJob(); // time consuming
			if (unallocJob == null) {
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				// do not allocate to this queue any more
				continue;
			}

			int taskId = unallocJob.getCommingTaskId();
		  InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId); // time consuming?
		  InterchangableResourceDemand reportDemand = unallocJob.getReportDemand();
		  
		  Resource allocRes = reportDemand.isCpuJob()?reportDemand.getCpuDemand():reportDemand.getGpuDemand();
		  if (Globals.METHOD.equals(Method.DRFFIFO)){
		  	allocRes = reportDemand.getGpuDemand();
			}
			// Like Yarn, assign one single container for the task
			// step 3: if fit, C+D_i <= R, allocate
			Resource temp = Resources.sumRound(consumedRes, allocRes);
//			if (resCapacity.greaterOrEqual(temp)) {
			if (availRes.greaterOrEqual(allocRes)) {
				consumedRes = temp;
				q.setRsrcQuota(Resources.sum(q.getRsrcQuota(), q.nextTaskRes()));
				
				double duration = reportDemand.isCpuJob()?demand.cpuCompl:demand.gpuCompl;
				if (Globals.METHOD.equals(Method.DRFFIFO)){
					duration = demand.gpuCompl;
				}
				
				boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
						duration, allocRes);
				if (assigned) {
					// update userDominantShareArr
					unallocJob.getQueue().addRunningJob(unallocJob);
					double maxRes = q.getNormalizedDorminantResUsage();
					userDominantShareArr[sMinIdx] = maxRes / q.getWeight();
					unallocJob.onStart(clusterTotCapacity);
					if (unallocJob.jobStartRunningTime<0){
					  unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
					}
				} else {
					Output.debugln(DEBUG, "[DRFScheduler] Cannot assign resource to the task" + taskId
					    + " of Job " + unallocJob.dagId + " " + allocRes);
					userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
					break;
				}

			} else {
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				// do not allocate to this queue any more
				break;
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
		List<BaseJob> runningJobs = new LinkedList<BaseJob>(q.getQueuedUpJobs());
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

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
