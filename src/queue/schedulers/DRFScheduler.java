package queue.schedulers;

import java.util.LinkedList;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.utils.Output;

public class DRFScheduler implements Scheduler {
	private static final boolean DEBUG = true;

	private String schedulePolicy;

	Resources clusterTotCapacity = null;

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
			 Output.debugln(DEBUG,"Allocated to queue:" + q.getQueueName() + " share:" + q.getRsrcQuota());
			// TODO: share the resources among the jobs in the same queue. (using Fair)
			
//			Resources rsShare = Resources.divide(allocRes, q.getRunningJobs().size());
//			Queue<BaseDag> runningJobs = q.getRunningJobs();
//			for (BaseDag job : runningJobs) {
//					job.rsrcQuota = rsShare;
////					 Output.debugln(DEBUG,"Allocated to job:" + job.dagId + " share:" + job.rsrcQuota);
//			}
		}
		
		availRes = Simulator.cluster.getClusterMaxResAlloc();
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
			if (!fit) {
				Resources newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
				q.setRsrcQuota(newQuota);
			}
			Output.debugln(DEBUG, "[DRFScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
			q.receivedResourcesList.add(q.getRsrcQuota());

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

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
