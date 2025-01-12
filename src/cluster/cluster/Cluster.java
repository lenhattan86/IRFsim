package cluster.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Logger;

import cluster.datastructures.BaseJob;
import cluster.datastructures.Resource;
import cluster.datastructures.Task;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;

/**
 * describes the cluster characteristics both per cluster and per machine
 * properties
 */
public class Cluster {
	
	private static final boolean DEBUG = true;

  boolean execMode;
  public Map<Integer, Machine> machines;

  private static Logger LOG = Logger.getLogger(Cluster.class.getName());
  public Map<Integer, Double> availableTimes = null;
  public Map<Integer, Queue<BaseJob>> scheduledJobs = null;

  public Cluster(boolean state, Resource res) {
    execMode = state;
    machines = new TreeMap<Integer, Machine>();
    int numberMachines = execMode ? Globals.NUM_MACHINES : 1;
    for (int i = 0; i < numberMachines; ++i) {
      machines.put(i, new Machine(i, res, execMode));
    }
    availableTimes = new HashMap<Integer, Double>();
    int numberOfNodes = (int) (Globals.MACHINE_MAX_GPU + (Globals.MACHINE_MAX_CPU/Globals.CPU_PER_NODE) );
    for (int i=0; i< numberOfNodes; i++){
    	availableTimes.put(i, 0.0);
    }
    scheduledJobs = new HashMap<Integer, Queue<BaseJob>>();
    for (int i=0; i< numberOfNodes; i++){
    	scheduledJobs.put(i, new LinkedList<BaseJob>());
    }
  }

  public boolean assignTask(int machineId, int dagId, int taskId,
      double taskDuration, Resource taskResources) {
    // LOG.info("assign task: "+taskId+" from dag:"+dagId+" on machine:"+machineId);
    Machine machine = machines.get(machineId);
    assert (machine != null);
    boolean fit = machine.getTotalResAvail().greaterOrEqual(taskResources);
    if (!fit) {
      LOG.warning("ERROR; task should fit");
      return false;
    }
    //TODO: how to assign task?
    machine.assignTask(dagId, taskId, taskDuration, taskResources);
    return true;
  }

  // checks for fitting in resShare should already be done
  public boolean assignTask(int dagId, int taskId, double taskDuration,
      Resource taskResources) {

    // find the first machine where the task can fit
    // put it there
    for (Machine machine : machines.values()) {
      boolean fit = machine.getTotalResAvail().greaterOrEqual(taskResources);
      if (!fit)
        continue;
      machine.assignTask(dagId, taskId, taskDuration, taskResources);
      
      // update resource allocated to the corresponding job
      BaseJob dag = Simulator.getDag(dagId);
      dag.getRsrcInUse().addWith(taskResources);
      
   // remove the task from runnable and put it in running
  		dag.runningTasks.add(taskId);
  		// unallocJob.launchedTasksNow.add(taskId);
  		dag.runnableTasks.remove(taskId);
			
      return true;
    }
    return false;
  }
  public boolean assignTask(int dagId, int taskId, double taskDuration,
      Resource taskResources, int machineId) {

    // find the first machine where the task can fit
    // put it there
    for (Machine machine : machines.values()) {
      boolean fit = machine.getTotalResAvail().greaterOrEqual(taskResources);
      if (!fit)
        continue;
      machine.assignTask(dagId, taskId, taskDuration, taskResources);
      
      // update resource allocated to the corresponding job
      BaseJob dag = Simulator.getDag(dagId);
      dag.getRsrcInUse().addWith(taskResources);
      
   // remove the task from runnable and put it in running
  		dag.runningTasks.add(taskId);
  		// unallocJob.launchedTasksNow.add(taskId);
  		dag.runnableTasks.remove(taskId);
			
      return true;
    }
    return false;
  }
  
//checks for fitting in resShare should already be done
 /*public Resource preemptTask(String queueName) {
	 Resource returnedRes = null;
	 
   for (Machine machine : machines.values()) {
  	 Task task = machine.getLastRunningTask(queueName);
     if (task != null){
    	 returnedRes = machine.preemptTask(task);
     }
   }
   return returnedRes;
 }*/

  // return: [Key: dagId -- Value: List<taskId>]
  public Map<Integer, List<Integer>> finishTasks(double... earliestFinishTime) {

    // finish any task on this machine at the current time
    Map<Integer, List<Integer>> finishedTasks = new HashMap<Integer, List<Integer>>();

    for (Machine machine : machines.values()) {
      Map<Integer, List<Integer>> finishedTasksMachine = execMode ? machine
          .finishTasks() : machine.finishTasks((double) earliestFinishTime[0]);

      for (Map.Entry<Integer, List<Integer>> entry : finishedTasksMachine
          .entrySet()) {
        int dagId = entry.getKey();
        List<Integer> tasksFinishedDagId = entry.getValue();
        if (finishedTasks.get(dagId) == null) {
          finishedTasks.put(dagId, new ArrayList<Integer>());
        }
        finishedTasks.get(dagId).addAll(tasksFinishedDagId);
      }
      machine.currentTime = execMode ? 0.0 : (double) earliestFinishTime[0];
    }

    // update the currentTime with the earliestFinishTime on every machine
    return finishedTasks;
  }
  
  public void preemptAllTasks() {

    // finish any task on this machine at the current time
    for (Machine machine : machines.values()) {
    	Map<Task, Double> runningTasks = machine.runningTasks;
    	List<Task> tasksToBeRemove = new LinkedList<Task>();
      for (Map.Entry<Task, Double> entry : runningTasks.entrySet()) {
        // remove task from running tasks
      	Task t = entry.getKey();
      	BaseJob dag = Simulator.getDag(t.dagId);
      	if(!dag.isProfiling) { // not profiling jobs
	//      	machine.runningTasks.remove(t);      	
	      	tasksToBeRemove.add(t);
	      	machine.totalResAlloc.subtract(t.usage);
	        // update resource freed from corresponding job
	        
	        dag.runnableTasks.add(t.taskId);
	        dag.runningTasks.remove(t.taskId);
	        dag.preempt();
	        //remove job from running queues
	        dag.getQueue().addRunnableJob(dag);
	        dag.getQueue().removeRunningJob(dag);
//	        System.out.println("[INFO] preempt job "+ t.dagId + " at " + Simulator.CURRENT_TIME + " " + dag.getRsrcInUse());
      	}
      }
      for (Task t: tasksToBeRemove){
      	runningTasks.remove(t);
      }
    }
  }
  
  public Map<Task, Double> getCurrentRunningTasks() {
  	Map<Task, Double> runningTasks = new HashMap<Task, Double>();
    for (Machine machine : machines.values()) {
    	runningTasks.putAll(machine.runningTasks);
    }
    return runningTasks;
  }
  
  
  public Map<Integer, List<Integer>> finishTasksPrev(double... earliestFinishTime) {

    // finish any task on this machine at the current time
    Map<Integer, List<Integer>> finishedTasks = new HashMap<Integer, List<Integer>>();

    for (Machine machine : machines.values()) {
      Map<Integer, List<Integer>> finishedTasksMachine = execMode ? machine
          .finishTasks() : machine.finishTasks((double) earliestFinishTime[0]);

      for (Map.Entry<Integer, List<Integer>> entry : finishedTasksMachine
          .entrySet()) {
        int dagId = entry.getKey();
        List<Integer> tasksFinishedDagId = entry.getValue();
        if (finishedTasks.get(dagId) == null) {
          finishedTasks.put(dagId, new ArrayList<Integer>());
        }
        finishedTasks.get(dagId).addAll(tasksFinishedDagId);
      }
      machine.currentTime = execMode ? 0.0 : (double) earliestFinishTime[0];
    }

    // update the currentTime with the earliestFinishTime on every machine
    return finishedTasks;
  }

  // util classes //
  public Machine getMachine(int machine_id) {
    return machines.get(machine_id);
  }

  public Collection<Machine> getMachines() {
    return machines.values();
  }

  public Resource getClusterMaxResAlloc() {
    Resource maxClusterResAvail = new Resource();
    for (Machine machine : machines.values()) {
      maxClusterResAvail.addWith(machine.maxResAlloc);
    }
    return maxClusterResAvail;
  }

  public Resource getClusterResAvail() {
    Resource clusterResAvail = new Resource();
    for (Machine machine : machines.values()) {
      clusterResAvail.addWith(machine.getTotalResAvail());
    }
    return clusterResAvail;
  }
  
  public Resource getClusterAllocatedRes() {
    Resource clusterAllocatedRes = new Resource();
    for (Machine machine : machines.values()) {
      clusterAllocatedRes.addWith(machine.totalResAlloc);
    }
    return clusterAllocatedRes;
  }
  
  public Resource getClusterResQuotaAvail() {
    Resource clusterResAvail = new Resource();
    for (Machine machine : machines.values()) {
      clusterResAvail.addWith(machine.getTotalResAvail());
    }
    return clusterResAvail;
  }

  public double earliestFinishTime() {
    double earliestFinishTime = Double.MAX_VALUE;
    for (Machine machine : machines.values()) {
      earliestFinishTime = Math.min(earliestFinishTime,
          machine.earliestFinishTime());
    }
    return earliestFinishTime;
  }

  public double earliestStartTime() {
    double earliestStartTime = Double.MAX_VALUE;
    for (Machine machine : machines.values()) {
      earliestStartTime = Math.min(earliestStartTime,
          machine.earliestStartTime());
    }
    return earliestStartTime;
  }
  // end util classes //
}
