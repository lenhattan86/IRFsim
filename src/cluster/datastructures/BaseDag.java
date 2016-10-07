package cluster.datastructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cluster.simulator.Simulator;
import cluster.speedfair.ServiceCurve;
import cluster.utils.Interval;

public abstract class BaseDag implements Cloneable{
	
	private static final boolean DEBUG = true;
	
	protected String queueName = ""; 
	private boolean fullyAllocated = false;
	
	public void setQueueName(String queueName){
		this.queueName = queueName;
	}
	
	public String getQueueName(){
		return this.queueName;
	}

  public int dagId;
  public int arrivalTime; // arrival time from the input
  public int numStages; 
  public int numEdgesBtwStages;
  
  public Map<String, Stage> stages;
	public Map<Integer, String> vertexToStage;

  public Map<Integer, Double> CPlength, BFSOrder;

  public abstract void setCriticalPaths();
  public abstract double totalWorkJob();
  public abstract double getMaxCP();
  public abstract Map<Integer, Double> area();
  
  public abstract double longestCriticalPath(int taskId);

  public abstract void setBFSOrder();

  public abstract Resources rsrcDemands(int task_id); // demand of from a task at a certain time step
  
  public ServiceCurve serviceCurve = new ServiceCurve();
  public Resources receivedService = new Resources();

  public abstract double duration(int task_id); // duration of a task

  public abstract List<Interval> getChildren(int task_id);

  public abstract List<Interval> getParents(int task_id);

  public abstract Set<Integer> allTasks();

  public Resources rsrcQuota; // resource should be allocated
  public Resources rsrcInUse; // real used resources

  public LinkedHashSet<Integer> runnableTasks;
  public LinkedHashSet<Integer> runningTasks;
  public LinkedHashSet<Integer> finishedTasks;

  public LinkedHashSet<Integer> launchedTasksNow;

  public double jobStartTime, jobEndTime; // start-time & end-time of serving a job
  public double jobStartRunningTime; // when the job is allocated resources.
  public double jobExpDur; // real completion time of the job.

  // keep track remaining time from current time given some share
  public double timeToComplete;

  public BaseDag(int id, int... arrival) {
    this.dagId = id;
    this.arrivalTime = (arrival.length > 0) ? arrival[0] : 0;

    rsrcQuota = new Resources();
    rsrcInUse = new Resources();

    runnableTasks = new LinkedHashSet<Integer>();
    runningTasks = new LinkedHashSet<Integer>();
    finishedTasks = new LinkedHashSet<Integer>();

    launchedTasksNow = new LinkedHashSet<Integer>();
    serviceCurve = new ServiceCurve();
  }

  public Resources currResDemand() {
    Resources usedRes = new Resources(0.0);
    for (int taskId : runningTasks) {
      usedRes.addWith(rsrcDemands(taskId));
    }
    return usedRes;
  }
  
  public double getCompletionTime(){
  	return this.jobEndTime - this.jobStartTime;
  }
  
  public Resources getMaxDemand(){ 
  	Resources demand = new Resources(0.0);
    for (int taskId : runnableTasks) {
    	demand.addWith(rsrcDemands(taskId));
    }
    for (int taskId : runningTasks) {
    	demand.addWith(rsrcDemands(taskId));
    }
    return demand;
  }
  
	public boolean isFulllyAllocated() {
	  return this.runnableTasks.size()==0;
  }
	
	public int getCommingTaskId() {
		return this.runnableTasks.iterator().next();
  }

	public Resources getCommingTaskRes() {
		int nextTaskId = this.runnableTasks.iterator().next();
	  return this.rsrcDemands(nextTaskId);
  }
	
	public double minCompletionTime(){
		// for simplicity, assumming that all stages can start at the same time.
		double complTime = -Double.MAX_VALUE;
		for (Map.Entry<String, Stage> entry : this.stages.entrySet()) {
			Stage stage = entry.getValue();
			if (complTime<stage.vDuration)
				complTime = stage.vDuration;
		}
		return complTime;
	}
	
//  public Object BaseDag clone();
}
