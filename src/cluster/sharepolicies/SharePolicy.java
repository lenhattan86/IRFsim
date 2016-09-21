package cluster.sharepolicies;

public abstract class SharePolicy {
	
	private static final boolean DEBUG = true;

  public String sharingPolicyName;

  public SharePolicy(String policyName) {
    sharingPolicyName = policyName;
  }

  // recompute the resource share allocated for every job
  public void computeResShare() {
  }

  public void packTasks() {}
}
