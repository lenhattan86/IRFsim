package cluster.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cluster.datastructures.BaseDag;
import cluster.datastructures.Resource;
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
			final Map<Integer, Resource> runnableDagsComparatorVal = new HashMap<Integer, Resource>();
			for (BaseDag dag : Simulator.runningJobs) {
				runningDagsIds.add(dag.dagId);
				Resource farthestFromShare = Resources.subtract(dag.rsrcQuota, dag.getRsrcInUse());
				runnableDagsComparatorVal.put(dag.dagId, farthestFromShare);
			}
			Collections.sort(runningDagsIds, new Comparator<Integer>() {
				public int compare(Integer arg0, Integer arg1) {
					Resource val0 = runnableDagsComparatorVal.get(arg0);
					Resource val1 = runnableDagsComparatorVal.get(arg1);
					return val0.compareTo(val1);
				}
			});
		}

		return runningDagsIds;
	}
	
}
