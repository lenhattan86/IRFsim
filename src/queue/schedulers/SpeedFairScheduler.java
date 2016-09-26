package queue.schedulers;

import java.util.LinkedList;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.utils.Output;

public class SpeedFairScheduler implements Scheduler {
	private static final boolean DEBUG = true;

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
		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}
		Resources availRes= Simulator.cluster.getClusterMaxResAlloc();
		
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			q.updateGuartRate(); // update long-term fairness
			// compute the rsrcQuota based on the guarateed rated.
			Resources rsrcQuota = Resources.piecewiseMax(q.computeShortTermShare(), q.computeLongTermShare());
			q.setRsrcQuota(rsrcQuota);
			availRes =  Resources.subtractPositivie(availRes, q.getRsrcQuota());
		}
		
		// equally share the available resources
		
		if (availRes.greaterOrEqual(new Resources())){
			Resources equalShare = Resources.divide(availRes, Simulator.QUEUE_LIST.getRunningQueues().size());
			for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
				Resources res = q.getRsrcQuota();
				res.addWith(equalShare);
				q.setRsrcQuota(res);
			}
		}
		//TODO: sort queues for interactive jobs.
		// recompute rsrcQuota to match the cluster capacity.
		availRes = Simulator.cluster.getClusterMaxResAlloc();
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
			if (!fit) {
				Resources newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
				q.setRsrcQuota(newQuota);
			} 
			Output.debugln(DEBUG, "Allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
			q.receivedResourcesList.add(q.getRsrcQuota());
		 //TODO: share the resources among the jobs in the same queue. (using Fair)
			Resources rsShare = Resources.divide(q.getRsrcQuota(), q.runningJobs.size());
			Queue<BaseDag> runningJobs = q.runningJobs;
			for (BaseDag job : runningJobs) {
				job.rsrcQuota = rsShare;
				 Output.debugln(DEBUG, "Allocated to job:" + job.dagId + " " + job.rsrcQuota);
			}
			
			availRes = Resources.subtract(availRes ,q.getRsrcQuota());
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
			//TODO: change job.jobStartTime to job.jobStartRunningTime (dynamic for each job).
			Resources guaranteedResource = job.serviceCurve.getMinReqService(Simulator.CURRENT_TIME-job.jobStartTime); 
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
		
		//TODO: share the remaining resources on demand
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
