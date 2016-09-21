package cluster.schedulers;

import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;
import cluster.schedpolicies.BFSSchedPolicy;
import cluster.schedpolicies.CPSchedPolicy;
import cluster.schedpolicies.CarbyneSchedPolicy;
import cluster.schedpolicies.RandomSchedPolicy;
import cluster.schedpolicies.SchedPolicy;
import cluster.schedpolicies.SpeedFairSchedPolicy;
import cluster.schedpolicies.TetrisSchedPolicy;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;

// responsible for scheduling tasks inside a Job
// it can adopt various strategies such as Random, Critical Path
// BFS, Tetris, Graphene, etc.

// return unnecessary resources to the resource pool
public class IntraJobScheduler {
	
	private static final boolean DEBUG = true;

  public SchedPolicy resSchedPolicy;

  public IntraJobScheduler() {

    switch (Globals.INTRA_JOB_POLICY) {
    case Random:
      resSchedPolicy = new RandomSchedPolicy(Simulator.cluster);
      break;
    case BFS:
      resSchedPolicy = new BFSSchedPolicy(Simulator.cluster);
      break;
    case CP:
      resSchedPolicy = new CPSchedPolicy(Simulator.cluster);
      break;
    case Tetris:
      resSchedPolicy = new TetrisSchedPolicy(Simulator.cluster);
      break;
    case Carbyne:
      resSchedPolicy = new CarbyneSchedPolicy(Simulator.cluster);
    case SpeedFair:
      resSchedPolicy = new SpeedFairSchedPolicy(Simulator.cluster);  
      break;
    default:
      System.err.println("Unknown sharing policy");
    }
  }

  public void schedule(StageDag dag) {
    // while tasks can be assigned in my resource
    // share quanta, on any machine, keep assigning
    // otherwise return
    resSchedPolicy.schedule(dag);
  }

  public double planSchedule(StageDag dag, Resources leftOverResources) {
    return resSchedPolicy.planSchedule(dag, leftOverResources);
  }
}
