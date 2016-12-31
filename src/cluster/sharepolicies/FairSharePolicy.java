package cluster.sharepolicies;

import cluster.datastructures.BaseDag;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;

public class FairSharePolicy extends SharePolicy {
	
	private static final boolean DEBUG = true;

  Resource clusterTotCapacity = null;

  public FairSharePolicy(String policyName) {
    super(policyName);
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

    Resource quotaRsrcShare = Resources.divide(clusterTotCapacity,
        numJobsRunning);

    // update the resourceShareAllocated for every running job
    for (BaseDag job : Simulator.runningJobs) {
      job.rsrcQuota = quotaRsrcShare;
      // Output.debugln(DEBUG,"Allocated to job:" + job.dagId + " share:"
      // + job.rsrcQuota);
    }
  }
}
