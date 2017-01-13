package queue.schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.datastructures.Session;
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;

public class SpeedFairSchedulerHardGuarantee implements Scheduler {
  private boolean DEBUG = false;
  private boolean SCHEDULING_OVERHEADS = false;

  private Queue<JobQueue> admittedBurstyQueues = null;
  private Queue<JobQueue> admittedBatchQueues = null;
  private Queue<JobQueue> bestEffortQueues = null;

  private String schedulePolicy;

  Resource clusterTotCapacity = null;

  public SpeedFairSchedulerHardGuarantee() {
    clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
    schedulePolicy = "SpeedFair";
    admittedBurstyQueues = new LinkedList<JobQueue>();
    admittedBatchQueues = new LinkedList<JobQueue>();
    bestEffortQueues = new LinkedList<JobQueue>();
  }

  @Override
  public void computeResShare() {
    periodicSchedule();
  }

  private void periodicSchedule() {
    // if(Simulator.CURRENT_TIME>=Globals.DEBUG_START &&
    // Simulator.CURRENT_TIME<=Globals.DEBUG_END){
    // DEBUG = false;
    // }else
    // DEBUG = false;
    // update queue status
    Output.debugln(DEBUG,
        "\n==== STEP_TIME:" + Simulator.CURRENT_TIME + " ====");

    updateQueueStatus();
    // add queues to the best effort queues
    updateBestEfforQueue();
    // obtain the available resource
    // Resource avaiRes = Simulator.cluster.getClusterResAvail();
    // admission control
    admit();
    // allocate resources to bursty and batch queues
    allocate();
    // allocate spare resousrces.
    allocateSpareResources();

    // update resources
    for (JobQueue q : Simulator.QUEUE_LIST.getJobQueues()) {
      q.addResourcesList(q.getResourceUsage());
    }
  }

  private void allocateSpareResources() {
    // allocate the spare resource
    Resource remainingResources = Resources
        .clone(Simulator.cluster.getClusterResAvail());
    DRFScheduler.onlineDRFShare(remainingResources, (List) bestEffortQueues);
  }

  private void allocate() {
    Resource avaiRes = Simulator.cluster.getClusterResAvail();

    for (JobQueue q : admittedBurstyQueues) {
      // compute the rsrcQuota based on the guarateed rate.
      // Resources a = q.getGuaranteeRate(Simulator.CURRENT_TIME);
      Resource a = getBurstyGuarantee(q);
      Resource moreRes = Resources.subtractPositivie(a, q.getResourceUsage());
      moreRes = Resources.piecewiseMin(moreRes, avaiRes);
      Resource remain = q.assign(moreRes);
      // assign the task
      Resource rsrcQuota = null;
      rsrcQuota = Resources.subtract(a, remain);
      moreRes = Resources.subtract(moreRes, remain);
      q.setRsrcQuota(rsrcQuota);
      avaiRes = Resources.subtractPositivie(avaiRes, moreRes);
      Output.debugln(DEBUG, "[SpeedFairScheduler] [allocate] "
          + q.getQueueName() + ": " + rsrcQuota);
    }
    // avaiRes = Simulator.cluster.getClusterResAvail();
    // use DRF for the admitted batch queues
    Resource remainingResources = Resources.clone(avaiRes);
    if (remainingResources.distinct(Resources.ZEROS))
      DRFScheduler.onlineDRFShare(remainingResources,
          (List) admittedBatchQueues);
  }

  private Resource getBurstyGuarantee(JobQueue q) {
    int numAdmittedQueues = admittedBatchQueues.size()
        + admittedBurstyQueues.size();
    Resource res = new Resource();
    Session s = q.getCurrSession(Simulator.CURRENT_TIME);
    if (s==null)
      System.err.println(q.getQueueName());
    Resource alpha = s.getAlpha();
    Resource guaranteedRes = Resources.multiply(alpha,
        s.getAlphaDuration());
    // TODO: fix q.getStartTime to getSessionStartTime;
    double lasting = (Simulator.CURRENT_TIME - q.getCurrSessionStartTime())
        % s.getPeriod(numAdmittedQueues);
    boolean inStage1 = lasting <= s.getAlphaDuration();
    Resource receivedRes = q.getReceivedRes(lasting);
    boolean isGuaranteed = receivedRes.greaterOrEqual(guaranteedRes);
    if (inStage1 || !isGuaranteed)
      res = alpha;
    else {
      Resource nom = Resources.multiply(clusterTotCapacity,
          s.getPeriod(numAdmittedQueues)
              / (admittedBatchQueues.size() + admittedBurstyQueues.size()));
      nom = Resources.subtractPositivie(nom, receivedRes);
      Resource beta = Resources.divide(nom,
          (s.getPeriod(numAdmittedQueues)
              - s.getAlphaDuration()));
      res = beta;
    }
    return res;
  }

  /*
   * private Resource getBurstyGuaranteePreemption(JobQueue q){ Resource res =
   * new Resource(); Resource alpha = q.getAlpha(Simulator.CURRENT_TIME);
   * Resource nom = Resources.multiply(clusterTotCapacity,
   * q.getPeriod()/(admittedBatchQueues.size()+admittedBurstyQueues.size()));
   * nom = Resources.subtract(nom, Resources.multiply(alpha,
   * q.getStage1Duration(Simulator.CURRENT_TIME))); Resource beta =
   * Resources.divide(nom,
   * (q.getPeriod()-q.getStage1Duration(Simulator.CURRENT_TIME))); if
   * (Simulator.CURRENT_TIME % Globals.PERIODIC_INTERVAL <=
   * q.getStage1Duration(Simulator.CURRENT_TIME)) res = alpha; else res = beta;
   * return res; }
   */

  private boolean resGuarateeCond(JobQueue newQueue) {
    Session currSession = newQueue.getCurrSession(Simulator.CURRENT_TIME);
    if(currSession ==null)
      System.err.println(newQueue.getQueueName());
    int numAdmittedQueues = admittedBatchQueues.size()
        + admittedBurstyQueues.size();
    for (int j = 0; j < currSession.getNumOfJobs(); j++) {
      double currTime = Simulator.CURRENT_TIME
          + j * currSession.getPeriod(numAdmittedQueues);
      Resource alpha = currSession.getAlpha();
      for (double t = currTime; t < currTime
          + currSession.getAlphaDuration(); t += Globals.STEP_TIME) {
        Resource burstyRes = new Resource(Resources.ZEROS);
        for (JobQueue q : admittedBurstyQueues) {
          burstyRes.addWith(q.getGuaranteeRate(t));
        }
        boolean result = alpha.smallerOrEqual(
            Resources.subtractPositivie(clusterTotCapacity, burstyRes));

        if (!result)
          return result;
      }
    }
    return true;
  }

  private boolean resFairnessCond(JobQueue newQueue) {
    
    Session currSession = newQueue.getCurrSession(Simulator.CURRENT_TIME);
    int numAdmittedQueues = admittedBatchQueues.size()
        + admittedBurstyQueues.size();
    //TODO: do not need the loop.
    for (int j = 0; j < currSession.getNumOfJobs(); j++) {
      Resource alpha = currSession.getAlpha();
      Resource lhs = Resources.multiply(alpha,
          currSession.getAlphaDuration());
      Resource rhs = Resources.multiply(clusterTotCapacity,
          currSession.getPeriod(numAdmittedQueues));
      double denom = Math.max(
          admittedBurstyQueues.size() + admittedBatchQueues.size() + 1,
          Double.MIN_VALUE);
      rhs.divide(denom);

      if (!lhs.smallerOrEqual(rhs))
        return false;
    }
    return true;
  }

  private void admit() {
    long tStart = System.currentTimeMillis();
    Queue<JobQueue> newAdmittedQueues = new LinkedList<JobQueue>();
    for (JobQueue q : bestEffortQueues) {
      if (q.isLQ) {
        boolean condition1 = resGuarateeCond(q);
        boolean condition2 = resFairnessCond(q);
        Session currSession = q.getCurrSession(Simulator.CURRENT_TIME);
        int sId = q.getCurrSessionIdx(Simulator.CURRENT_TIME);
        if (condition1 && condition2) {
          admittedBurstyQueues.add(q);
          newAdmittedQueues.add(q);
          Output.debugln(DEBUG,
              "[SpeedFairScheduler] admitted session "
                  + sId + " of "
                  + q.getQueueName());
        } else {
          Output.debugln(DEBUG,
              "[SpeedFairScheduler] cannot admit session "
                  + sId + " of "
                  + q.getQueueName());
          
          // remove all jobs from the session 
          Queue<BaseDag> removedJobs = new LinkedList<BaseDag>();
          for(BaseDag job: Simulator.runningJobs){
            if(job.getQueue().equals(q) && job.sessionId == sId)
              removedJobs.add(job);
          }
          Simulator.runningJobs.removeAll(removedJobs);
          q.getRunningJobs().removeAll(removedJobs);
          
          removedJobs = new LinkedList<BaseDag>();
          for(BaseDag job: Simulator.runnableJobs){
            if(job.getQueue().equals(q)&& job.sessionId == sId)
              removedJobs.add(job);
          }
          Simulator.runnableJobs.removeAll(removedJobs);
          q.getRunningJobs().removeAll(removedJobs);
          
          currSession.reject();
        }
      } else {

        if (condFairness4Batch()) {
          admittedBatchQueues.add(q);
          newAdmittedQueues.add(q);
          Output.debugln(DEBUG,
              "[SpeedFairScheduler] admitted " + q.getQueueName());
        } else
          Output.debugln(DEBUG,
              "[SpeedFairScheduler] cannot admit " + q.getQueueName());
      }
    }
    bestEffortQueues.removeAll(newAdmittedQueues);
    long overheads = System.currentTimeMillis() - tStart;

    if (SCHEDULING_OVERHEADS)
      System.out.println(
          "Admit takes: " + overheads + " ms at " + Simulator.CURRENT_TIME);
  }

  private boolean condFairness4Batch() {
    int numAdmittedQueues = admittedBatchQueues.size()
        + admittedBurstyQueues.size();
    boolean condition = true;
    for (JobQueue A : admittedBurstyQueues) {
      Session s = A.getCurrSession(Simulator.CURRENT_TIME);
      Resource alpha = s.getAlpha();
      Resource lhs = Resources.multiply(alpha,
          s.getAlphaDuration());
      Resource rhs = Resources.multiply(clusterTotCapacity, A
          .getCurrSession(Simulator.CURRENT_TIME).getPeriod(numAdmittedQueues));
      double denom = Math.max(
          admittedBurstyQueues.size() + admittedBatchQueues.size() + 1,
          Double.MIN_VALUE);
      rhs.divide(denom);
      condition = lhs.smallerOrEqual(rhs);
      if (!condition)
        break;
    }

    return condition;
  }

  private void updateBestEfforQueue() {
    // for (JobQueue q: Simulator.QUEUE_LIST.getJobQueues()){
    for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
//      Output.debugln(true,
//          "[SpeedFairScheduler] updateBestEfforQueue " + q.getQueueName());
      if (admittedBatchQueues.contains(q) || admittedBurstyQueues.contains(q)
          || bestEffortQueues.contains(q) || !q.isActive()) {
      } else {
//        Output.debugln(true, "[SpeedFairScheduler] updateBestEfforQueue adds "
//            + q.getQueueName() + " to best effort queues");
        bestEffortQueues.add(q);
      }
    }
  }

  private void updateQueueStatus() {
    Queue<JobQueue> temp = new LinkedList<JobQueue>();
    for (JobQueue q : admittedBurstyQueues) {
      if (!q.isActive())
        temp.add(q);
    }
    admittedBurstyQueues.removeAll(temp);
    temp.clear();
    for (JobQueue q : admittedBatchQueues) {
      if (!q.isActive())
        temp.add(q);
    }
    admittedBatchQueues.removeAll(temp);
    temp.clear();
    for (JobQueue q : bestEffortQueues) {
      if (!q.isActive())
        temp.add(q);
    }
    bestEffortQueues.removeAll(temp);
  }

  public void fifoShareForJobs(JobQueue q, Resource availRes) {
    boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
    if (!fit) {
      Resource newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
      q.setRsrcQuota(newQuota);
    }
    q.receivedResourcesList.add(q.getRsrcQuota());

    Resource remain = q.getRsrcQuota();
    List<BaseDag> runningJobs = new LinkedList<BaseDag>(q.getRunningJobs());
    Collections.sort(runningJobs, new JobArrivalComparator());
    for (BaseDag job : runningJobs) {
      Resource rsShare = Resources.piecewiseMin(remain, job.getMaxDemand());
      job.rsrcQuota = rsShare;
      remain.subtract(rsShare);
      Output.debugln(DEBUG, "[SpeedFairScheduler] Allocated to job:" + job.dagId
          + " @ " + job.getQueueName() + " " + job.rsrcQuota);
    }
    availRes = Resources.subtract(availRes, q.getRsrcQuota());
  }

  @Override
  public String getSchedulePolicy() {
    return schedulePolicy;
  }
}
