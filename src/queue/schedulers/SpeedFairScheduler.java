package queue.schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;

public class SpeedFairScheduler implements Scheduler {
	private boolean DEBUG = false;
	
	private static boolean DIFF = false; // refer line 18 & 19 of algorithm 2.
	
	private Queue<JobQueue> admittedBurstyQueues = null;  
	private Queue<JobQueue> admittedBatchQueues = null;
	private Queue<JobQueue> bestEffortQueues = null;

	private String schedulePolicy;

	Resources clusterTotCapacity = null;

	public SpeedFairScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SpeedFair";
		this.admittedBurstyQueues = new LinkedList<JobQueue>();
		this.admittedBatchQueues = new LinkedList<JobQueue>();
		this.bestEffortQueues = new LinkedList<JobQueue>();
	}
	
	@Override
	public void computeResShare() {
		this.schedulev02();
//	  this.schedulev01();
	}
	
	private void schedulev02(){
    if(Simulator.CURRENT_TIME>=Globals.DEBUG_START && Simulator.CURRENT_TIME<=Globals.DEBUG_END){
      DEBUG = false;
    }else
      DEBUG = false;
    // update queue status
	  Output.debugln(DEBUG, "\n==== STEP_TIME:" + Simulator.CURRENT_TIME + " ====");
	  
    updateQueueStatus();
    // add queues to the best effort queues    
    updateBestEfforQueue();
    // obtain the available resource
    Resources avaiRes = Simulator.cluster.getClusterResAvail();
    // admission control
    admit();
    // allocate resources to bursty and batch queues
    allocate();
    // allocate spare resousrces.
    allocateSpareResources();
  }
	
	private void allocateSpareResources() {
	  // allocate the spare resource
    Resources remainingResources = Resources.clone(Simulator.cluster.getClusterResAvail());
    DRFScheduler.onlineDRFShare(remainingResources, (List) this.bestEffortQueues);
  }

  private void allocate() {
    Resources avaiRes = Simulator.cluster.getClusterResAvail();
    
    for (JobQueue q : this.admittedBurstyQueues) {
      // compute the rsrcQuota based on the guarateed rate.
//      Resources a = q.getGuaranteeRate(Simulator.CURRENT_TIME);
    	Resources a = this.getBurstyGuarantee(q);
    	Resources moreRes = Resources.subtractPositivie(a, q.getResourceUsage());
     	moreRes= Resources.piecewiseMin(moreRes, avaiRes);
      Resources remain = q.assign(moreRes);
      // assign the task
      Resources rsrcQuota = null;
      if(DIFF){
        rsrcQuota = Resources.subtract(a, remain);
        moreRes = Resources.subtract(moreRes, remain);
      } else {
        rsrcQuota = a;
      }
      q.setRsrcQuota(rsrcQuota);
      avaiRes = Resources.subtractPositivie(avaiRes, moreRes);
      Output.debugln(DEBUG, "[SpeedFairScheduler] [allocate] " + q.getQueueName() +": "+rsrcQuota);
    }
//    avaiRes = Simulator.cluster.getClusterResAvail();
    // use DRF for the admitted batch queues
    Resources remainingResources = Resources.clone(avaiRes);
    if (remainingResources.distinct(Resources.ZEROS))
      DRFScheduler.onlineDRFShare(remainingResources, (List) this.admittedBatchQueues);
  }
  
  private Resources getBurstyGuarantee(JobQueue q){
  	Resources res = new Resources();
  	Resources alpha = q.getAlpha();
  	Resources nom  = Resources.multiply(this.clusterTotCapacity, q.getPeriod()/(this.admittedBatchQueues.size()+this.admittedBurstyQueues.size()));
  	nom = Resources.subtract(nom, Resources.multiply(alpha, q.getStage1Duration()));
  	Resources beta = Resources.divide(nom, (q.getPeriod()-q.getStage1Duration()));
  	if (Simulator.CURRENT_TIME % Globals.PERIODIC_INTERVAL <= q.getStage1Duration())
  		res = alpha;
  	else
  		res = beta;
  	return res;
  }
  
  private boolean resGuarateeCond(JobQueue newQueue){
    boolean result = true;
    for(double t=0.0; t<newQueue.getStage1Duration(); t+=Globals.STEP_TIME){
      Resources burstyRes = new Resources(Resources.ZEROS);
      for (JobQueue q: this.admittedBurstyQueues){
        burstyRes.addWith(q.getGuaranteeRate(Simulator.CURRENT_TIME+t));
      }
      Resources alpha = newQueue.getAlpha(); // TODO: some time it is not alpha
      result = alpha.smallerOrEqual(Resources.subtractPositivie(this.clusterTotCapacity, burstyRes));
      if (!result)
        break;
    }
    return result;
  }
  
  private boolean resFairnessCond(JobQueue newQueue){
    Resources alpha = newQueue.getAlpha();
    Resources lhs = Resources.multiply(alpha, newQueue.getStage1Duration());
    Resources rhs = Resources.multiply(this.clusterTotCapacity, newQueue.getPeriod());
    double denom = Math.max(this.admittedBurstyQueues.size()+this.admittedBatchQueues.size()+1,Double.MIN_VALUE);
    rhs.divide(denom);
    boolean result = lhs.smallerOrEqual(rhs);
    return result;
  }
  

  private void admit() {
    Queue<JobQueue> newAdmittedQueues = new LinkedList<JobQueue>();
    for (JobQueue q: this.bestEffortQueues){
      if (q.isInteractive){
        boolean condition1 = resGuarateeCond(q);
        boolean condition2 = resFairnessCond(q);
        if ( condition1 && condition2 ){
          this.admittedBurstyQueues.add(q);
          newAdmittedQueues.add(q);
          Output.debugln(DEBUG, "[SpeedFairScheduler] admit " + q.getQueueName());
        }
        else
          Output.debugln(DEBUG, "[SpeedFairScheduler] cannot addmit " + q.getQueueName());
      }
      else{
        boolean condition = true;
        for (JobQueue A: this.admittedBurstyQueues){
          Resources alpha = A.getAlpha();
          Resources lhs = Resources.multiply(alpha, A.getStage1Duration());
          Resources rhs = Resources.multiply(this.clusterTotCapacity, A.getPeriod());
          double denom = Math.max(this.admittedBurstyQueues.size()+this.admittedBatchQueues.size()+1, Double.MIN_VALUE);
          rhs.divide(denom);
          condition = lhs.smallerOrEqual(rhs);
          if(!condition)
            break;
        }
        
        if (condition){
          this.admittedBatchQueues.add(q);
          newAdmittedQueues.add(q);
          Output.debugln(DEBUG, "[SpeedFairScheduler] admit " + q.getQueueName());
        }else
          Output.debugln(DEBUG, "[SpeedFairScheduler] cannot admit " + q.getQueueName());
      }
    } 
    this.bestEffortQueues.removeAll(newAdmittedQueues);
  }
  
  private void updateBestEfforQueue(){
//    for (JobQueue q: Simulator.QUEUE_LIST.getJobQueues()){
  	for (JobQueue q: Simulator.QUEUE_LIST.getRunningQueues()){
      if(this.admittedBatchQueues.contains(q) || this.admittedBurstyQueues.contains(q) || this.bestEffortQueues.contains(q)){
      }
      else {
//        Output.debugln(DEBUG, "[SpeedFairScheduler] updateBestEfforQueue adds " + q.getQueueName() + " to best effort queues");
        this.bestEffortQueues.add(q);
      }
    }
  }

  private void updateQueueStatus() {
  	Queue<JobQueue> temp = new LinkedList<JobQueue>();
    for (JobQueue q: this.admittedBurstyQueues){
      if (q.isDeactived())
      	temp.add(q);
    }
    this.admittedBurstyQueues.removeAll(temp);
    temp.clear();
    for (JobQueue q: this.admittedBatchQueues){
      if (q.isDeactived())
      	temp.add(q);
    }
    this.admittedBatchQueues.removeAll(temp);
    temp.clear();
    for (JobQueue q: this.bestEffortQueues){
      if (q.isDeactived())
      	temp.add(q);
    }
    this.bestEffortQueues.removeAll(temp);
  }


	public void fifoShareForJobs(JobQueue q, Resources availRes){
		boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
		if (!fit) {
			Resources newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
			q.setRsrcQuota(newQuota);
		}
		q.receivedResourcesList.add(q.getRsrcQuota());

		Resources remain = q.getRsrcQuota();
		List<BaseDag> runningJobs = new LinkedList<BaseDag>(q.getRunningJobs());
		Collections.sort(runningJobs, new JobArrivalComparator());
		for (BaseDag job : runningJobs) {
			Resources rsShare = Resources.piecewiseMin(remain, job.getMaxDemand());
			job.rsrcQuota = rsShare;
			remain.subtract(rsShare);
			Output.debugln(DEBUG, "[SpeedFairScheduler] Allocated to job:" + job.dagId + " @ " + job.getQueueName() +" " + job.rsrcQuota);
		}
		availRes = Resources.subtract(availRes, q.getRsrcQuota());
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
