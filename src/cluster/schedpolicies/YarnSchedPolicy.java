package cluster.schedpolicies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import cluster.cluster.Cluster;
import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import cluster.utils.Output;

public class YarnSchedPolicy extends SchedPolicy {

	private static final boolean DEBUG = true;

	public YarnSchedPolicy(Cluster cluster) {
		super(cluster);
	}

	@Override
	public void schedule(final StageDag dag) { // TODO: add preemption

		if (dag.runnableTasks.isEmpty())
			return;

		ArrayList<Integer> rtCopy = new ArrayList<Integer>(dag.runnableTasks);

		// among the runnable tasks:
		// pick one which has the largest CP
		// as the next task to schedule
		Collections.sort(rtCopy, new Comparator<Integer>() {
			public int compare(Integer arg0, Integer arg1) {
				Double val0 = dag.BFSOrder.get(arg0);
				Double val1 = dag.BFSOrder.get(arg1);
				return (int) (val1 - val0);
			}
		});

		Iterator<Integer> iter = rtCopy.iterator();
		while (iter.hasNext()) {
			int taskId = iter.next();

			// discard tasks whose resource requirements are larger than total share
			Resources currResShareAvail = dag.currResShareAvailable();
			Resources taskDemand = dag.rsrcDemands(taskId);
			boolean fit = currResShareAvail.greaterOrEqual(taskDemand);
			if (!fit) {
				continue;
			}

			// TODO: try to assign the next task on a machine. This function needs to be
			// modified for multiple machines. It results in
			// low utilization as it cannot optimally allocate the resources on each
			// machine.
			boolean assigned = cluster.assignTask(dag.dagId, taskId, dag.duration(taskId), dag.rsrcDemands(taskId));

			if (assigned) {
				// remove the task from runnable and put it in running
				dag.runningTasks.add(taskId);
				dag.launchedTasksNow.add(taskId);
				iter.remove();
				dag.runnableTasks.remove(taskId);
			}
		}

		dag.launchedTasksNow.clear();
	}

	@Override
	public double planSchedule(StageDag dag, Resources leftOverResources) {
		return -1;
	}

}
