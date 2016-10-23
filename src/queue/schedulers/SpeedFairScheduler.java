package queue.schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

public class SpeedFairScheduler implements Scheduler {
	private boolean DEBUG = false;

	private String schedulePolicy;

	Resources clusterTotCapacity = null;

	public SpeedFairScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SpeedFair";
	}
	
	@Override
	public void computeResShare() {
		if(Simulator.CURRENT_TIME>=Globals.DEBUG_START && Simulator.CURRENT_TIME<=Globals.DEBUG_END){
			DEBUG = true;
		}else
			DEBUG = false;
//		System.out.println("SpeedFairScheduler:" + Simulator.CURRENT_TIME);	
		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}
		Resources avaiRes = Simulator.cluster.getClusterResAvail();
		Resources admissionControlRes =  Resources.clone(Simulator.cluster.getClusterMaxResAlloc());
		
		Resources speedFairRes = new Resources();
		
		List<JobQueue> queuesNeedAlloc = new LinkedList<JobQueue>();
		boolean isAdmitted = false;
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
		  isAdmitted = false;
		  if (q.isInteractive){
  			// compute the rsrcQuota based on the guarateed rate.
//  			Resources rsrcQuota = Resources.piecewiseMin(q.getMinService(Simulator.CURRENT_TIME), q.getMaxDemand());
		    Resources rate = q.getGuaranteeRate(Simulator.CURRENT_TIME);
		    Resources rsrcQuota = Resources.piecewiseMin(rate, avaiRes);
		    
//		    if (rate.greaterOrEqual(avaiRes)){
//		      queuesNeedAlloc.add(q);
//		    }
		    
		    if (!rate.smaller(admissionControlRes) 
		    		&& avaiRes.greaterOrEqual(rsrcQuota)){ // admission control condition.
		    	//TODO add 2nd admission condition
		    	isAdmitted = true;
		    	admissionControlRes.subtract(rate);	
		    	Resources currentUsage = q.getResourceUsage();
//    			Resources moreRes = Resources.subtractPositivie(rsrcQuota, currentUsage);
		    	Resources moreRes = Resources.piecewiseMin(rsrcQuota, avaiRes);
    			Resources realResRemain = Simulator.cluster.getClusterResAvail();
    			Resources remain = q.assign(moreRes);
    			realResRemain = Simulator.cluster.getClusterResAvail();
    			// assign the task
    			rsrcQuota = Resources.subtract(rsrcQuota, remain);
    			q.setRsrcQuota(rsrcQuota);
    			speedFairRes = Resources.sum(speedFairRes, rsrcQuota);
//    			avaiRes = Resources.subtractPositivie(avaiRes, Resources.subtract(moreRes, remain));
    			avaiRes = Resources.subtractPositivie(avaiRes, rsrcQuota);
//    			Output.debugln(DEBUG, "[SpeedFairScheduler] Step 1: SpeedFair share: " + q.getQueueName() + " " + q.getRsrcQuota());
		    }
		  } 
		  
		  if(!isAdmitted){
		    queuesNeedAlloc.add(q);
		  }
		}
		
		// use DRF for the remaining resources
//		onlineDRFShare(flexibleResources, queuesNeedAlloc);
//		Resources remainingResources = Resources.subtract(clusterTotCapacity, speedFairRes);
		Resources remainingResources = Resources.clone(avaiRes);
		Resources realResRemain = Simulator.cluster.getClusterResAvail();
		if (remainingResources.distinct(Resources.ZEROS))
		  DRFScheduler.onlineDRFShare(remainingResources, queuesNeedAlloc);
	}

	public void computeResShareOffline() {
		if(Simulator.CURRENT_TIME>=Globals.DEBUG_START && Simulator.CURRENT_TIME<=Globals.DEBUG_END){
			DEBUG = true;
		}else
			DEBUG = false;
			
//		Output.debugln(DEBUG, "[SpeedFairScheduler] STEP_TIME:" + Simulator.CURRENT_TIME);
		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}
		Resources flexibleResources = Simulator.cluster.getClusterMaxResAlloc();
		
		List<JobQueue> queuesNeedAlloc = new LinkedList<JobQueue>();
		
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			// compute the rsrcQuota based on the guarateed rate.
			Resources rsrcQuota = Resources.piecewiseMin(q.getMinService(Simulator.CURRENT_TIME), q.getMaxDemand());
			if (q.getMaxDemand().greater(rsrcQuota))
				queuesNeedAlloc.add(q);
			q.setRsrcQuota(rsrcQuota);
			flexibleResources = Resources.subtractPositivie(flexibleResources, q.getRsrcQuota());
			Output.debugln(DEBUG, "[SpeedFairScheduler] Step 1: compute min share: " + q.getQueueName() + " " + q.getRsrcQuota());
		}

		computeDRFShare(flexibleResources, queuesNeedAlloc);
		
		// TODO: sort queues for interactive jobs.
		// Resource admission control for the queues.
		Resources availRes = Simulator.cluster.getClusterMaxResAlloc();
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
			if (!fit) {
				Resources newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
				q.setRsrcQuota(newQuota);
			}
			Output.debugln(DEBUG, "[SpeedFairScheduler] Step 2: allocate share: " + q.getQueueName() + " " + q.getRsrcQuota());
			q.receivedResourcesList.add(q.getRsrcQuota());

			// TODO: Fair share the resources among the jobs. 
			
			Resources remain = q.getRsrcQuota();
			fifoShareForJobs(q, remain);
			
			Output.debugln(DEBUG, "[SpeedFairScheduler] Step 2: FIFO share @ " + q.getQueueName() + " " + q.getJobsQuota());
			availRes = Resources.subtract(availRes, q.getRsrcQuota());
		}
		
		// assign the resource
		
		Simulator.queueSched.adjustShares();
		
		for (BaseDag job : Simulator.runningJobs) {
			if (job.rsrcQuota.distinct(Resources.ZEROS)) {
				Simulator.intraJobSched.schedule((StageDag) job);
			}
		}
	}
	
	private void computeDRFShare(Resources flexibleResources, List<JobQueue> runningQueues){
		HashMap<String, Resources> resDemandsQueues = new HashMap<String, Resources>();
		double factor = 0.0;
		for (JobQueue q : runningQueues) {
			factor += q.getSpeedFairWeight();
		}
		
		for (JobQueue q : runningQueues) {
        // 1. compute it's avg. resource demand vector it not already computed
				Resources remainDemand =  Resources.subtract(q.getMaxDemand(), q.getRsrcQuota());
        Resources avgResDemandDag = Resources.clone(remainDemand);
        avgResDemandDag.divide(factor);

        // 2. normalize every dimension to the total capacity of the cluster
        avgResDemandDag.divide(flexibleResources);

        // 3. scale the resource demand vector to the max resource
        avgResDemandDag.divide(avgResDemandDag.max());
        avgResDemandDag.multiply(q.getSpeedFairWeight());
        
        Resources resDemand = Resources.piecewiseMin(remainDemand, avgResDemandDag); // increase utilization.
        
        resDemandsQueues.put(q.getQueueName(), resDemand);
    }

    // 4. sum it up across every dimension
    Resources sumDemandsRunQueues = new Resources(0.0);
    for (JobQueue q : runningQueues) {
      sumDemandsRunQueues.addWith(resDemandsQueues.get(q.getQueueName()));
    }

    // 5. find the max sum
    if (sumDemandsRunQueues.max() > Globals.ZERO){
    	int maxIdx = sumDemandsRunQueues.idOfMaxResource();
	    double drfShare = flexibleResources.resource(maxIdx) / sumDemandsRunQueues.max(); //idx of max for flexibleResources.
	    
	    for (JobQueue q : runningQueues) {
	    	Resources drfQuota = Resources.clone(resDemandsQueues.get(q.getQueueName()));
	      drfQuota.multiply(drfShare);
	    	q.setRsrcQuota(Resources.sum(drfQuota, q.getRsrcQuota())); // Note: Different with DRFScheduler.computeDRFShare()
	  		Output.debugln(DEBUG, "[SpeedFairScheduler] DRF for remaining resources allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
	    }
    } else
    	Output.debugln(DEBUG, "[SpeedFairScheduler] DRF is not necessary for EMPTY resources");
	}
	
	public void fifoShareForJobs(JobQueue q, Resources availRes){
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
			Output.debugln(DEBUG, "[SpeedFairScheduler] Allocated to job:" + job.dagId + " @ " + job.getQueueName() +" " + job.rsrcQuota);
		}
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
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
