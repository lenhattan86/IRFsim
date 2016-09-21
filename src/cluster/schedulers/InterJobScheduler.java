package cluster.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cluster.datastructures.BaseDag;
import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;
import cluster.sharepolicies.DRFSharePolicy;
import cluster.sharepolicies.FairSharePolicy;
import cluster.sharepolicies.SJFSharePolicy;
import cluster.sharepolicies.SharePolicy;
import cluster.sharepolicies.SpeedFairSharePolicy;
import cluster.sharepolicies.TetrisUniversalSched;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.SharingPolicy;
import cluster.utils.Output;

// responsible for recomputing the resource share and update
// the resource counters for every running job
public class InterJobScheduler {

	private final static boolean DEBUG = true;

	public SharePolicy resSharePolicy;

	public InterJobScheduler() {

		switch (Globals.INTER_JOB_POLICY) {
		case Fair:
			resSharePolicy = new FairSharePolicy("Fair");
			break;
		case DRF:
			resSharePolicy = new DRFSharePolicy("DRF");
			break;
		case SJF:
			resSharePolicy = new SJFSharePolicy("SJF");
			break;
		case TETRIS_UNIVERSAL:
			resSharePolicy = new TetrisUniversalSched("Tetris_Universal");
			break;
		case SpeedFair:
			resSharePolicy = new SpeedFairSharePolicy();
			break;
		default:
			System.err.println("Unknown sharing policy");
		}
	}

	public void schedule() {
		// compute how much share each DAG should get
		resSharePolicy.computeResShare();
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

//		// compute the real resources having been used.
//		for (BaseDag dag : Simulator.runningJobs) {
//			dag.receivedService.addUsage(Resources.minRes(dag.rsrcInUse, dag.currResDemand()));
//			Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- dag.rsrcInUse: " + dag.rsrcInUse + " -- dag.currResDemand(): "
//					+ dag.currResDemand());
//			Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- Received Resource: " + dag.receivedService);
//		}
//		Output.debugln(DEBUG, "availRes " + availRes );
	}

	// return the jobs IDs based on different policies
	// SJF: return ids should be based on SRTF
	// All other policies should be based on Fairness considerations
	public List<Integer> orderedListOfJobsBasedOnPolicy() {
		List<Integer> runningDagsIds = new ArrayList<Integer>();
		if (Globals.INTER_JOB_POLICY == SharingPolicy.SJF) {
			final Map<Integer, Double> runnableDagsComparatorVal = new HashMap<Integer, Double>();
			for (BaseDag dag : Simulator.runningJobs) {
				runningDagsIds.add(dag.dagId);
				runnableDagsComparatorVal.put(dag.dagId, ((StageDag) dag).srtfScore());
			}
			// if (Globals.INTER_JOB_POLICY == SharingPolicy.SJF) {
			Collections.sort(runningDagsIds, new Comparator<Integer>() {
				public int compare(Integer arg0, Integer arg1) {
					Double val0 = runnableDagsComparatorVal.get(arg0);
					Double val1 = runnableDagsComparatorVal.get(arg1);
					if (val0 < val1)
						return -1;
					if (val0 > val1)
						return 1;
					return 0;
				}
			});
		}

		else {
			final Map<Integer, Resources> runnableDagsComparatorVal = new HashMap<Integer, Resources>();
			for (BaseDag dag : Simulator.runningJobs) {
				runningDagsIds.add(dag.dagId);
				Resources farthestFromShare = Resources.subtract(dag.rsrcQuota, dag.rsrcInUse);
				runnableDagsComparatorVal.put(dag.dagId, farthestFromShare);
			}
			Collections.sort(runningDagsIds, new Comparator<Integer>() {
				public int compare(Integer arg0, Integer arg1) {
					Resources val0 = runnableDagsComparatorVal.get(arg0);
					Resources val1 = runnableDagsComparatorVal.get(arg1);
					return val0.compareTo(val1);
				}
			});
		}

		return runningDagsIds;
	}
}