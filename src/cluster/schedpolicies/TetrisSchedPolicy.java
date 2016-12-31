package cluster.schedpolicies;

import cluster.cluster.Cluster;
import cluster.cluster.Machine;
import cluster.datastructures.Resource;
import cluster.datastructures.StageDag;
import cluster.utils.Pair;

public class TetrisSchedPolicy extends SchedPolicy {
	
	private static final boolean DEBUG = true;

  // flag which specifies if duration is considered
  // during the cosine similarity computation
  boolean inclDurInCosineSim = false;

  public TetrisSchedPolicy(Cluster cluster) {
    super(cluster);
  }

  @Override
  public void schedule(StageDag dag) {

    if (dag.runnableTasks.isEmpty()) {
      return;
    }

    Pair<Integer, Integer> bestTaskToPack;

    // RG: this can be very expensive - TODO
    // among the runnable tasks, compute the best packing score
    // for each of them w.r.t to every machine they fit.
    while ((bestTaskToPack = computeBestTaskToPack(dag)).first() >= 0) {
      int taskToPack = bestTaskToPack.first();
      int machineTaskToPack = bestTaskToPack.second();

      // try to assign the next task on machineTaskTopack
      boolean assigned = cluster.assignTask(machineTaskToPack, dag.dagId,
          taskToPack, dag.duration(taskToPack), dag.rsrcDemands(taskToPack));

      if (assigned) {
        // remove the task from runnable and put it in running
        dag.runningTasks.add(taskToPack);
        dag.runnableTasks.remove((Integer) taskToPack);
      }
    }
  }

  Pair<Integer, Integer> computeBestTaskToPack(StageDag dag) {
    Pair<Integer, Integer> bestTaskToPack = Pair.createPair(-1, -1);

    if (dag.runnableTasks.isEmpty()) {
      return bestTaskToPack;
    }

    double maxScore = Double.MIN_VALUE;
    for (Integer taskId : dag.runnableTasks) {

      double maxScoreTask = Double.MIN_VALUE;
      int maxScoreTaskMachineId = -1;
      Resource taskRes = dag.rsrcDemands(taskId);
      double taskDur = dag.duration(taskId);
      boolean fit = dag.currResShareAvailable().greaterOrEqual(taskRes);
      if (!fit)
        continue;

      for (Machine machine : cluster.getMachines()) {
        Resource machineRes = machine.getTotalResAvail();
        double scoreTaskMachine = Resource.dotProduct(taskRes, machineRes);
        scoreTaskMachine = inclDurInCosineSim ? taskDur + scoreTaskMachine
            : scoreTaskMachine;
        if (maxScoreTask < scoreTaskMachine) {
          maxScoreTask = scoreTaskMachine;
          maxScoreTaskMachineId = machine.getMachineId();
        }
      }

      if (maxScore < maxScoreTask) {
        maxScore = maxScoreTask;
        bestTaskToPack.setFirst(taskId);
        bestTaskToPack.setSecond(maxScoreTaskMachineId);
      }
    }
    return bestTaskToPack;
  }

  @Override
  public double planSchedule(StageDag dag, Resource leftOverResources) {
    return -1;
  }
}
