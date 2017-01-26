package cluster.datastructures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.Method;
import cluster.simulator.Simulator;
import cluster.speedfair.ServiceRate;
import cluster.utils.Output;

public class JobQueue {

  private static boolean DEBUG = false;

  // private Queue<BaseDag> runnableJobs; // Jobs are in the queue
  private Queue<BaseDag> runningJobs;
  public Queue<BaseDag> completedJobs;

  public Session session = null;

  public boolean isLQ = false;

  private double weight = 1.0;

  /*
   * public double getStartTime() { return startTime; }
   * 
   * public void setStartTime(double startTime) { this.startTime = startTime; }
   */
  // private double startTime = 0.0;

  private double speedFairWeight = 1.0;

  private double startTimeOfNewJob = -1.0;

  // private ServiceRate serviceRate = new ServiceRate();

  public List<Resource> receivedResourcesList = new LinkedList<Resource>();

  private Resource rsrcQuota = new Resource();

  String queueName = "";

  public JobQueue(String queueName) {
    this.queueName = queueName;
    // runnableJobs = new LinkedList<BaseDag>();
    runningJobs = new LinkedList<BaseDag>();
    completedJobs = new LinkedList<BaseDag>();
  }

  public JobQueue(String queueName, Session session) {
    this.queueName = queueName;
    // runnableJobs = new LinkedList<BaseDag>();
    runningJobs = new LinkedList<BaseDag>();
    completedJobs = new LinkedList<BaseDag>();
    this.session = session;
  }

  public String getQueueName() {
    return this.queueName;
  }

  public void updateGuartRate() {
    // resLongTermGuartRate =
    // Resources.divide(Simulator.cluster.getClusterMaxResAlloc(),
    // Simulator.QUEUE_LIST.getRunningQueues().size());
    // Output.debugln(DEBUG, "resLongTermGuartRate:" + resLongTermGuartRate);
  }

  public double avgCompletionTime() {
    if (completedJobs.size() <= 0) {
      return -1.0;
    }

    double avgTime = 0.0;
    for (BaseDag job : completedJobs)
      avgTime += job.getCompletionTime();
    avgTime = avgTime / completedJobs.size();

    return avgTime;
  }

  /*
   * public void admitJobs(BaseDag newArrivalJob) {
   * runnableJobs.add(newArrivalJob); }
   */

  public void setRsrcQuota(Resource rsrcQuota) {
    this.rsrcQuota = new Resource(rsrcQuota);
  }

  public Resource getRsrcQuota() {
    return new Resource(rsrcQuota);
  }

  public Resource getJobsQuota() {
    Resource res = new Resource();
    for (BaseDag job : runningJobs) {
      res.addWith(job.rsrcQuota);
    }
    return res;
  }

  public Resource getResRate(List<Resource> resList, double term) {
    int timeSteps = (int) Math.round(term / Globals.STEP_TIME);
    Resource res = new Resource();
    Iterator<Resource> iRes = resList.iterator();
    int i = 0;
    while (iRes.hasNext()) {
      if (i++ > timeSteps)
        break;
      res = Resources.sum(res, iRes.next());
    }
    return Resources.divide(res, timeSteps);
  }

  public Resource getReceivedRes(double term) {
    Resource res = new Resource();
    Iterator<Resource> iRes = this.receivedResourcesList.iterator();
    int timeSteps = (int) Math.round(term / Globals.STEP_TIME);
    int i = 0;
    while (iRes.hasNext() && i++ < timeSteps) {
      res.addWith(iRes.next());
    }
    return res;
  }

  public Resource getResRate(double term) {
    return this.getResRate(this.receivedResourcesList, term);
  }

  public void addResourcesList(Resource res) {
    this.receivedResourcesList.add(0, res);
  }

  public Resource computeShare(double term, Resource guartRate) {
    Resource resQuota = new Resource();
    Resource received = this.getReceivedRes(term - Globals.STEP_TIME);
    Resource total = Resources.multiply(guartRate,
        (int) (Math.round(term / Globals.STEP_TIME)));
    resQuota = Resources.subtractPositivie(total, received);
    Resource resDemand = new Resource();
    for (BaseDag job : runningJobs) {
      resDemand = Resources.sum(resDemand, job.getMaxDemand());
    }
    return Resources.piecewiseMin(resQuota, resDemand);
  }

  public Resource getMaxDemand() {
    Resource resDemand = new Resource();
    for (BaseDag job : runningJobs) {
      resDemand = Resources.sum(resDemand, job.getMaxDemand());
    }
    return resDemand;
  }

  public Resource getResourceUsage() {
    Resource res = new Resource();
    for (BaseDag job : this.runningJobs) {
      res.addWith(job.getRsrcInUse());
    }
    return res;
  }

  public String getResourceUsageStr() {
    String str = this.queueName;
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++)
      str += "," + this.getResourceUsage().resource(i);
    return str;
  }

  // getters & setters
  public double getWeight() {
    double res = weight;
    if (isLQ && (Globals.METHOD.equals(Method.Strict) || Globals.METHOD.equals(Method.Strict_Reject)))
      res = Globals.STRICT_WEIGHT;
    else if (isLQ && Globals.METHOD.equals(Method.DRFW))
      res = Globals.DRFW_weight;
    // else if(isInteractive && Globals.METHOD.equals(Method.SpeedFair))
    // res = this.getSpeedFairWeight();
    return res;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public void setSpeedFairWeight(double weight) {
    this.speedFairWeight = weight;
  }

  /*
   * public double getSpeedFairWeight() { if
   * (this.serviceRate.isBeyondGuaranteedDuration(Simulator.CURRENT_TIME,
   * this.startTimeOfNewJob)) //add 1 condition for batch queues return
   * this.speedFairWeight; else return 1.0; // make equal share to others. }
   */

  public Resource getGuaranteeRate(double currTime) {
    Resource zero = new Resource();
    if (isInStage1(currTime))
      return session.getAlpha(currTime);
    else
      return zero;
  }

  public Session getSession() {
    return this.session;
  }

  /*
   * public Resource getAlpha(double currTime){ for (Session s:
   * sessions.toList()){ double period = 0.0; if (s.isPeriodic()){ period =
   * s.getPeriod(); }else { period = s.getAlphaDuration(); } double endTime =
   * s.getStartTime()+s.getNumOfJobs()*period; //TODO: getGuaranteeRate may NOT
   * be correct. if(currTime>=s.getStartTime()&& currTime< endTime) return
   * s.getAlpha(); } return new Resource(Resources.ZEROS); }
   */

  /*
   * public double getStage1Duration(double currTime){ for (Session s:
   * sessions.toList()){ double period = 0.0; if (s.isPeriodic()){ period =
   * s.getPeriod(); }else { period = s.getAlphaDuration(); } double endTime =
   * s.getStartTime()+s.getNumOfJobs()*period;
   * 
   * if(currTime>=s.getStartTime()&& currTime<=endTime) return
   * s.getAlphaDuration(); } return 0.0; }
   */

  /*
   * public void addRunnableJob(BaseDag newJob) { this.runnableJobs.add(newJob);
   * }
   */

  public void removeRunningJob(BaseDag newJob) {
    this.runningJobs.remove(newJob);
  }

  public void addCompletedJob(BaseDag newJob) {
    this.completedJobs.add(newJob);
  }

  public boolean isActive() {
//    if (!isLQ && this.runningJobs.size() > 0)
    if(!isLQ)
      return true;

    if (!Globals.METHOD.equals(Globals.Method.SpeedFair)
        && this.runningJobs.size() > 0)
      return true;

    if (isLQ && Globals.METHOD.equals(Globals.Method.SpeedFair)) {
      return this.getStartTime() <= Simulator.CURRENT_TIME
          && this.session.getEndTime() > Simulator.CURRENT_TIME;
    }

    return false;
  }

  public boolean hasRunningJobs() {
    return this.runningJobs.size() > 0;
  }

  public boolean isActive(double currTime) {
    if (!isLQ && this.runningJobs.size() > 0)
      return true;

    if (isLQ && Globals.METHOD.equals(Globals.Method.SpeedFair)) {
      // return (session!=null) && isInStage1(currTime);
      return this.getStartTime() <= currTime;
    }
    return false;
  }

  public Queue<BaseDag> getRunningJobs() {
    return this.runningJobs;
  }

  public int runningJobsSize() {
    return this.runningJobs.size();
  }

  public boolean isInStage1(double currTime) {
    double startTime = session.getStartPeriodTime(currTime);

    double virtualCurrTime = currTime - startTime;
    if (virtualCurrTime < session.getAlphaDuration(currTime))
      return true;
    else
      return false;

  }

  public Resource getInStage1Alpha(double currTime) {
    Resource res = new Resource(Resources.ZEROS);
    if (isInStage1(currTime))
      return session.getAlpha(currTime);
    return res;
  }

  public void addRunningJob(BaseDag newJob) {
    this.startTimeOfNewJob = Simulator.CURRENT_TIME;
    Output.debugln(DEBUG, this.queueName + " at " + this.startTimeOfNewJob);
    this.runningJobs.add(newJob);
  }

  /*
   * public void removeRunnableJob(BaseDag oldJob) {
   * this.runnableJobs.remove(oldJob); }
   */
  public double getStartTimeOfNewJob() {
    return startTimeOfNewJob;
  }

  public void setStartTimeOfNewJob(double startTimeOfNewJob) {
    this.startTimeOfNewJob = startTimeOfNewJob;
  }

  public Queue<BaseDag> cloneRunningJobs() {
    Queue<BaseDag> jobs = new LinkedList<BaseDag>();
    for (BaseDag job : this.runningJobs)
      jobs.add(job);
    return jobs;
  }

  public Resource nextTaskRes() {
    Resource res = new Resource();

    return res;
  }

  public BaseDag getUnallocRunningJob() {
    for (BaseDag job : this.runningJobs)
      if (!job.isFulllyAllocated()) {
        return job;
      }
    return null;
  }

  public Resource assign(Resource assignedRes) {
    Resource remain = Resources.clone(assignedRes);
    while (true) {
      BaseDag unallocJob = this.getUnallocRunningJob();
      if (unallocJob == null) {
        return remain;
      }
      // TODO: a job may have a variety of tasks having different resource
      // demands.
      int taskId = unallocJob.getCommingTaskId();
      Resource allocRes = unallocJob.rsrcDemands(taskId);
      if (remain.greaterOrEqual(allocRes)) {
        boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId,
            taskId, unallocJob.duration(taskId), allocRes);
        if (assigned) {
          remain = Resources.subtract(remain, allocRes);

          if (unallocJob.jobStartRunningTime < 0) {
            unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
          }
          // update userDominantShareArr
        } else {
          Output.debugln(DEBUG,
              "[DRFScheduler] Cannot assign resource to the task" + taskId
                  + " of Job " + unallocJob.dagId + " " + allocRes);
          break; // TODO: Add the further process to utilize more resource.
        }
      } else {
        // do not allocate to this queue any more
        break;
      }
    }
    return remain;
  }

  public double getStartTime() {
    if (session != null)
      return session.getStartTime();
    return 0.0;
  }
}
