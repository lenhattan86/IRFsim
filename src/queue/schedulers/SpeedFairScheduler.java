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
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;

public class SpeedFairScheduler implements Scheduler {
	private boolean DEBUG = false;
	private boolean SCHEDULING_OVERHEADS = true; 
	
	private Queue<JobQueue> admittedBurstyQueues = null;  
	private Queue<JobQueue> admittedBatchQueues = null;
	private Queue<JobQueue> bestEffortQueues = null;

	private String schedulePolicy;

	Resource clusterTotCapacity = null;

	public SpeedFairScheduler() {
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
	
	private void periodicSchedule(){
//    if(Simulator.CURRENT_TIME>=Globals.DEBUG_START && Simulator.CURRENT_TIME<=Globals.DEBUG_END){
//      DEBUG = false;
//    }else
//      DEBUG = false;
    // update queue status
	  Output.debugln(DEBUG, "\n==== STEP_TIME:" + Simulator.CURRENT_TIME + " ====");
	  
    updateQueueStatus();
    // add queues to the best effort queues    
    updateBestEfforQueue();
    // obtain the available resource
//    Resource avaiRes = Simulator.cluster.getClusterResAvail();
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
    Resource remainingResources = Resources.clone(Simulator.cluster.getClusterResAvail());
    DRFScheduler.onlineDRFShare(remainingResources, (List) bestEffortQueues);
  }

  private void allocate() {
    Resource avaiRes = Simulator.cluster.getClusterResAvail();
    
    for (JobQueue q : admittedBurstyQueues) {
      // compute the rsrcQuota based on the guarateed rate.
//      Resources a = q.getGuaranteeRate(Simulator.CURRENT_TIME);
    	Resource a = getBurstyGuarantee(q);
    	Resource moreRes = Resources.subtractPositivie(a, q.getResourceUsage());
     	moreRes= Resources.piecewiseMin(moreRes, avaiRes);
      Resource remain = q.assign(moreRes);
      // assign the task
      Resource rsrcQuota = null;
      rsrcQuota = Resources.subtract(a, remain);
      moreRes = Resources.subtract(moreRes, remain);
      q.setRsrcQuota(rsrcQuota);
      avaiRes = Resources.subtractPositivie(avaiRes, moreRes);
      Output.debugln(DEBUG, "[SpeedFairScheduler] [allocate] " + q.getQueueName() +": "+rsrcQuota);
    }
//    avaiRes = Simulator.cluster.getClusterResAvail();
    // use DRF for the admitted batch queues
    Resource remainingResources = Resources.clone(avaiRes);
    if (remainingResources.distinct(Resources.ZEROS))
      DRFScheduler.onlineDRFShare(remainingResources, (List) admittedBatchQueues);
  }
  
  private Resource getBurstyGuarantee(JobQueue q){
  	Resource res = new Resource();
  	Resource alpha = q.getAlpha();
  	Resource guaranteedRes = Resources.multiply(alpha, q.getStage1Duration());
  	double lasting = (Simulator.CURRENT_TIME - q.getStartTime()) % Globals.PERIODIC_INTERVAL;
  	boolean inStage1 = lasting <= q.getStage1Duration();
  	Resource receivedRes = q.getReceivedRes(lasting);
  	boolean isGuaranteed = receivedRes.greaterOrEqual(guaranteedRes); 
  	if (inStage1 || !isGuaranteed)
  		res = alpha;
  	else {
  	  Resource nom  = Resources.multiply(clusterTotCapacity, q.getPeriod()/(admittedBatchQueues.size()+admittedBurstyQueues.size()));
      nom = Resources.subtractPositivie(nom, receivedRes);
      Resource beta = Resources.divide(nom, (q.getPeriod()-q.getStage1Duration()));
  		res = beta;
  	}
  	return res;
  }
  
  private Resource getBurstyGuaranteePreemption(JobQueue q){
    Resource res = new Resource();
    Resource alpha = q.getAlpha();
    Resource nom  = Resources.multiply(clusterTotCapacity, q.getPeriod()/(admittedBatchQueues.size()+admittedBurstyQueues.size()));
    nom = Resources.subtract(nom, Resources.multiply(alpha, q.getStage1Duration()));
    Resource beta = Resources.divide(nom, (q.getPeriod()-q.getStage1Duration()));
    if (Simulator.CURRENT_TIME % Globals.PERIODIC_INTERVAL <= q.getStage1Duration())
      res = alpha;
    else
      res = beta;
    return res;
  }
  
  private boolean resGuarateeCond(JobQueue newQueue){
    boolean result = true;
    for(double t=0.0; t<newQueue.getStage1Duration(); t+=Globals.STEP_TIME){
      Resource burstyRes = new Resource(Resources.ZEROS);
      for (JobQueue q: admittedBurstyQueues){
        burstyRes.addWith(q.getGuaranteeRate(Simulator.CURRENT_TIME+t)); //TODO: double check.
      }
      Resource alpha = newQueue.getAlpha(); // TODO: sometime it is not alpha
      result = alpha.smallerOrEqual(Resources.subtractPositivie(clusterTotCapacity, burstyRes));
      if (!result)
        break;
    }
    return result;
  }
  
  private boolean resFairnessCond(JobQueue newQueue){
    Resource alpha = newQueue.getAlpha();
    Resource lhs = Resources.multiply(alpha, newQueue.getStage1Duration());
    Resource rhs = Resources.multiply(clusterTotCapacity, newQueue.getPeriod());
    double denom = Math.max(admittedBurstyQueues.size()+admittedBatchQueues.size()+1,Double.MIN_VALUE);
    rhs.divide(denom);
    boolean result = lhs.smallerOrEqual(rhs);
    return result;
  }
  

  private void admit() {
    long tStart = System.currentTimeMillis();
    Queue<JobQueue> newAdmittedQueues = new LinkedList<JobQueue>();
    for (JobQueue q: bestEffortQueues){
      Output.debugln(DEBUG, "admit():"+q.getQueueName());
      if (q.isInteractive){
        boolean condition1 = resGuarateeCond(q);
        boolean condition2 = resFairnessCond(q);
        if ( condition1 && condition2 ){
          admittedBurstyQueues.add(q);
          newAdmittedQueues.add(q);
          Output.debugln(DEBUG, "[SpeedFairScheduler] admit " + q.getQueueName());
        }
        else
          Output.debugln(DEBUG, "[SpeedFairScheduler] cannot addmit " + q.getQueueName());
      }
      else{
        
        if (condFairness4Batch()){
          admittedBatchQueues.add(q);
          newAdmittedQueues.add(q);
          Output.debugln(DEBUG, "[SpeedFairScheduler] admit " + q.getQueueName());
        }else
          Output.debugln(DEBUG, "[SpeedFairScheduler] cannot admit " + q.getQueueName());
      }
    } 
    bestEffortQueues.removeAll(newAdmittedQueues);
    long overheads = System.currentTimeMillis() - tStart;
    
    if(SCHEDULING_OVERHEADS)
      System.out.println("Admit takes: "+overheads +" ms at "+Simulator.CURRENT_TIME);
  }
  
  private boolean condFairness4Batch(){
    boolean condition = true;
    for (JobQueue A: admittedBurstyQueues){
      Resource alpha = A.getAlpha();
      Resource lhs = Resources.multiply(alpha, A.getStage1Duration());
      Resource rhs = Resources.multiply(clusterTotCapacity, A.getPeriod());
      double denom = Math.max(admittedBurstyQueues.size()+admittedBatchQueues.size()+1, Double.MIN_VALUE);
      rhs.divide(denom);
      condition = lhs.smallerOrEqual(rhs);
      if(!condition)
        break;
    }
    
    return condition;
  }
  
  private void updateBestEfforQueue(){
//    for (JobQueue q: Simulator.QUEUE_LIST.getJobQueues()){
  	for (JobQueue q: Simulator.QUEUE_LIST.getRunningQueues()){
      if(admittedBatchQueues.contains(q) || admittedBurstyQueues.contains(q) || bestEffortQueues.contains(q)){
      }
      else {
//        Output.debugln(DEBUG, "[SpeedFairScheduler] updateBestEfforQueue adds " + q.getQueueName() + " to best effort queues");
        bestEffortQueues.add(q);
      }
    }
  }

  private void updateQueueStatus() {
  	Queue<JobQueue> temp = new LinkedList<JobQueue>();
    for (JobQueue q: admittedBurstyQueues){
      if (q.isDeactived())
      	temp.add(q);
    }
    admittedBurstyQueues.removeAll(temp);
    temp.clear();
    for (JobQueue q: admittedBatchQueues){
      if (q.isDeactived())
      	temp.add(q);
    }
    admittedBatchQueues.removeAll(temp);
    temp.clear();
    for (JobQueue q: bestEffortQueues){
      if (q.isDeactived())
      	temp.add(q);
    }
    bestEffortQueues.removeAll(temp);
  }


	public void fifoShareForJobs(JobQueue q, Resource availRes){
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
			Output.debugln(DEBUG, "[SpeedFairScheduler] Allocated to job:" + job.dagId + " @ " + job.getQueueName() +" " + job.rsrcQuota);
		}
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
	}

	@Override
	public String getSchedulePolicy() {
		return schedulePolicy;
	}
}
