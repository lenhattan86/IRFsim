package queue.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobArrivalComparator;
import cluster.datastructures.JobLengthComparator;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.JobScheduling;
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
		this.schedulePolicy = "ES";
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
				Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobArrivalComparator());
			}
		// equal share all resources
		// equallyAllocate(clusterTotCapacity, // Simulator.QUEUE_LIST.getRunningQueues());
		onlineEqualShare(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
	}

	public static void equallyAllocate(Resource resCapacity, List<JobQueue> runningQueues2) {

		List<JobQueue> runningQueues = new ArrayList<JobQueue>();

		for (JobQueue queue : runningQueues2) {
			if (queue.hasRunningJobs())
				runningQueues.add(queue);
		}

		int numOfQueues = runningQueues.size();
		if (runningQueues.isEmpty())
			return;

		// step 1: compute equal share
		Resource equalShares = Resources.divide(resCapacity, numOfQueues);

		for (int i = 0; i < numOfQueues; i++) {
			JobQueue q = runningQueues.get(i);
			double shares[] = Resources.clone(equalShares).resources;
			// Allocate on GPU
			if (Globals.JOB_SCHEDULER.equals(JobScheduling.FIFO))
				QueueScheduler.allocateResToQueue(q, shares, false);
			else if (Globals.JOB_SCHEDULER.equals(JobScheduling.SRPT)) {
				double cpuShare = shares[0];
				shares[0] = 0;
				Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobLengthComparator(2));
				Resource remain = QueueScheduler.allocateResToQueue(q, shares, false);
				shares[0] = cpuShare;
				shares[2] = remain.resource(2);
				Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobLengthComparator(1));
				remain = QueueScheduler.allocateResToQueue(q, shares, false);
				shares[0] = 0;
			}
		}
	}

	public static void onlineEqualShare(Resource resCapacity, List<JobQueue> runningQueues2) {

		List<JobQueue> runningQueues = new ArrayList<JobQueue>();

		for (JobQueue queue : runningQueues2) {
			if (queue.hasRunningJobs())
				runningQueues.add(queue);
		}

		int numOfQueues = runningQueues.size();
		if (runningQueues.isEmpty())
			return;

		// allocate on GPU
		boolean isResAvail = true;
		while (isResAvail) {
			// pick the least GPU demand
			double minGPU = Double.MAX_VALUE;
			JobQueue qMin = null;
			for (int i = 0; i < numOfQueues; i++) {
				JobQueue q = runningQueues.get(i);
				Resource usage = q.getResourceUsage();
				BaseJob unallocJob = q.getUnallocRunningJob();
				if (unallocJob == null)
					continue;
				if (minGPU > usage.resource(1)) {
					minGPU = usage.resource(1);
					qMin = q;
				}
			}
			if (qMin==null)
				break;
		// allocate the job for minIdx
			Collections.sort((List<BaseJob>) qMin.getRunningJobs(), new JobLengthComparator(2));
			BaseJob unallocJob = qMin.getUnallocRunningJob();
			Resource demand = unallocJob.getDemand().getGpuDemand();
			Resource remain = QueueScheduler.allocateResToQueue(qMin, Resources.sum(demand, qMin.getResourceUsage()).resources, false);
			Resource resAvail = Simulator.cluster.getClusterResAvail();
			// TODO: [bug] all GPU are allocated but the log says different. 
			if (remain.resource(1)>=0.1){
				isResAvail = false;
//				QueueScheduler.allocateResToQueue(qMin, Resources.sum(demand, qMin.getResourceUsage()).resources, false);
			}
		}
		
		isResAvail = true;
		while (isResAvail) {
			// pick the least CPU demand
			double minCPU = Double.MAX_VALUE;
			JobQueue qMin = null;
			for (int i = 0; i < numOfQueues; i++) {
				JobQueue q = runningQueues.get(i);
				Resource usage = q.getResourceUsage();
				BaseJob unallocJob = q.getUnallocRunningJob();
				if (unallocJob == null)
					continue;
				if (minCPU > usage.resource(0)) {
					minCPU = usage.resource(0);
					qMin = q;
				}
			}
			if (qMin==null)
				break;
		// allocate the job for minIdx
			Collections.sort((List<BaseJob>) qMin.getRunningJobs(), new JobLengthComparator(1));
			BaseJob unallocJob = qMin.getUnallocRunningJob();
			Resource demand = unallocJob.getDemand().getCpuDemand();
			Resource remain = QueueScheduler.allocateResToQueue(qMin, Resources.sum(demand, qMin.getResourceUsage()).resources, false);
			Resource resAvail = Simulator.cluster.getClusterResAvail();
			if (remain.resource(0)>=0.001)
				isResAvail = false;
		}
		
	}

	public static void equallyAllocate_online(Resource resCapacity, List<JobQueue> runningQueues2) {

		List<JobQueue> runningQueues = new ArrayList<JobQueue>();

		for (JobQueue queue : runningQueues2) {
			if (queue.hasRunningJobs())
				runningQueues.add(queue);
		}

		int numOfQueues = runningQueues.size();
		if (runningQueues.isEmpty())
			return;
		// retrieved current usage
		Resource[] userShareArr = new Resource[runningQueues.size()];
		int i = 0;
		for (JobQueue queue : runningQueues) {
			Resource normalizedShare = Resources.divideVector(queue.getResourceUsage(),
					Simulator.cluster.getClusterMaxResAlloc());
			userShareArr[i] = Resources.divide(normalizedShare, queue.getWeight());
			i++;
		}

		// step 1: compute equal share
		Resource equalShares = Resources.divide(resCapacity, numOfQueues);

		// step 2: allocate the share
		for (JobQueue q : runningQueues) {
			Resource allocRes = q.getResourceUsage();
			boolean jobAvail = true;
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
					if (taskId < 0)
						break;
					Resource remainingRes = Resources.subtract(equalShares, allocRes);

					InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
					Resource gDemand = demand.getGpuDemand();
					Resource cDemand = demand.getCpuDemand();
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

					boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId, unallocJob.duration(taskId),
							taskDemand);

					if (assigned) {
						allocRes = Resources.sum(allocRes, taskDemand);
						// TODO: fix this for equal shares
						/*
						 * if(isCPU){ unallocJob.isCPUUsages.put(taskId, true); }
						 */

						if (unallocJob.jobStartRunningTime < 0) {
							unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
						}
					} else {
						// Output.debugln(true, "Failed to assign the resource to job " +
						// unallocJob.dagId);
						isResAvail = false;
						jobAvail = false;
						break;
					}
				}
			}
		}
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}