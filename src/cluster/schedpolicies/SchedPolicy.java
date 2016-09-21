package cluster.schedpolicies;

import cluster.cluster.Cluster;
import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;

public abstract class SchedPolicy {
	
	private static final boolean DEBUG = true;

  public Cluster cluster;

  public SchedPolicy(Cluster _cluster) {
    cluster = _cluster;
  }

  public abstract void schedule(StageDag dag);

  public abstract double planSchedule(StageDag dag, Resources leftOverResources);
}
