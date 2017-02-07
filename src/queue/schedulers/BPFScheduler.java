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
import cluster.simulator.Main.Globals.Method;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;
import jdk.nashorn.internal.ir.CatchNode;

public class BPFScheduler implements Scheduler {
  private boolean DEBUG = false;
  private boolean SCHEDULING_OVERHEADS = false;

  private Queue<JobQueue> hardGuaranteeQueues = null;
  // private Queue<JobQueue> admittedBatchQueues = null;
  private Queue<JobQueue> softGuaranteeQueues = null;
  private Queue<JobQueue> elasticQueues = null;
  private Queue<JobQueue> rejectedQueues = null;
  
  boolean enableSoftGuarantee = true;

  private String schedulePolicy;

  Resource clusterTotCapacity = null;

  public BPFScheduler() {
    clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
    schedulePolicy = "SpeedFair";
    hardGuaranteeQueues = new LinkedList<JobQueue>();
    // admittedBatchQueues = new LinkedList<JobQueue>();
    softGuaranteeQueues = new LinkedList<JobQueue>();
    elasticQueues = new LinkedList<JobQueue>();
    rejectedQueues = new LinkedList<JobQueue>();
    
    if(Globals.METHOD.equals(Method.N_BPF))
      enableSoftGuarantee = false;
    else
      enableSoftGuarantee = true;
  }

  @Override
  public void computeResShare() {
    periodicSchedule();
  }

  private void periodicSchedule() {
    if (Simulator.CURRENT_TIME >= Globals.DEBUG_START && Simulator.CURRENT_TIME <= Globals.DEBUG_END) {
      DEBUG = true;
    } else
      DEBUG = false;

    Output.debugln(DEBUG, "\n==== STEP_TIME:" + Simulator.CURRENT_TIME + " ====");

    Queue<JobQueue> newQueues = updateQueueStatus();

    admit(newQueues);

    allocate();

    // update resources
    for (JobQueue q : Simulator.QUEUE_LIST.getJobQueues()) {
      q.addResourcesList(q.getResourceUsage());
    }
  }

  private void allocateSpareResources() {
    Resource remainingResources = Resources.clone(Simulator.cluster.getClusterResAvail());
    DRFScheduler.onlineDRFShare(remainingResources, (List) softGuaranteeQueues);
  }

  private void allocate() {
    /*
     * if (Simulator.CURRENT_TIME == 226) DEBUG = true;
     */

    Resource avaiRes = Simulator.cluster.getClusterResAvail();

    Resource maxResource = Simulator.cluster.getClusterMaxResAlloc();
    boolean enableCompensation = true;
    // hard guarantee
    for (JobQueue q : hardGuaranteeQueues) {
      Resource gRes = getBurstyGuarantee(q, enableCompensation);

      Resource moreRes = Resources.subtractPositivie(gRes, q.getResourceUsage());
      moreRes = Resources.piecewiseMin(moreRes, avaiRes);
      Resource remain = q.assign(moreRes);
      // assign the task
      Resource rsrcQuota = null;
      rsrcQuota = Resources.subtract(gRes, remain);
      moreRes = Resources.subtract(moreRes, remain);
      q.setRsrcQuota(rsrcQuota);
      avaiRes = Resources.subtractPositivie(avaiRes, moreRes);
      Output.debugln(DEBUG, "[BPFScheduler] [allocate] " + q.getQueueName() + ": " + rsrcQuota);

      // gRes = Resources.piecewiseMin(gRes, moreRes);
      /*
       * maxResource = Resources.subtractPositivie(maxResource,
       * q.getInStage1Alpha(Simulator.CURRENT_TIME));
       */
    }
    // soft guarantee

    for (JobQueue q : softGuaranteeQueues) { // q just be TQ
      if (!q.isLQ) {
        System.err.println("queue " + q.getQueueName() + " is not TQ.");
      }

      Resource gRes = getBurstyGuarantee(q, true);

      Resource moreRes = Resources.subtractPositivie(gRes, q.getResourceUsage());
      moreRes = Resources.piecewiseMin(moreRes, avaiRes);
      Resource remain = q.assign(moreRes);
      // assign the task
      Resource rsrcQuota = null;
      rsrcQuota = Resources.subtract(gRes, remain);
      moreRes = Resources.subtract(moreRes, remain);
      q.setRsrcQuota(rsrcQuota);
      avaiRes = Resources.subtractPositivie(avaiRes, moreRes);
      Output.debugln(DEBUG, "[BPFScheduler] [allocate] " + q.getQueueName() + ": " + rsrcQuota);

      // gRes = Resources.piecewiseMin(gRes, moreRes);

      /*
       * else { elasticQueues.add(q); }
       */
    }

    // spare resouce allocation
    // elasticQueues.addAll(admittedBatchQueues);
    Resource remainingResources = Resources.clone(avaiRes);
    if (remainingResources.distinct(Resources.ZEROS))
      DRFScheduler.onlineDRFShare(remainingResources, (List) elasticQueues);
  }

  private Resource getBurstyGuarantee(JobQueue q, boolean enableCompensation) {
    int numQueues = elasticQueues.size() + hardGuaranteeQueues.size() + softGuaranteeQueues.size();
    Resource res = new Resource();
    Session s = q.session;
    if (s == null)
      System.err.println(q.getQueueName());

    Resource alpha = s.getAlpha(Simulator.CURRENT_TIME);

    if (alpha.fitsIn(Resources.ZEROS))
      return new Resource(0.0);

    Resource guaranteedRes = Resources.multiply(alpha, s.getAlphaDuration(Simulator.CURRENT_TIME));
    double lasting = (Simulator.CURRENT_TIME - q.session.getStartPeriodTime(Simulator.CURRENT_TIME));
    boolean inStage1 = lasting <= s.getAlphaDuration(Simulator.CURRENT_TIME);
    Resource receivedRes = q.getReceivedRes(lasting);
    boolean isGuaranteed = receivedRes.greaterOrEqual(guaranteedRes);
    if (inStage1 || (!isGuaranteed && enableCompensation))
      // if (inStage1)
      res = alpha;
    else {
      Resource nom = Resources.multiply(clusterTotCapacity, s.getPeriod(Simulator.CURRENT_TIME) / (numQueues));
      nom = Resources.subtractPositivie(nom, receivedRes);
      Resource beta = Resources.divide(nom,
          (s.getPeriod(Simulator.CURRENT_TIME) - s.getAlphaDuration(Simulator.CURRENT_TIME)));
      res = beta;
    }
    return res;
  }

  private boolean resourceCond(JobQueue newQueue) {
    Session s = newQueue.session;
    if (s == null)
      System.err.println(newQueue.getQueueName());

    for (int j = 0; j < s.getNumOfJobs(); j++) {
      double currTime = s.getStartTime() + j * s.getPeriod(Simulator.CURRENT_TIME);
      Resource alpha = s.getAlpha(Simulator.CURRENT_TIME);
      for (double t = currTime; t < currTime + s.getAlphaDuration(Simulator.CURRENT_TIME); t += Globals.STEP_TIME) {
        Resource burstyRes = new Resource(Resources.ZEROS);
        for (JobQueue q : hardGuaranteeQueues) {
          try {
            burstyRes.addWith(q.getGuaranteeRate(t));
          } catch (Exception e) {
            burstyRes.addWith(q.getGuaranteeRate(t));
          }
        }
        boolean result = alpha.smallerOrEqual(Resources.subtractPositivie(clusterTotCapacity, burstyRes));

        if (!result)
          return result;
      }
    }
    return true;
  }


  private boolean fairnessCond(JobQueue newQueue) {
    Session currSession = newQueue.session;
    int numQueues = 0;

    for (JobQueue q : elasticQueues) {
      if (!q.equals(newQueue))
        numQueues++;
    }

    numQueues += hardGuaranteeQueues.size();
    numQueues += softGuaranteeQueues.size();

    // TODO: do not need the loop.
    for (int j = 0; j < currSession.getNumOfJobs(); j++) {
      Resource alpha = currSession.getAlpha(Simulator.CURRENT_TIME);
      Resource lhs = Resources.multiply(alpha, currSession.getAlphaDuration(Simulator.CURRENT_TIME));
      Resource rhs = Resources.multiply(clusterTotCapacity, currSession.getPeriod(Simulator.CURRENT_TIME));
      double denom = numQueues + 1;
      rhs.divide(denom);

      if (!lhs.smallerOrEqual(rhs))
        return false;
    }
    return true;
  }

  private void admit(Queue<JobQueue> newQueues) {
    DEBUG = true;
    long tStart = System.currentTimeMillis();
    for (JobQueue q : newQueues) {
      boolean isSafe = safetyCond(q);
      if (!isSafe) {
        rejectedQueues.add(q);
        Output.debugln(DEBUG, "[BPFScheduler] reject " + q.getQueueName() + " at " + Simulator.CURRENT_TIME);
        continue;
      }

      if (q.isLQ) {
        boolean isResource = resourceCond(q);
        boolean isFair = fairnessCond(q);

        if (isResource && isFair) {
          hardGuaranteeQueues.add(q);
          Output.debugln(DEBUG, "[BPFScheduler] admit " + q.getQueueName() + " to hardGuaranteeQueues at "
              + Simulator.CURRENT_TIME);
        } else {
          if (isFair && enableSoftGuarantee) {
            softGuaranteeQueues.add(q);
            Output.debugln(DEBUG, "[BPFScheduler] admit " + q.getQueueName() + "  to softGuaranteeQueues at "
                + Simulator.CURRENT_TIME);
          } else {
            elasticQueues.add(q);
            Output.debugln(DEBUG,
                "[BPFScheduler] admit " + q.getQueueName() + "  to elasticQueues at " + Simulator.CURRENT_TIME);
          }
        }
      } else {
        elasticQueues.add(q);
        Output.debugln(DEBUG,
            "[BPFScheduler] admit " + q.getQueueName() + "  to elasticQueues at " + Simulator.CURRENT_TIME);
      }
    }
    long overheads = System.currentTimeMillis() - tStart;
    DEBUG = false;
    if (SCHEDULING_OVERHEADS)
      System.out.println("Admit takes: " + overheads + " ms at " + Simulator.CURRENT_TIME);
  }

  private boolean safetyCond(JobQueue newQueue) {
    int numQueues = elasticQueues.size();
    Queue<JobQueue> allLQs = new LinkedList<JobQueue>(hardGuaranteeQueues);
    allLQs.addAll(softGuaranteeQueues);
    numQueues += allLQs.size();
    for (JobQueue q:allLQs){
      Session session = q.getSession();
      for(int n=0; n<q.getSession().getNumOfJobs(); n++){
        Resource alpha = session.getAlphas()[n];
        Resource lhs = Resources.multiply(alpha, session.getAlphaDurations()[n]);
        Resource rhs = Resources.multiply(clusterTotCapacity, session.getPeriods()[n]);
        double denom = numQueues + 1;
        rhs.divide(denom);

        if (!lhs.smallerOrEqual(rhs))
          return false;
      }
    }
    return true;
  }

  private Queue<JobQueue> updateQueueStatus() {
    Queue<JobQueue> newQueues = new LinkedList<JobQueue>();
    for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
      if (hardGuaranteeQueues.contains(q) || softGuaranteeQueues.contains(q) || elasticQueues.contains(q)
          || rejectedQueues.contains(q)) {
        // do nothing
      } else {
        newQueues.add(q);
      }
    }

    return newQueues;
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
      Output.debugln(DEBUG,
          "[BPFScheduler] Allocated to job:" + job.dagId + " @ " + job.getQueueName() + " " + job.rsrcQuota);
    }
    availRes = Resources.subtract(availRes, q.getRsrcQuota());
  }

  @Override
  public String getSchedulePolicy() {
    return schedulePolicy;
  }
}
