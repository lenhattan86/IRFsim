package cluster.sharepolicies;

import cluster.datastructures.BaseDag;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;

public class SpeedFairSharePolicy extends SharePolicy {

  Resources clusterTotCapacity = null;

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

    // update the resourceShareAllocated for every running job
    for (BaseDag job : Simulator.runningJobs) {
    	Resources guaranteedResource = job.serviceCurve.getMinReqService(Simulator.CURRENT_TIME);
    	job.rsrcQuota = Resources.subtractNonZero(guaranteedResource, job.receivedService);
    }
    
    // TODO: equally share the remaining resources
  }
}
