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

public abstract class BaseDag implements Cloneable {

  private static final boolean DEBUG = true;

  protected String queueName = "";
  private boolean fullyAllocated = false;
  
  public ArrayList<Resource> usedReses = new ArrayList<Resource>();

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public String getQueueName() {
    return this.queueName;
  }
  
  public int sessionId = -1;

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

  public abstract Resource rsrcDemands(int task_id); // demand of from a task at
                                                     // a certain time step

  public ServiceCurve serviceCurve = new ServiceCurve();
  
  public Resource receivedService = new Resource();

  public abstract double duration(int task_id); // duration of a task

  public abstract List<Interval> getChildren(int task_id);

  public abstract List<Interval> getParents(int task_id);

  public abstract Set<Integer> allTasks();

  public Resource rsrcQuota; // resource should be allocated
  private Resource rsrcInUse; // real used resources

  public Resource getRsrcInUse() {
    return rsrcInUse;
  }

  public void setRsrcInUse(Resource rsrcInUse) {
    this.rsrcInUse = rsrcInUse;
  }

  public LinkedHashSet<Integer> runnableTasks;
  public LinkedHashSet<Integer> runningTasks;
  public LinkedHashSet<Integer> finishedTasks;

  public LinkedHashSet<Integer> launchedTasksNow;

  public double jobStartTime, jobEndTime; // start-time & end-time of serving a
                                          // job
  public double jobStartRunningTime = -1.0; // when the job is allocated
                                            // resources.
  public double jobExpDur; // real completion time of the job.

  // keep track remaining time from current time given some share
  public double timeToComplete;

  public BaseDag(int id, int... arrival) {
    this.dagId = id;
    this.arrivalTime = (arrival.length > 0) ? arrival[0] : 0;

    rsrcQuota = new Resource();
    rsrcInUse = new Resource();

    runnableTasks = new LinkedHashSet<Integer>();
    runningTasks = new LinkedHashSet<Integer>();
    finishedTasks = new LinkedHashSet<Integer>();

    launchedTasksNow = new LinkedHashSet<Integer>();
    serviceCurve = new ServiceCurve();
  }

  public Resource currResDemand() {
    Resource usedRes = new Resource(0.0);
    for (int taskId : runningTasks) {
      usedRes.addWith(rsrcDemands(taskId));
    }
    return usedRes;
  }

  public double getCompletionTime() {
    if (this.jobStartRunningTime < 0)
      System.err.println(
          "you haven't set the starting time for this job " + this.dagId);
    return this.jobEndTime - this.jobStartRunningTime;
  }

  public Resource getMaxDemand() {
    Resource demand = new Resource(0.0);
    for (int taskId : runnableTasks) {
      demand.addWith(rsrcDemands(taskId));
    }
    for (int taskId : runningTasks) {
      demand.addWith(rsrcDemands(taskId));
    }
    return demand;
  }

  public boolean isFulllyAllocated() {
    return this.runnableTasks.size() == 0;
  }

  public int getCommingTaskId() {
    return this.runnableTasks.iterator().next();
  }

  public Resource getCommingTaskRes() {
    int nextTaskId = this.runnableTasks.iterator().next();
    return this.rsrcDemands(nextTaskId);
  }

  public double minCompletionTimeSimple() {
    // for simplicity, assumming that all stages can start at the same time.
    double complTime = -Double.MAX_VALUE;
    for (Map.Entry<String, Stage> entry : this.stages.entrySet()) {
      Stage stage = entry.getValue();
      if (complTime < stage.vDuration)
        complTime = stage.vDuration;
    }
    return complTime;
  }
  
  public double minCompletionTime() {
    double complTime = 0.0;
    for (Map.Entry<String, Stage> entry : this.stages.entrySet()) {
      Stage stage = entry.getValue();
      double temp = minCompletionTime(stage);
//      System.out.print(stage.name+ " "+ temp +",");
      if (complTime < temp)
        complTime = temp;
    }
    return complTime;
  }
  
  public double getLongestTaskDuration(){
    double maxTaskDur = 0.0;
    for (Map.Entry<String, Stage> entry : this.stages.entrySet()) {
      Stage stage = entry.getValue();
//      System.out.print(stage.name+ " "+ temp +",");
      if (maxTaskDur < stage.vDuration)
        maxTaskDur = stage.vDuration;
    }
    return maxTaskDur;
  }

  private double minCompletionTime(Stage stage) {
//    String stageName = "";
    double localMin = 0;
    for (String parent : stage.parents.keySet()) {
      Stage s = stages.get(parent);
      if (s != null) {
        double temp = minCompletionTime(s);
        if (localMin < temp){
          localMin = temp;
//          stageName = parent;
        }
      }
    }
//    String extra = ",";
//    if (stage.parents.keySet().size()==0)
//      extra = ";\n";
//    System.out.print(stageName+ " "+ localMin + extra);
    return localMin  + stage.vDuration;
  }

  public JobQueue getQueue() {
    return Simulator.QUEUE_LIST.getJobQueue(queueName);
  }

  // public Object BaseDag clone();
}
