package cluster.sharepolicies;

import java.util.LinkedList;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.utils.Output;

public class SpeedFairSharePolicy extends SharePolicy {

	private static final boolean DEBUG = true;

	Resource clusterTotCapacity = null;

	public SpeedFairSharePolicy() {
		super("SpeedFair");
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
	}

	// FairShare = 1 / N across all dimensions
	// N - total number of running jobs
	@Override
	public void computeResShare() {
		int numJobsRunning = Simulator.runningJobs.size();
		if (numJobsRunning == 0) {
			return;
		}

		Resource clusterResQuotaAvail = Simulator.cluster.getClusterResQuotaAvail();

		// TODO: sort the runningJobs

		// update the resourceShareAllocated for every running job
		// assign the resources based on service curves.
		Queue<BaseDag> unhappyRunningJobs = new LinkedList<BaseDag>();
		for (BaseDag job : Simulator.runningJobs) {
			//TODO: change job.jobStartTime to job.jobStartRunningTime (dynamic for each job).
			Resource guaranteedResource = job.serviceCurve.getMinReqService(Simulator.CURRENT_TIME-job.jobStartTime); 
			Resource resToBeShared = Resources.subtractPositivie(guaranteedResource, job.receivedService);
			boolean fit = clusterResQuotaAvail.greater(resToBeShared);
			if (fit) {
				job.rsrcQuota = resToBeShared;
				clusterResQuotaAvail = Resources.subtract(clusterResQuotaAvail, resToBeShared);
			} else {
				job.rsrcQuota = new Resource(0.0); // unable to allocate the resources
				unhappyRunningJobs.add(job);
			}
		}

		// equally share the remaining resources to the unhappy jobs or all jobs.
		int numUnhappyJobs = unhappyRunningJobs.size();
		if (numUnhappyJobs > 0) {
			Output.debugln(DEBUG, "number of unhappy jobs: " + numUnhappyJobs);
			if (clusterResQuotaAvail.greater(new Resource(0))) {
				Resource quotaRsrcShare = Resources.divide(clusterResQuotaAvail, unhappyRunningJobs.size());
				for (BaseDag job : unhappyRunningJobs) {
					job.rsrcQuota.addRes(quotaRsrcShare);
				}
			}
		} else {
			if (clusterResQuotaAvail.greater(new Resource(0))) {
				Resource quotaRsrcShare = Resources.divide(clusterResQuotaAvail, numJobsRunning);
				for (BaseDag job : Simulator.runningJobs) {
					job.rsrcQuota.addRes(quotaRsrcShare);
				}
			}
		}
		
		//TODO: share the remaining resources on demand
	}
}
