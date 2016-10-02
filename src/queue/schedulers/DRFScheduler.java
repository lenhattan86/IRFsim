package queue.schedulers;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.Method;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;
import cluster.utils.Utils;

public class DRFScheduler implements Scheduler {
	private static final boolean DEBUG = true;

	private String schedulePolicy;
	// Map<String, Resources> resDemandsQueues = null;

	Resources clusterTotCapacity = null;

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

		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}
		//
		// if (Globals.METHOD.equals(Method.Strict)){
		// getRunningInteractiveQueues()
		// }

		computeDRFShare(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());

		Resources availRes = Simulator.cluster.getClusterMaxResAlloc();
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			fifoShareForJobs(q, availRes);
		}
	}

	public static void computeDRFShare(Resources flexibleResources, List<JobQueue> runningQueues) {
		HashMap<String, Resources> resDemandsQueues = new HashMap<String, Resources>();
		double factor = 0.0;
		for (JobQueue q : runningQueues) {
			factor += q.getWeight();
		}

		for (JobQueue q : runningQueues) {
			// 1. compute it's avg. resource demand vector it not already computed
			Resources avgResDemandDag = q.getMaxDemand();
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
			avgResDemandDag.round(Globals.TOLERANT_ERROR);

			Resources resDemand = Resources.piecewiseMin(q.getMaxDemand(), avgResDemandDag); // increase
																																												// utilization.

			resDemandsQueues.put(q.getQueueName(), resDemand);
		}

		// 4. sum it up across every dimension
		Resources sumDemandsRunQueues = new Resources(0.0);
		for (JobQueue q : runningQueues) {
			sumDemandsRunQueues.addWith(resDemandsQueues.get(q.getQueueName()));
		}

		// 5. find the max sum
		int maxIdx = sumDemandsRunQueues.idOfMaxResource();
		double drfShare = flexibleResources.resource(maxIdx) / sumDemandsRunQueues.max();

		for (JobQueue q : runningQueues) {
			Resources drfQuota = Resources.clone(resDemandsQueues.get(q.getQueueName()));
			drfQuota.multiply(drfShare);
			drfQuota.round(Globals.TOLERANT_ERROR);
			q.setRsrcQuota(drfQuota);
			Output.debugln(DEBUG, "[DRFScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
		}
	}

	public void fairShareForJobs(JobQueue q, Resources availRes) {
		boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
		if (!fit) {
			Resources newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
			q.setRsrcQuota(newQuota);
		}
		Output.debugln(DEBUG, "[DRFScheduler] drf share allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
		q.receivedResourcesList.add(q.getRsrcQuota());

		Resources remain = q.getRsrcQuota();
		List<BaseDag> runningJobs = new LinkedList<BaseDag>(q.getRunningJobs());
		Collections.sort(runningJobs, new JobArrivalComparator());

		for (BaseDag job : runningJobs) {
			Resources rsShare = Resources.divide(q.getRsrcQuota(), q.runningJobsSize());
			// rsShare.floor();
			job.rsrcQuota = rsShare;
			remain.subtract(rsShare);
		}
		// shareRemainRes(q, remain);
		Output.debugln(DEBUG, "[DRFScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getJobsQuota());
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
	}

	public void fifoShareForJobs(JobQueue q, Resources availRes) {
		boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
		if (!fit) {
			Resources newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
			q.setRsrcQuota(newQuota);
		}
		q.receivedResourcesList.add(q.getRsrcQuota());

		Resources remain = q.getRsrcQuota();
		List<BaseDag> runningJobs = new LinkedList<BaseDag>(q.getRunningJobs());
		Collections.sort(runningJobs, new JobArrivalComparator());
		for (BaseDag job : runningJobs) {
			Resources rsShare = Resources.piecewiseMin(remain, job.getMaxDemand());
			job.rsrcQuota = rsShare;
			remain.subtract(rsShare);
			Output.debugln(DEBUG,
					"[DRFScheduler] Allocated to job:" + job.dagId + " @ " + job.getQueueName() + " " + job.rsrcQuota);
		}
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
	}

	public void fairShare() { // backup
		Queue<JobQueue> nonAllocatedQueues = new LinkedList<JobQueue>(Simulator.QUEUE_LIST.getRunningQueues());
		Resources availRes = Simulator.cluster.getClusterMaxResAlloc();
		// update the resourceShareAllocated for every running job
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			double factor = 0.0;
			for (JobQueue qTemp : nonAllocatedQueues) {
				factor += qTemp.getWeight();
			}
			Resources allocRes = Resources.divideNoRound(availRes, factor);
			allocRes = Resources.multiply(allocRes, q.getWeight());
			allocRes.floor();
			allocRes = Resources.piecewiseMin(allocRes, q.getMaxDemand());
			q.setRsrcQuota(allocRes);
			nonAllocatedQueues.remove(q);

			availRes = Resources.subtractPositivie(availRes, q.getRsrcQuota());

			q.setRsrcQuota(allocRes);
			Output.debugln(DEBUG, "Allocated to queue:" + q.getQueueName() + " share:" + q.getRsrcQuota());
		}
	}

	private void shareRemainRes(JobQueue q, Resources remain) {
		Queue<BaseDag> runningJobs = q.cloneRunningJobs();
		while (true) {
			if (runningJobs.isEmpty() || remain.isEmpty()) {
				break;
			}
			Queue<BaseDag> jobs = new LinkedList<BaseDag>(runningJobs);
			for (BaseDag job : jobs) {
				if (remain.isEmpty())
					break;

				if (job.getMaxDemand().greater(job.rsrcQuota)) {
					Resources unit = Resources.piecewiseMin(new Resources(1.0), remain);
					job.rsrcQuota.addWith(unit);
					remain.subtract(unit);
				} else {
					runningJobs.remove(job);
				}
			}
		}
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
