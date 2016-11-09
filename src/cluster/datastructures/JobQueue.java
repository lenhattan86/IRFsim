package cluster.datastructures;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.Method;
import cluster.simulator.Simulator;
import cluster.speedfair.ServiceCurve;
import cluster.speedfair.ServiceRate;
import cluster.utils.Output;

public class JobQueue {
	
	private final static boolean DEBUG=false;
	
	private Queue<BaseDag> runnableJobs; // Jobs are in the queue
	private Queue<BaseDag> runningJobs;
	public Queue<BaseDag> completedJobs;
	
	public boolean isInteractive = false;
	
	private double weight = 1.0;
	
	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	private double startTime = 0.0;
	
	private double speedFairWeight = 1.0;

	private double startTimeOfNewJob = -1.0;
	
	private ServiceRate serviceRate = new ServiceRate();

	public List<Resources> receivedResourcesList = new LinkedList<Resources>();

	private Resources rsrcQuota = new Resources();

	String queueName = "";

	private double period;

	public JobQueue(String queueName) {
		this.queueName = queueName;
		runnableJobs = new LinkedList<BaseDag>();
		runningJobs = new LinkedList<BaseDag>();
		completedJobs = new LinkedList<BaseDag>();
	}

	public String getQueueName() {
		return this.queueName;
	}

	public void updateGuartRate() {
//		resLongTermGuartRate = Resources.divide(Simulator.cluster.getClusterMaxResAlloc(),
//				Simulator.QUEUE_LIST.getRunningQueues().size());
//		Output.debugln(DEBUG, "resLongTermGuartRate:" + resLongTermGuartRate);
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

	public void admitJobs(BaseDag newArrivalJob) {
		runnableJobs.add(newArrivalJob);
	}

	public void setRsrcQuota(Resources rsrcQuota) {
		this.rsrcQuota = new Resources(rsrcQuota);
	}

	public Resources getRsrcQuota() {
		return new Resources(rsrcQuota);
	}
	
	public Resources getJobsQuota() {
		Resources res = new Resources();
		for (BaseDag job: runningJobs){
			res.addWith(job.rsrcQuota);
		}
		return res;
	}

	public Resources getResRate(List<Resources> resList, double term) {
		int timeSteps = (int) Math.round(term / Globals.STEP_TIME);
		Resources res = new Resources();
		Iterator<Resources> iRes = resList.iterator();
		int i = 0;
		while (iRes.hasNext()) {
			if (i++ > timeSteps)
				break;
			res = Resources.sum(res, iRes.next());
		}
		return Resources.divide(res, timeSteps);
	}

	public Resources getReceivedRes(double term) {
		Resources res = new Resources();
		Iterator<Resources> iRes = this.receivedResourcesList.iterator();
		int timeSteps = (int) Math.round(term / Globals.STEP_TIME);
		int i = 0;
		while (iRes.hasNext()) {
			if (i++ > timeSteps)
				break;
			res.addWith(iRes.next());
		}
		return res;
	}

	public Resources getResRate(double term) {
		return this.getResRate(this.receivedResourcesList, term);
	}


	public void addResourcesList(Resources res) {
		this.receivedResourcesList.add(0, res);
	}


	public Resources computeShare(double term, Resources guartRate) {
		Resources resQuota = new Resources();
		Resources received = this.getReceivedRes(term - Globals.STEP_TIME);
		Resources total = Resources.multiply(guartRate, (int) (Math.round(term / Globals.STEP_TIME)));
		resQuota = Resources.subtractPositivie(total, received);
		Resources resDemand = new Resources();
		for (BaseDag job : runningJobs) {
			resDemand = Resources.sum(resDemand, job.getMaxDemand());
		}
		return Resources.piecewiseMin(resQuota, resDemand);
	}
	
	public Resources getMaxDemand(){
		Resources resDemand = new Resources();
		for (BaseDag job : runningJobs) {
			resDemand = Resources.sum(resDemand, job.getMaxDemand());
		}
		return resDemand;
	}
	
	public Resources getResourceUsage(){
		Resources res = new Resources();
		for (BaseDag job : this.runningJobs) {
			res.addWith(job.getRsrcInUse());
		}
		return res;
	}
	
	public String getResourceUsageStr(){
		String str = this.queueName;
		for (int i=0; i<Globals.NUM_DIMENSIONS; i++)
			str += "," + this.getResourceUsage().resource(i);
		return str;
	}
	
	// getters & setters
	public double getWeight() {
		double res = weight;
		if (isInteractive && Globals.METHOD.equals(Method.Strict))
			res = Globals.STRICT_WEIGHT;
		else if (isInteractive && Globals.METHOD.equals(Method.DRFW))
			res = Globals.DRFW_weight;
//		else if(isInteractive && Globals.METHOD.equals(Method.SpeedFair))
//			res = this.getSpeedFairWeight();
		return res;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public void setPeriod(double period){
		this.period = period;
	}
	
	public double getPeriod(){
		return this.period;
	}
	
	public void setSpeedFairWeight(double weight) {
		this.speedFairWeight = weight;
	}
	
	public double getSpeedFairWeight() {
		if (this.serviceRate.isBeyondGuaranteedDuration(Simulator.CURRENT_TIME, this.startTimeOfNewJob)) //add 1 condition for batch queues
			return this.speedFairWeight;
		else
			return 1.0; // make equal share to others.
	}
	
	public Resources getMinService(double currTime){
		return this.serviceRate.guaranteedResources(this.getMaxDemand(), Simulator.CURRENT_TIME, this.startTimeOfNewJob);
	}
	
	public Resources getGuaranteeRate(double currTime){
    return this.serviceRate.guaranteedResources(this.getMaxDemand(), Simulator.CURRENT_TIME, this.startTimeOfNewJob);
  }
	
	public Resources getAlpha(){
    return this.serviceRate.getAlpha();
  }


	public void addRate(double slope, double duration) {
		this.serviceRate.addSlope(slope, duration);
	}
	
	public ServiceRate getServiceRate(){
	  return this.serviceRate;
	}
	
	public double getStage1Duration(){
	  double res = 0;
	  if (this.serviceRate.getCurveDurations().get(0)!=null)
	    res = this.serviceRate.getCurveDurations().get(0);
	  return res;
	}

	public void addRunnableJob(BaseDag newJob) {
		this.runnableJobs.add(newJob);
	}

	public void removeRunningJob(BaseDag newJob) {
		this.runningJobs.remove(newJob);
	}

	public void addCompletedJob(BaseDag newJob) {
		this.completedJobs.add(newJob);
	}
	
	public boolean isActive(){
		return this.runningJobs.size()>0;
	}

	public Queue<BaseDag> getRunningJobs() {
		return this.runningJobs;
	}

	public int runningJobsSize() {
		return this.runningJobs.size();
	}

	public void addRunningJob(BaseDag newJob) {
		this.startTimeOfNewJob = Simulator.CURRENT_TIME;
		Output.debugln(DEBUG, this.queueName+ " at " +this.startTimeOfNewJob);
		this.runningJobs.add(newJob);		
	}

	public void removeRunnableJob(BaseDag oldJob) {
		this.runnableJobs.remove(oldJob);
	}
	public double getStartTimeOfNewJob() {
		return startTimeOfNewJob;
	}

	public void setStartTimeOfNewJob(double startTimeOfNewJob) {
		this.startTimeOfNewJob = startTimeOfNewJob;
	}
	
	public Queue<BaseDag> cloneRunningJobs(){
		Queue<BaseDag> jobs = new LinkedList<BaseDag>();
		for (BaseDag job: this.runningJobs)
			jobs.add(job);
		return jobs;
	}

	public Resources nextTaskRes() {
		Resources res = new Resources();
		
	  return res;
  }

	public BaseDag getUnallocRunningJob() {
		for (BaseDag job: this.runningJobs)
			if (!job.isFulllyAllocated()){
				return job;
			}
		return null;
  }

	public Resources assign(Resources assignedRes) {
		Resources remain = Resources.clone(assignedRes);
		while (true){
			BaseDag unallocJob = this.getUnallocRunningJob();
			if (unallocJob == null) {
				return remain;
			}
			//TODO: a job may have a variety of tasks having different resource demands.
			int taskId = unallocJob.getCommingTaskId();
			Resources allocRes = unallocJob.rsrcDemands(taskId);
			if (remain.greaterOrEqual(allocRes)) {
				boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
				    unallocJob.duration(taskId), allocRes);
				if (assigned) {
//					unallocJob.runningTasks.add(taskId);
//					unallocJob.runnableTasks.remove(taskId);
					remain = Resources.subtract(remain, allocRes);
					
					if (unallocJob.jobStartRunningTime<0){
            unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
          }
					// update userDominantShareArr
				} else {
					Output.debugln(DEBUG,"[DRFScheduler] Cannot assign resource to the task" + taskId
					    + " of Job " + unallocJob.dagId + " " + allocRes);
				}
			} else {
				// do not allocate to this queue any more
				 break;
			}
		}
	  return remain;
  }
}
