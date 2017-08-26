package queue.schedulers;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import cluster.datastructures.BaseJob;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.utils.Output;

public class FairScheduler implements Scheduler {
	private static final boolean DEBUG = true;

	private String schedulePolicy;
	Map<String, Resource> resDemandsQueues = null;

	Resource clusterTotCapacity = null;
	
	public FairScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "Fair";
	}

	// FairShare = 1 / N across all dimensions (In Yarn, it should be based on a single dimension)
	// N - total number of running jobs
	@Override
	public void computeResShare() {
		
		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}
		
		Queue<JobQueue> nonAllocatedQueues = new LinkedList<JobQueue>(Simulator.QUEUE_LIST.getRunningQueues());
		Resource availRes = Simulator.cluster.getClusterMaxResAlloc();
		// update the resourceShareAllocated for every running job
		// TODO: Sort queue: large demand to -> low demand
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			double factor = 0.0;
			for (JobQueue qTemp : nonAllocatedQueues) {
				factor += qTemp.getWeight();
			}
			Resource allocRes = Resources.divideNoRound(availRes, factor);
			allocRes = Resources.multiply(allocRes, q.getWeight());
//			allocRes.floor();
			allocRes = Resources.piecewiseMin(allocRes, q.getMaxDemand());
			q.setRsrcQuota(allocRes);
			nonAllocatedQueues.remove(q);
			
			availRes = Resources.subtractPositivie(availRes, q.getRsrcQuota());
			
			q.setRsrcQuota(allocRes);
			 Output.debugln(DEBUG,"Allocated to queue:" + q.getQueueName() + " share:" + q.getRsrcQuota());
		}
		
		availRes = Simulator.cluster.getClusterMaxResAlloc();
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
			if (!fit) {
				Resource newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
				q.setRsrcQuota(newQuota);
			}
			Output.debugln(DEBUG, "[DRFScheduler] real share allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
			q.receivedResourcesList.add(q.getRsrcQuota());

			Resource remain = q.getRsrcQuota();
			Queue<BaseJob> runningJobs = q.getRunningJobs();
			for (BaseJob job : runningJobs) {
				Resource rsShare = Resources.divide(q.getRsrcQuota(), q.runningJobsSize());
//				rsShare.floor();
				job.rsrcQuota = rsShare;
				remain.subtract(rsShare);
			}
//			shareRemainRes(q, remain);
			Output.debugln(DEBUG, "[DRFScheduler] Allocated to queue:" + q.getQueueName() + " " + q.getJobsQuota());
			availRes = Resources.subtract(availRes, q.getRsrcQuota());
		}
	}
	
	private void shareRemainRes(JobQueue q, Resource remain){ // utilize more resource
		Queue<BaseJob> runningJobs = q.cloneRunningJobs();
		while (true) {
			if (runningJobs.isEmpty() || remain.isEmpty()){
				break;
			}
			Queue<BaseJob> jobs = new LinkedList<BaseJob>(runningJobs);
			for (BaseJob job: jobs){
				if (remain.isEmpty())
					break;
				
				if (job.getMaxDemand().greater(job.rsrcQuota)){
					Resource tmp = Resources.piecewiseMin(new Resource(1.0), remain);
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
