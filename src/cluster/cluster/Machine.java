package cluster.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cluster.datastructures.BaseJob;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.datastructures.Task;
import cluster.simulator.Simulator;
import cluster.utils.Utils;

public class Machine {

  private static final boolean DEBUG = true;

  int machineId;

  boolean execMode;
  public Cluster cluster;

  public double currentTime = 0;

  // max capacity of this machine
  // default is 1.0 across all dimensions
  Resource maxResAlloc;
  Resource totalResAlloc;
  // map: expected completion time -> Task context
  public Map<Task, Double> runningTasks;

  public Machine(int machineId, Resource size, boolean execMode) {
    // LOG.info("Initialize machine: "+machineId+" size:"+size);
    this.machineId = machineId;
    this.execMode = execMode;
    this.currentTime = Simulator.CURRENT_TIME;
    totalResAlloc = new Resource();
    assert size != null;
    maxResAlloc = Resources.clone(size);
    runningTasks = new HashMap<Task, Double>();
  }

  public double earliestFinishTime() {
    double earliestFinishTime = Double.MAX_VALUE;
    for (Double finishTime : runningTasks.values()) {
      earliestFinishTime = Math.min(earliestFinishTime, finishTime);
    }
    return earliestFinishTime;
  }

  public double earliestStartTime() {
    double earliestStartTime = Double.MAX_VALUE;
    for (Double startTime : runningTasks.values()) {
      earliestStartTime = Math.min(earliestStartTime, startTime);
    }
    return earliestStartTime;
  }

  public void assignTask(int dagId, int taskId, double taskDuration, Resource taskResources) {
    currentTime = execMode ? Simulator.CURRENT_TIME : currentTime;

    // if task does not fit -> reject it
    boolean fit = getTotalResAvail().greaterOrEqual(taskResources);
    if (!fit) return;

    // 1. update the amount of resources allocated
    totalResAlloc.addWith(taskResources);

    // 2. compute the expected time for this task
    // TODO: compute the expected task completion time.
    double expTaskComplTime = Utils.roundDefault(currentTime + taskDuration);
    Task t = new Task(dagId, taskId, taskDuration, null);
    t.usage = taskResources;
    runningTasks.put(t, expTaskComplTime);
  }

/*  public Resource preemptTask(Task task) {
    currentTime = execMode ? Simulator.CURRENT_TIME : currentTime;

    runningTasks.remove(task);
    BaseJob dag = Simulator.getDag(task.dagId);
    // TODO: may not need this
    dag.getRsrcInUse().subtract(task.demand);

    // remove the task from runnable and put it in running
    dag.runningTasks.add(task.dagId);
    // unallocJob.launchedTasksNow.add(taskId);
    dag.runnableTasks.remove(task.dagId);
    // TODO: do not need this
    return task.demand;
  }*/

  // [dagId -> List<TaskId>]
  public Map<Integer, List<Integer>> finishTasks(double... finishTime) {

    currentTime = execMode ? Simulator.CURRENT_TIME : (Double) finishTime[0];

    Map<Integer, List<Integer>> tasksFinished = new HashMap<Integer, List<Integer>>();

    Iterator<Map.Entry<Task, Double>> iter = runningTasks.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<Task, Double> entry = iter.next();

      if (entry.getValue() <= currentTime) {
        Task t = entry.getKey();
        Resource taskRes = t.usage;
        // TODO: update the remaining resource
        totalResAlloc.subtract(taskRes);

        // update resource freed from corresponding job
        BaseJob dag = Simulator.getDag(t.dagId);
        // TODO: update the remaining resource
        dag.getRsrcInUse().subtract(taskRes);

        if (tasksFinished.get(t.dagId) == null) {
          tasksFinished.put(t.dagId, new ArrayList<Integer>());
        }
        tasksFinished.get(t.dagId).add(t.taskId);
        iter.remove();
      }
    }
    return tasksFinished;
  }

  public Resource getTotalResAvail() {
    return Resources.subtract(maxResAlloc, totalResAlloc);
  }

  public int getMachineId() {
    return this.machineId;
  }

  public Task getLastRunningTask(String queueName) {
    Task res = null;
    Iterator<Map.Entry<Task, Double>> iter = runningTasks.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<Task, Double> entry = iter.next();
      Task task = entry.getKey();
      String qName = Simulator.getDag(task.dagId).getQueueName();
      if (qName.equals(queueName)) {
        res = task;
      }
    }
    return res;
  }
}
