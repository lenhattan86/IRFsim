package cluster.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import queue.schedulers.DRFScheduler;
import queue.schedulers.FairScheduler;
import queue.schedulers.Scheduler;
import queue.schedulers.SpeedFairScheduler;
import queue.schedulers.StrictScheduler;
import cluster.simulator.Main.Globals;

public class QueueScheduler {
	private final static boolean DEBUG = true;

	public Scheduler scheduler;

	public QueueScheduler() {

		switch (Globals.QUEUE_SCHEDULER) {
		case Fair:
			scheduler = new FairScheduler();
			break;
		case DRF:
			scheduler = new DRFScheduler();
			break;
		case SpeedFair:
			scheduler = new SpeedFairScheduler();
			break;
		case Strict:
			scheduler = new StrictScheduler();
			break;
		default:
			System.err.println("Unknown sharing policy");
		}
	}

	public void schedule() {
		// compute how much share each queue should get
		scheduler.computeResShare();
	}

	public void adjustShares() {
		List<Integer> unhappyDagsIds = new ArrayList<Integer>();

		final Map<Integer, Resources> unhappyDagsDistFromResShare = new HashMap<Integer, Resources>();
		for (BaseDag dag : Simulator.runningJobs) {
			if (!dag.rsrcQuota.distinct(dag.rsrcInUse)) {
				continue;
			}

			if (dag.rsrcInUse.greaterOrEqual(dag.rsrcQuota)) {
				// TODO: do we need to deal with this case: this dag has more resources than fairshare.
			} else {
				Resources farthestFromShare = Resources.subtract(dag.rsrcQuota, dag.rsrcInUse);
				unhappyDagsIds.add(dag.dagId);
				unhappyDagsDistFromResShare.put(dag.dagId, farthestFromShare);
			}
		}
		Collections.sort(unhappyDagsIds, new Comparator<Integer>() {
			public int compare(Integer arg0, Integer arg1) {
				Resources val0 = unhappyDagsDistFromResShare.get(arg0);
				Resources val1 = unhappyDagsDistFromResShare.get(arg1);
				return val0.compareTo(val1);
			}
		});

		// now try to allocate the available resources to dags in this order
		Resources availRes = Resources.clone(Simulator.cluster.getClusterResAvail());

		for (int dagId : unhappyDagsIds) {
			if (!availRes.greater(new Resources(0.0)))
				break;

			StageDag dag = Simulator.getDag(dagId);

			Resources rsrcReqTillShare = unhappyDagsDistFromResShare.get(dagId);

			if (availRes.greaterOrEqual(rsrcReqTillShare)) {
				availRes.subtract(rsrcReqTillShare);
			} else {
				Resources toGive = Resources.piecewiseMin(availRes, rsrcReqTillShare);
				dag.rsrcQuota.copy(toGive);
				availRes.subtract(toGive);
			}
		}
		
	}
	
}
