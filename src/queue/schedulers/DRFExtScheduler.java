package queue.schedulers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobArrivalComparator;
import cluster.datastructures.JobLengthComparator;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.JobScheduling;
import cluster.utils.Output;
import cluster.utils.Utils;

public class DRFExtScheduler implements Scheduler {
	private static boolean DEBUG = false;

	private String schedulePolicy;
	// Map<String, Resources> resDemandsQueues = null;

	static Resource clusterTotCapacity = null;
	static double beta = 32.0;

	public DRFExtScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "DRFExt";
		// TODO: compute avg. beta.
		double cpuSize = 0.0;
		double gpuSize = 0.0;
		for (BaseJob b: Simulator.runnableJobs){
			InterchangableResourceDemand demand = b.getDemand();
			cpuSize = demand.cpu * demand.cpuCompl;
			gpuSize = demand.gpu * demand.gpuCompl;
			this.beta += cpuSize/gpuSize;
		}
		this.beta = this.beta/Simulator.runnableJobs.size();
		
		System.out.println("avg. beta             = " + this.beta);
	}

	@Override
	public void computeResShare() {
		
		List<JobQueue> runningQueues = Simulator.QUEUE_LIST.getRunningQueues();

		int numQueuesRuning = runningQueues.size();
		if (numQueuesRuning == 0) {
			return;
		}
		
		if (Globals.JOB_SCHEDULER.equals(JobScheduling.FIFO))
			for (JobQueue q : runningQueues) {
				Collections.sort((List<BaseJob>) q.getQueuedUpJobs(), new JobArrivalComparator());
			}
		
		onlineDRFShare(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues()); 
	}
	
	public static double normVirResource(Resource usage, Resource capacity){
		double virtualCPU = (usage.resource(0) + usage.resource(1)*beta) / (capacity.resource(0) +capacity.resource(1)*beta); 
		double mem = usage.resource(2)/ capacity.resource(2);
		return Math.max(virtualCPU, mem);
	}
	
	public static void onlineDRFShare(Resource resCapacity, List<JobQueue> runningQueues) {
		// init
		Resource consumedRes = new Resource();
		double[] userDominantShareArr = new double[runningQueues.size()];
		// TODO: consider the allocated share (because of no preemption).
		int i = 0;
//		double[] auxilaryShare = new double[runningQueues.size()];
		for (JobQueue queue : runningQueues) {
			Resource usage = queue.getResourceUsage();
			userDominantShareArr[i] = normVirResource(usage, resCapacity)/ queue.getWeight();
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
			if (Globals.JOB_SCHEDULER.equals(JobScheduling.SJF))
					Collections.sort((List<BaseJob>) q.getQueuedUpJobs(), new JobLengthComparator(2));
			
			BaseJob unallocJob = q.getNonProfilingRunnableJob();

			if (unallocJob == null) {
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				// do not allocate to this queue any more
				continue;
			}
			
			int taskId = unallocJob.getCommingTaskId();
		  InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
			// Like Yarn, assign one single container for the task
			// step 3: if fit, C+D_i <= R, allocate
		  // try GPU
	  	Resource allocRes = demand.getGpuDemand();
			double duration = demand.gpuCompl;
			boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
					duration, allocRes);
			
			// try CPU
			if (!assigned) {
				if (Globals.JOB_SCHEDULER.equals(JobScheduling.SJF))
					Collections.sort((List<BaseJob>) q.getQueuedUpJobs(), new JobLengthComparator(1));			
				unallocJob = q.getNonProfilingRunnableJob();
				allocRes = demand.getCpuDemand();
				duration = demand.cpuCompl;
				assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
						duration, allocRes);
			}  
			
			if (assigned) {
				// update userDominantShareArr
				unallocJob.getQueue().addRunningJob(unallocJob);
				double maxRes = normVirResource(q.getResourceUsage(), resCapacity);
				userDominantShareArr[sMinIdx] = maxRes / q.getWeight();
				unallocJob.onStart(clusterTotCapacity);
				if (unallocJob.jobStartRunningTime<0){
				  unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
				}
			} else {
				Output.debugln(DEBUG, "[DRFExtScheduler] Cannot assign resource to the task" + taskId
				    + " of Job " + unallocJob.dagId + " " + allocRes);
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				break;
			}
		}
	}
	
	
	public static void onlineDRFShare_v1(Resource resCapacity, List<JobQueue> runningQueues) {
		// init
		Resource consumedRes = new Resource();
		double[] userDominantShareArr = new double[runningQueues.size()];
		// TODO: consider the allocated share (because of no preemption).
		int i = 0;
//		double[] auxilaryShare = new double[runningQueues.size()];
		for (JobQueue queue : runningQueues) {
			Resource usage = queue.getResourceUsage();
			userDominantShareArr[i] = normVirResource(usage, resCapacity)/ queue.getWeight();
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
			if (Globals.JOB_SCHEDULER.equals(JobScheduling.SJF))
				Collections.sort((List<BaseJob>) q.getQueuedUpJobs(), new JobLengthComparator(2));
			
			BaseJob unallocJob = q.getUnallocRunnableJob();
			if (unallocJob == null) {
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
				// do not allocate to this queue any more
				continue;
			}

			int taskId = unallocJob.getCommingTaskId();
		  InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
		  
			// Like Yarn, assign one single container for the task
			// step 3: if fit, C+D_i <= R, allocate
		  // try with the better configration
	  	Resource allocRes = demand.isCpuJob()?demand.getCpuDemand():demand.getGpuDemand();
			double duration = demand.isCpuJob()?demand.cpuCompl:demand.gpuCompl;
			boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
					duration, allocRes);
			
			// try with the other configuration
			if (!assigned) {
				allocRes = !demand.isCpuJob()?demand.getCpuDemand():demand.getGpuDemand();
				duration = !demand.isCpuJob()?demand.cpuCompl:demand.gpuCompl;
				assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
						duration, allocRes);
			}
			
			if (assigned) {
				// update userDominantShareArr
				double maxRes = normVirResource(q.getResourceUsage(), resCapacity);
				userDominantShareArr[sMinIdx] = maxRes / q.getWeight();
				if (unallocJob.jobStartRunningTime<0){
				  unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
				}
			} else {
				Output.debugln(DEBUG, "[DRFExtScheduler] Cannot assign resource to the task" + taskId
				    + " of Job " + unallocJob.dagId + " " + allocRes);
				userDominantShareArr[sMinIdx] = Double.MAX_VALUE;
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
		Output.debugln(DEBUG, "[DRFExtScheduler] drf share allocated to queue:" + q.getQueueName() + " "
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
		    "[DRFExtScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getJobsQuota());
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
