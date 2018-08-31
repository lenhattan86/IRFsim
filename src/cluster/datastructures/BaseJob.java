package cluster.datastructures;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cluster.simulator.Simulator;
import cluster.speedfair.ServiceCurve;
import cluster.utils.Interval;

public abstract class BaseJob implements Cloneable {

  private static final boolean DEBUG = true;

  protected String queueName = "";
  
  public int NUM_ITERATIONS = 1;
  public double fairVal = 0; 
  public boolean wasScheduled = false;
  private int curr_iter = 0;
  public boolean isCpu = false;
  public boolean isCompleted = false;
  
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
  
  public int numIterations=1;

  public Map<String, SubGraph> stages;
  public Map<Integer, String> vertexToStage;

  public Map<Integer, Double> CPlength, BFSOrder;

  public abstract void setCriticalPaths();

//  public abstract double totalWorkJob();

  public abstract double getMaxCP();

//  public abstract Map<Integer, Double> area();

  public abstract double longestCriticalPath(int taskId);

  public abstract void setBFSOrder();

  public abstract InterchangableResourceDemand rsrcDemands(int task_id); // demand of from a task at
                                                     // a certain time step
  
  public abstract Resource naiveRsrcDemands(int task_id); // demand of from a task at
  // a certain time step
  
  public abstract InterchangableResourceDemand reportDemands(int taskId);
  
//  public HashMap<Integer, Boolean> isCPUUsages = new HashMap<Integer, Boolean>();
  
  public abstract Resource rsrcUsage(int task_id); // resource usage

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

  public BaseJob(int id, int... arrival) {
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

  public InterchangableResourceDemand getDemand() {
  	//TODO: we need to convert to the sum of demand if the tasks are not identical
    Resource resDemand = new Resource();
    for (int taskId : runnableTasks) {
      return rsrcDemands(taskId);
    }
    for (int taskId : runningTasks) {
    	return rsrcDemands(taskId);
    }
    return null;
  }
  
  public double getPForAlloX() {
  	double res = 0;
  	res = (this.getDemand().cpuCompl/this.getDemand().gpuCompl)*(this.getDemand().cpuCompl - this.getDemand().gpuCompl);
  	return res;
  }
  
  public InterchangableResourceDemand getReportDemand() {
    for (int taskId : runnableTasks) {
    	return reportDemands(taskId);
    }
    for (int taskId : runningTasks) {
    	return reportDemands(taskId);
    }
    return null;
  }
  
  public double getMinProcessingTime(){
  	return this.getReportDemand().cpuCompl < this.getReportDemand().gpuCompl ? this.getReportDemand().cpuCompl
				: this.getReportDemand().gpuCompl;
  }
  

  public double getCompletionTime() {
    if (this.jobStartRunningTime < 0)
      System.err.println(
          "you haven't set the starting time for this job " + this.dagId);
    if (this.jobEndTime < 0)
      System.err.println(
          "completion time of the job is not " + this.dagId);
    
    return this.jobEndTime - this.jobStartTime;
  }
  public double getCompletionTimeFromAllocated() {
    if (this.jobStartRunningTime < 0)
      System.err.println(
          "you haven't set the starting time for this job " + this.dagId);
    return this.jobEndTime - this.jobStartRunningTime;
  }


  public boolean isFulllyAllocated() {
    return this.runnableTasks.size() == 0;
  }

  public int getCommingTaskId() {
    if(this.runnableTasks.isEmpty())
      return -1;
    return this.runnableTasks.iterator().next();
  }

/*  public Resource getCommingTaskRes() {
    int nextTaskId = this.runnableTasks.iterator().next();
    return this.rsrcDemands(nextTaskId);
  }*/

  public double minCompletionTimeSimple() {
    // for simplicity, assumming that all stages can start at the same time.
    double complTime = -Double.MAX_VALUE;
    for (Map.Entry<String, SubGraph> entry : this.stages.entrySet()) {
      SubGraph stage = entry.getValue();
      if (complTime < stage.vDuration)
        complTime = stage.vDuration;
    }
    return complTime;
  }
  
  public double minCompletionTime() {
    double complTime = 0.0;
    for (Map.Entry<String, SubGraph> entry : this.stages.entrySet()) {
      SubGraph stage = entry.getValue();
      double temp = minCompletionTime(stage);
//      System.out.print(stage.name+ " "+ temp +",");
      if (complTime < temp)
        complTime = temp;
    }
    return complTime;
  }
  
  public double getLongestTaskDuration(){
    double maxTaskDur = 0.0;
    for (Map.Entry<String, SubGraph> entry : this.stages.entrySet()) {
      SubGraph stage = entry.getValue();
//      System.out.print(stage.name+ " "+ temp +",");
      if (maxTaskDur < stage.vDuration)
        maxTaskDur = stage.vDuration;
    }
    return maxTaskDur;
  }

  private double minCompletionTime(SubGraph stage) {
//    String stageName = "";
    double localMin = 0;
    for (String parent : stage.parents.keySet()) {
      SubGraph s = stages.get(parent);
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

	public boolean isOnCPU() {
		return isCpu;
	}
	
	public void onStart(Resource capacity){
	// find the owner of the job		
			double p1 = this.getDemand().cpuCompl;
			double p2 = this.getDemand().gpuCompl;
			if (p1 < p2){
				if (this.isOnCPU()) {
					this.fairVal = Math.max(this.getDemand().cpu/capacity.resource(0), this.getDemand().mem/capacity.resource(2));
				} else {
					this.fairVal = p1/p2* Math.max(this.getDemand().cpu/capacity.resource(0), this.getDemand().mem/capacity.resource(2));
				}
			} else {
				if (this.isOnCPU()) {
					this.fairVal = p2/p1* Math.max(this.getDemand().gpu/capacity.resource(1), this.getDemand().gpuMem/capacity.resource(2));
				} else {
					this.fairVal = Math.max(this.getDemand().gpu/capacity.resource(1), this.getDemand().gpuMem/capacity.resource(2));
				}
			}
			this.getQueue().L += this.fairVal;
			wasScheduled = true;
	}
	
	public void onFinish(){
		this.getQueue().L -= this.fairVal;
		this.isCompleted = true;
  }
}
