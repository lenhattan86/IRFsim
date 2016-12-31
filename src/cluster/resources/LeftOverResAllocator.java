package cluster.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cluster.datastructures.BaseDag;
import cluster.datastructures.Resource;
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import cluster.utils.Output;

// keep a pool of resources available
// allocate to jobs based on various policies

// it has to ensure that all leftover resources are actually used
// by tasks
public class LeftOverResAllocator {

	private static final boolean DEBUG = true;
  public Resource leftOverRes;

  public LeftOverResAllocator() {
    leftOverRes = new Resource(0.0);
  }

  private void takeStockOfLeftOverRsrcs() {
    leftOverRes = Simulator.cluster.getClusterResAvail();
  }

  public void allocLeftOverRsrcs() {

    takeStockOfLeftOverRsrcs();

    // if nothing is leftover -> nothing to do
    if (!leftOverRes.greater(new Resource(0.0))) {
      return;
    }

    // RG-MC: TODO FIX ME - look at jobs who can finish as of now
    // sort jobs in SRTF order
    List<Integer> runningDagsIds = new ArrayList<Integer>();
    final Map<Integer, Double> runnableDagsSrtf = new HashMap<Integer, Double>();
    for (BaseDag dag : Simulator.runningJobs) {
      runningDagsIds.add(dag.dagId);
      runnableDagsSrtf.put(dag.dagId, ((StageDag) dag).srtfScore());
    }
    Collections.sort(runningDagsIds, new Comparator<Integer>() {
      public int compare(Integer arg0, Integer arg1) {
        Double val0 = runnableDagsSrtf.get(arg0);
        Double val1 = runnableDagsSrtf.get(arg1);
        if (val0 < val1)
          return -1;
        if (val0 > val1)
          return 1;
        return 0;
      }
    });

    // TODO: sort based on:
    // 1. jobs whose running tasks are the last ones
    // for every running task starting from tasks with longest remaining
    // duration increase one dimension at a time and check if rem. compl time
    // decrease keep doing this until longest task cannot be reduced more

    // allocate most to the jobs which improves the remaining time the most

    for (int dagId : runningDagsIds) {
      // Output.debugln(DEBUG,"\tdag:" + dagId + " leftOverRes:" + leftOverRes);
      if (!leftOverRes.greater(new Resource(0.0))) {
        Output.debugln(DEBUG,"\tno more leftover res -> stop reallocating");
        break;
      }
      StageDag origDag = Simulator.getDag(dagId);
      if (origDag.runnableTasks.isEmpty()) {
        continue;
      }

      double projectedTime = Simulator.intraJobSched.planSchedule(origDag,
          leftOverRes);
      if (projectedTime > 0) {
        Simulator.intraJobSched.schedule(origDag);
        takeStockOfLeftOverRsrcs();
      }
    }
    Output.debugln(DEBUG,"LeftOverRes remaining:" + leftOverRes);
  }
}
