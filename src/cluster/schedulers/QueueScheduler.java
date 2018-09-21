package cluster.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobQueue;
import cluster.datastructures.MLJob;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;
import queue.schedulers.AlloXScheduler;
import queue.schedulers.DRFExtScheduler;
import queue.schedulers.DRFScheduler;
import queue.schedulers.EqualShareScheduler;
import queue.schedulers.FlowScheduler;
import queue.schedulers.PricingScheduler;
import queue.schedulers.SJFScheduler;
import queue.schedulers.Scheduler;

public class QueueScheduler {
	private final static boolean DEBUG = true;

	public Scheduler scheduler;
	public long profilingTime = 0;
	public long schedulingTime = 0;

	public QueueScheduler() {
		profilingTime = 0;
		schedulingTime = 0;
		switch (Globals.QUEUE_SCHEDULER) {
		case ES:
			scheduler = new EqualShareScheduler();
			break;
		case DRF:
			scheduler = new DRFScheduler();
			break;
		case DRFExt:
			scheduler = new DRFExtScheduler();
			break;
		case Pricing:
			scheduler = new PricingScheduler();
			break;
		case AlloX:
			scheduler = new AlloXScheduler(Globals.alpha);
			break;
		case FS:
			scheduler = new FlowScheduler(Globals.alpha);
			break;	
		case SJF:
			scheduler = new AlloXScheduler();
			break;
		default:
			System.err.println("Unknown sharing policy");
		}
	}

	public void schedule() {
		// compute how much share each queue should get
		long startTime = System.nanoTime();
		boolean coninueSchedule = true;
		if (Globals.EnableProfiling) {
			coninueSchedule = scheduleProfilingJobs();
		}
		profilingTime = profilingTime + System.nanoTime() - startTime;
		if (coninueSchedule) {
			startTime = System.nanoTime();
			scheduler.computeResShare();
			schedulingTime = schedulingTime + System.nanoTime() - startTime;
		}
	}

	public boolean  scheduleProfilingJobs() {

		boolean isCpuAvailable = true;
		boolean isGpuAvailable = true;
		if(Simulator.CURRENT_TIME >= 301){
			int a = 0;
		}
		
		int round = 0;
		int queueId = 0;
		int maxRound = Integer.MIN_VALUE;
		List<JobQueue> activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedProfilingJobs();
		while (isGpuAvailable || isCpuAvailable) {
			if (activeQueues.isEmpty())
				return true;
			JobQueue q = activeQueues.get(queueId % activeQueues.size());
			ArrayList<BaseJob> jobs = new ArrayList<BaseJob>(q.getQueuedUpProfilingJobs());
			int size = jobs.size();
			if (round<1)
				if (maxRound< size)
					maxRound= size;
			
			if (round < size){
				BaseJob job = jobs.get(round);
				if (!job.isCpu && isGpuAvailable) {
					isGpuAvailable = QueueScheduler.allocateResToJob(job, false);
				} else if (job.isCpu && isCpuAvailable) {
					isCpuAvailable = QueueScheduler.allocateResToJob(job, true);
				}
			}
			
			queueId++;
			if (queueId%activeQueues.size() == 0){
				round++;
				if (round > maxRound)
					return true;
			}
		}
		return false;
	}

	public void adjustShares() {
		List<Integer> unhappyDagsIds = new ArrayList<Integer>();

		final Map<Integer, Resource> unhappyDagsDistFromResShare = new HashMap<Integer, Resource>();
		for (BaseJob dag : Simulator.runningJobs) {
			if (!dag.rsrcQuota.distinct(dag.getRsrcInUse())) {
				continue;
			}

			if (dag.getRsrcInUse().greaterOrEqual(dag.rsrcQuota)) {
				// TODO: do we need to deal with this case: this dag has more resources
				// than fairshare.
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
			if (!availRes.greater(new Resource(0.0)))
				break;

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

	public static boolean allocateResToJob(BaseJob job, boolean isCpu) {
		int taskId = job.getCommingTaskId();
		if (taskId < 0)
			return true;
		InterchangableResourceDemand demand = job.rsrcDemands(taskId);

		double duration = isCpu ? demand.cpuCompl : demand.gpuCompl;
		Resource taskDemand = isCpu ? demand.getCpuDemand() : demand.getGpuDemand();
		boolean assigned = Simulator.cluster.assignTask(job.dagId, taskId, duration, taskDemand);
		if (assigned) {
			if (job.jobStartRunningTime < 0) {
				job.jobStartRunningTime = Simulator.CURRENT_TIME;
			}
			job.isCpu = isCpu;
			job.getQueue().addRunningJob(job);
			return true;
		} else
			return false;
	}
	
	public static boolean allocateResToJobOnMachine(BaseJob job, boolean isCpu, int machineId) {
		int taskId = job.getCommingTaskId();
		if (taskId < 0)
			return true;
		InterchangableResourceDemand demand = job.rsrcDemands(taskId);

		double duration = isCpu ? demand.cpuCompl : demand.gpuCompl;
		Resource taskDemand = isCpu ? demand.getCpuDemand() : demand.getGpuDemand();
		boolean assigned = Simulator.cluster.assignTask(job.dagId, taskId, duration, taskDemand);
		if (assigned) {
			if (job.jobStartRunningTime < 0) {
				job.jobStartRunningTime = Simulator.CURRENT_TIME;
			}
			job.isCpu = isCpu;
			job.getQueue().addRunningJob(job);
			return true;
		} else
			return false;
	}

	public static Resource allocateResToQueue(JobQueue q, double[] shares, boolean isEstimated) {
		Resource computedShares = new Resource(shares);
		Resource allocRes = q.getResourceUsage();
		boolean jobAvail = true;
		Resource remainingRes = new Resource(shares);
		while (jobAvail) {
			BaseJob unallocJob = q.getUnallocRunnableJob();
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

				remainingRes = Resources.subtract(computedShares, allocRes);

				InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
				Resource gDemand = demand.getGpuDemand();
				Resource cDemand = demand.getCpuDemand();
				Resource taskDemand = null;

				boolean isCPU = false;
				if (isEstimated && demand.isCpuJob())
					if (!cDemand.fitsIn(remainingRes)) {
						isResAvail = false;
						jobAvail = false;
						break;
					} else {
						taskDemand = cDemand;
						isCPU = true;
					}
				else if (!gDemand.fitsIn(remainingRes)) {
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

				double duration = isCPU ? demand.cpuCompl : demand.gpuCompl;

				// TODO: change switch the duration of the jobs.
				boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId, duration, taskDemand);

				if (assigned) {
					allocRes = Resources.sum(allocRes, taskDemand);

					if (unallocJob.jobStartRunningTime < 0) {
						unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
					}
					q.addRunningJob(unallocJob);
				} else {
					isResAvail = false;
					jobAvail = false;
					break;
				}
			}
		}
		return remainingRes;
	}

	public static void allocateResToSingJob(JobQueue q, double[] shares) {
		Resource computedShares = new Resource(shares);
		Resource allocRes = q.getResourceUsage();
		BaseJob unallocJob = q.getUnallocRunnableJob();

		if (unallocJob == null) {
			return;
		}

		boolean isResAvail = true;
		while (isResAvail) {
			// allocate resource to each task.
			int taskId = unallocJob.getCommingTaskId();
			if (taskId < 0)
				break;

			Resource remainingRes = Resources.subtract(computedShares, allocRes);

			InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
			Resource gDemand = demand.getGpuDemand();
			Resource cDemand = demand.getCpuDemand();
			Resource taskDemand = null;

			boolean isCPU = false;
			if (demand.isCpuJob())
				if (!cDemand.fitsIn(remainingRes)) {
					isResAvail = false;
					break;
				} else {
					taskDemand = cDemand;
					isCPU = true;
				}
			else if (!gDemand.fitsIn(remainingRes)) {
				if (!cDemand.fitsIn(remainingRes)) {
					isResAvail = false;
					break;
				} else {
					taskDemand = cDemand;
					isCPU = true;
				}
			} else {
				taskDemand = gDemand;
			}

			double duration = isCPU ? demand.cpuCompl : demand.gpuCompl;
			// unallocJob.duration(taskId) = duration;

			// TODO: change switch the duration of the jobs.
			boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId, duration, taskDemand);

			if (assigned) {
				allocRes = Resources.sum(allocRes, taskDemand);
				// TODO: add task to resource usage list.
				/*
				 * if (isCPU) { unallocJob.isCPUUsages.put(taskId, true); }
				 */

				if (unallocJob.jobStartRunningTime < 0) {
					unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
				}
			} else {
				isResAvail = false;
				break;
			}
		}
	}

}
