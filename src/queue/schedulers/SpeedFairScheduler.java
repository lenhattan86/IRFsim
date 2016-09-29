package queue.schedulers;

import java.util.LinkedList;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.utils.Output;

public class SpeedFairScheduler implements Scheduler {
	private boolean DEBUG = false;

	private String schedulePolicy;

	Resources clusterTotCapacity = null;

	public SpeedFairScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SpeedFair";
	}

	// FairShare = 1 / N across all dimensions
	// N - total number of running jobs
	@Override
	public void computeResShare() {
		if(Simulator.CURRENT_TIME>=90.0){
//			DEBUG = true;
		}
		Output.debugln(DEBUG, "[SpeedFairScheduler] STEP_TIME:" + Simulator.CURRENT_TIME);
		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}
		Resources availRes = Simulator.cluster.getClusterMaxResAlloc();
		
		Queue<JobQueue> allocatedQueues = new LinkedList<JobQueue>();
		
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			// compute the rsrcQuota based on the guarateed rate.
			Resources rsrcQuota = Resources.piecewiseMin(q.getMinService(Simulator.CURRENT_TIME), q.getMaxDemand());
			if (q.getMaxDemand().greater(rsrcQuota))
				allocatedQueues.add(q);
			q.setRsrcQuota(rsrcQuota);
			availRes = Resources.subtractPositivie(availRes, q.getRsrcQuota());
//			Output.debugln(DEBUG, "[SpeedFairScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
		}

		// Share the remaining resources
		if (availRes.greaterOrEqual(new Resources())) {
			double factor = 0.0;
			for (JobQueue q : allocatedQueues) {
				factor += q.getSpeedFairWeight();
			}
			Resources share = Resources.divide(availRes, factor);
			for (JobQueue q : allocatedQueues) {
				Resources res = q.getRsrcQuota();
				res.addWith(Resources.multiply(share, q.getSpeedFairWeight())); 
				res.floor();
				// compare the real demand and the fair share
				res = Resources.piecewiseMin(res, q.getMaxDemand());
				q.setRsrcQuota(res);
				availRes = Resources.subtractPositivie(availRes, res);
			}
		}
		
		// TODO: deal with the max demand is less than the allocated share. 

		// TODO: sort queues for interactive jobs.
		// Resource admission control for the queues.
		availRes = Simulator.cluster.getClusterMaxResAlloc();
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
			if (!fit) {
				Resources newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
				q.setRsrcQuota(newQuota);
			}
			Output.debugln(DEBUG, "[SpeedFairScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
			q.receivedResourcesList.add(q.getRsrcQuota());

			// TODO: Fair share the resources among the jobs. 
			
			Resources remain = q.getRsrcQuota();
			Queue<BaseDag> runningJobs = q.getRunningJobs();
			for (BaseDag job : runningJobs) {
				Resources rsShare = Resources.divide(q.getRsrcQuota(), q.runningJobsSize());
				rsShare.floor();
				job.rsrcQuota = rsShare;
				remain.subtract(rsShare);
			}
			shareRemainRes(q, remain);
//			Output.debugln(DEBUG, "[SpeedFairScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getJobsQuota());
			availRes = Resources.subtract(availRes, q.getRsrcQuota());
		}

	}
	
	private void shareRemainRes(JobQueue q, Resources remain){
		Queue<BaseDag> runningJobs = q.cloneRunningJobs();
		while (true) {
			if (runningJobs.isEmpty() || remain.isEmpty()){
				break;
			}
			Queue<BaseDag> jobs = new LinkedList<BaseDag>(runningJobs);
			for (BaseDag job: jobs){
				if (remain.isEmpty())
					break;
				
				if (job.getMaxDemand().greater(job.rsrcQuota)){
					Resources tmp = Resources.piecewiseMin(new Resources(1.0), remain);
					job.rsrcQuota.addWith(tmp);
					remain.subtract(tmp);
				}
				else {
					runningJobs.remove(job);
				}
			}
		}
	}

	public void computeResShare_prev() {
		int numJobsRunning = Simulator.runningJobs.size();
		if (numJobsRunning == 0) {
			return;
		}

		Resources clusterResQuotaAvail = Simulator.cluster.getClusterResQuotaAvail();

		// TODO: sort the runningJobs

		// update the resourceShareAllocated for every running job
		// assign the resources based on service curves.
		Queue<BaseDag> unhappyRunningJobs = new LinkedList<BaseDag>();
		for (BaseDag job : Simulator.runningJobs) {
			// TODO: change job.jobStartTime to job.jobStartRunningTime (dynamic for
			// each job).
			Resources guaranteedResource = job.serviceCurve.getMinReqService(Simulator.CURRENT_TIME - job.jobStartTime);
			Resources resToBeShared = Resources.subtractPositivie(guaranteedResource, job.receivedService);
			boolean fit = clusterResQuotaAvail.greater(resToBeShared);
			if (fit) {
				job.rsrcQuota = resToBeShared;
				clusterResQuotaAvail = Resources.subtract(clusterResQuotaAvail, resToBeShared);
			} else {
				job.rsrcQuota = new Resources(0.0); // unable to allocate the resources
				unhappyRunningJobs.add(job);
			}
		}

		// equally share the remaining resources to the unhappy jobs or all jobs.
		int numUnhappyJobs = unhappyRunningJobs.size();
		if (numUnhappyJobs > 0) {
			Output.debugln(DEBUG, "number of unhappy jobs: " + numUnhappyJobs);
			if (clusterResQuotaAvail.greater(new Resources(0))) {
				Resources quotaRsrcShare = Resources.divide(clusterResQuotaAvail, unhappyRunningJobs.size());
				for (BaseDag job : unhappyRunningJobs) {
					job.rsrcQuota.addRes(quotaRsrcShare);
				}
			}
		} else {
			if (clusterResQuotaAvail.greater(new Resources(0))) {
				Resources quotaRsrcShare = Resources.divide(clusterResQuotaAvail, numJobsRunning);
				for (BaseDag job : Simulator.runningJobs) {
					job.rsrcQuota.addRes(quotaRsrcShare);
				}
			}
		}

		// TODO: share the remaining resources on demand
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
