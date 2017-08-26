package cluster.sharepolicies;

import cluster.datastructures.BaseJob;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.datastructures.MLJob;
import cluster.simulator.Simulator;

public class SJFSharePolicy extends SharePolicy {
	
	private static final boolean DEBUG = true;

  public SJFSharePolicy(String policyName) {
    super(policyName);
  }

  // in this policy, give all the resources to the job with min(SRTF)
  @Override
  public void computeResShare() {
    if (Simulator.runningJobs.size() == 0) {
      return;
    }

    // sort the jobs in SRTF order
    // give the entire cluster share to the job with smallest SRTF
    double shortestJobVal = Double.MAX_VALUE;
    int shortestJobId = -1;
    for (BaseJob job : Simulator.runningJobs) {
      double jobSrtf = ((MLJob) job).srtfScore();
      if (jobSrtf < shortestJobVal) {
        shortestJobVal = jobSrtf;
        shortestJobId = job.dagId;
      }
    }

    assert (shortestJobVal != Double.MAX_VALUE);
    for (BaseJob job : Simulator.runningJobs) {
      if (job.dagId == shortestJobId) {
        job.rsrcQuota = Resources.clone(Simulator.cluster
            .getClusterMaxResAlloc());
      } else {
        job.rsrcQuota = new Resource(0.0);
      }
      // Output.debugln(DEBUG,"Allocated to job:" + job.dagId + " share:"
      // + job.rsrcQuota);
    }
  }
}
