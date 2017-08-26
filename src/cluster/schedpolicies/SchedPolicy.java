package cluster.schedpolicies;

import cluster.cluster.Cluster;
import cluster.datastructures.Resource;
import cluster.datastructures.MLJob;

public abstract class SchedPolicy {
	
	private static final boolean DEBUG = true;

  public Cluster cluster;

  public SchedPolicy(Cluster _cluster) {
    cluster = _cluster;
  }

  public abstract void schedule(MLJob dag);

  public abstract double planSchedule(MLJob dag, Resource leftOverResources);
}
