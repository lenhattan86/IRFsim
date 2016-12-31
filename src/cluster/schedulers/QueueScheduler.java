package cluster.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import cluster.cluster.Cluster;
import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import queue.schedulers.DRFScheduler;
import queue.schedulers.FairScheduler;
import queue.schedulers.Scheduler;
import queue.schedulers.SpeedFairScheduler;
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

		final Map<Integer, Resource> unhappyDagsDistFromResShare = new HashMap<Integer, Resource>();
		for (BaseDag dag : Simulator.runningJobs) {
			if (!dag.rsrcQuota.distinct(dag.getRsrcInUse())) {
				continue;
			}

			if (dag.getRsrcInUse().greaterOrEqual(dag.rsrcQuota)) {
				// TODO: do we need to deal with this case: this dag has more resources than fairshare.
			} else {
				Resource farthestFromShare = Resources.subtract(dag.rsrcQuota, dag.getRsrcInUse());
				unhappyDagsIds.add(dag.dagId);
				unhappyDagsDistFromResShare.put(dag.dagId, farthestFromShare);
			}
		}
		Collections.sort(unhappyDagsIds, new Comparator<Integer>() {
			public int compare(Integer arg0, Integer arg1) {
				Resource val0 = unhappyDagsDistFromResShare.get(arg0);
				Resource val1 = unhappyDagsDistFromResShare.get(arg1);
				return val0.compareTo(val1);
			}
		});

		// now try to allocate the available resources to dags in this order
		Resource availRes = Resources.clone(Simulator.cluster.getClusterResAvail());

		for (int dagId : unhappyDagsIds) {
			if (!availRes.greater(new Resource(0.0)))
				break;

			StageDag dag = Simulator.getDag(dagId);

			Resource rsrcReqTillShare = unhappyDagsDistFromResShare.get(dagId);

			if (availRes.greaterOrEqual(rsrcReqTillShare)) {
				availRes.subtract(rsrcReqTillShare);
			} else {
				Resource toGive = Resources.piecewiseMin(availRes, rsrcReqTillShare);
				dag.rsrcQuota.copy(toGive);
				availRes.subtract(toGive);
			}
		}
		
	}
	
}
