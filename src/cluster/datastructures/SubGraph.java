package cluster.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cluster.utils.Interval;

public class SubGraph {

  private static final boolean DEBUG = true;
  public int id;
  public String name;
  public Interval vids;
  public int taskNum;

  public double vDuration;
  public InterchangableResourceDemand vDemands;
  public InterchangableResourceDemand reportDemands;
  public int arrivalTime =0; // relative Arrival Time
  
  private double beta = 1.0; // tranfer rate CPU -> GPU

  public double getBeta() {
    return beta;
  }

  public void setBeta(double beta) {
    this.beta = beta;
  }

  public Map<String, Dependency> parents, children;

  public SubGraph(String name, int id, Interval vids, double duration, int taskNum,
      InterchangableResourceDemand demand) {
    this.name = name;
    this.id = id;
    this.taskNum = taskNum;
    this.vids = new Interval(vids.begin, vids.end);

    parents = new HashMap<String, Dependency>();
    children = new HashMap<String, Dependency>();

    vDuration = duration;
    vDemands = new InterchangableResourceDemand(demand.getGpuCpu(), demand.getMemory(), demand.getBeta());
  }

  public static SubGraph clone(SubGraph stage) {
    
    SubGraph clonedStage = new SubGraph(stage.name, stage.id, stage.vids,
        stage.vDuration, stage.taskNum, stage.vDemands);

    clonedStage.parents = new HashMap<String, Dependency>();
    clonedStage.children = new HashMap<String, Dependency>();

    for (Map.Entry<String, Dependency> entry : stage.parents.entrySet()) {
      String stageName = entry.getKey();
      Dependency dep = entry.getValue();
      Dependency clonedDep = new Dependency(dep.parent, dep.child, dep.type,
          dep.parent_ids, dep.child_ids);
      clonedStage.parents.put(stageName, clonedDep);
    }

    for (Map.Entry<String, Dependency> entry : stage.children.entrySet()) {
      String stageName = entry.getKey();
      Dependency dep = entry.getValue();
      Dependency clonedDep = new Dependency(dep.parent, dep.child, dep.type,
          dep.parent_ids, dep.child_ids);
      clonedStage.children.put(stageName, clonedDep);
    }
    return clonedStage;
  }
  
  public static SubGraph clone(SubGraph stage, boolean isDependency) {
	    
	    SubGraph clonedStage = new SubGraph(stage.name, stage.id, stage.vids,
	        stage.vDuration, stage.taskNum, stage.vDemands);

	    clonedStage.parents = new HashMap<String, Dependency>();
	    clonedStage.children = new HashMap<String, Dependency>();

	    if(isDependency) {
		    for (Map.Entry<String, Dependency> entry : stage.parents.entrySet()) {
		      String stageName = entry.getKey();
		      Dependency dep = entry.getValue();
		      Dependency clonedDep = new Dependency(dep.parent, dep.child, dep.type,
		          dep.parent_ids, dep.child_ids);
		      clonedStage.parents.put(stageName, clonedDep);
		    }
	
		    for (Map.Entry<String, Dependency> entry : stage.children.entrySet()) {
		      String stageName = entry.getKey();
		      Dependency dep = entry.getValue();
		      Dependency clonedDep = new Dependency(dep.parent, dep.child, dep.type,
		          dep.parent_ids, dep.child_ids);
		      clonedStage.children.put(stageName, clonedDep);
		    }
	    }
	    return clonedStage;
	  }

  // task level convenience
  public double duration(int task) {
    assert (task >= vids.begin && task <= vids.end);
    return vDuration;
  }

  public InterchangableResourceDemand rsrcDemands(int task) {
    assert (task >= vids.begin && task <= vids.end);
    return vDemands;
  }
  
  public InterchangableResourceDemand reportDemands(int task) {
    assert (task >= vids.begin && task <= vids.end);
    return reportDemands;
  }
  
  /*public Resource rsrcUsage(int task) {
    assert (task >= vids.begin && task <= vids.end);
    //todo: get the real usage of a task
    return vDemands.convertToCPUDemand();
  }*/


  public Resource totalWork() {
    Resource totalWork = Resources.clone(vDemands.convertToGPUDemand());
    totalWork.multiply(vids.end - vids.begin + 1);
    return totalWork;
  }

  public Resource totalWorkInclDur() {
    Resource totalWork = Resources.clone(vDemands.convertToGPUDemand());
    totalWork.multiply((vids.end - vids.begin + 1) * vDuration);
    return totalWork;
  }

/*  public double stageContribToSrtfScore(Set<Integer> consideredTasks) {
    Set<Integer> stageTasks = new HashSet<Integer>();
    for (int task = vids.begin; task <= vids.end; task++) {
      stageTasks.add(task);
    }
    stageTasks.removeAll(consideredTasks);

    int remTasksToSched = stageTasks.size();
    if (remTasksToSched == 0) {
      return 0;
    }
    double l2Norm = Resource.l2Norm(vDemands);
    return l2Norm * remTasksToSched * vDuration;
  }*/
  
  public ArrayList<Integer> getTasks(){
    ArrayList<Integer> stageTasks = new ArrayList<Integer>();
    for (int task = vids.begin; task <= vids.end; task++) {
      stageTasks.add(task);
    }
    return stageTasks;
  }

  // RG: not optimal at all
  // for every stage, pass the entire list of running/finished tasks
  public double remTasks(Set<Integer> consideredTasks) {
    Set<Integer> stageTasks = new HashSet<Integer>();
    for (int task = vids.begin; task <= vids.end; task++) {
      stageTasks.add(task);
    }
    stageTasks.removeAll(consideredTasks);
    return stageTasks.size();
  }
  // end task level convenience
  
}
