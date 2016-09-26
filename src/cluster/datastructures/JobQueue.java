package cluster.datastructures;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;
import cluster.speedfair.ServiceCurve;
import cluster.utils.Output;

public class JobQueue {
	public Queue<BaseDag> runnableJobs; // Jobs are in the queue
	public Queue<BaseDag> runningJobs;
	public Queue<BaseDag> completedJobs;
	
	private final static boolean DEBUG=true;
	
	public boolean isInteractive = true;
	
	public double weight = 1;
	
	public double startTime = -1.0;

	public ServiceCurve serviceCurve = new ServiceCurve();

	public double shortTerm = Globals.SHORT_TERM;
	public double longTerm = Globals.LONG_TERM;

	public Resources resShortTermGuartRate = new Resources(70);
	public Resources resLongTermGuartRate = new Resources(50);

	public List<Resources> receivedResourcesList = new LinkedList<Resources>();

	private Resources resDemand = new Resources();

	private Resources rsrcQuota = new Resources();

	String queueName = "";

	public JobQueue(String queueName) {
		this.queueName = queueName;
		// TODO: change it later~ this is hard coded.
		if (queueName.startsWith("batch")) {
			isInteractive = false;
			resShortTermGuartRate = new Resources(20);
			resLongTermGuartRate = new Resources(50);
			weight = 1.0;
		} else if (queueName.startsWith("interactive")) {
			resShortTermGuartRate = new Resources(100);
			resLongTermGuartRate = new Resources(50);
			weight = Globals.INTERACTIVE_WEIGHT;
		} else {
			System.err.println("unknown queue name!!");
		}
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
//		Output.debugln(DEBUG,"resLongTermGuartRate:"+resLongTermGuartRate);
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
		this.rsrcQuota = rsrcQuota;
	}

	public Resources getRsrcQuota() {
		return this.rsrcQuota;
	}

	public Resources getResRate(List<Resources> resList, double term) {
		int timeSteps = (int) Math.round(term / Globals.STEP_TIME);
		Resources res = new Resources();
		Iterator<Resources> iRes = resList.iterator();
		int i = 0;
		while (iRes.hasNext()) {
			if (i++ > timeSteps)
				break;
			res = Resources.add(res, iRes.next());
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

	public Resources getShortTermRate() {
		return getResRate(this.shortTerm);
	}

	public Resources getShortTermRes() {
		double mShort = Math.min(this.shortTerm, Simulator.CURRENT_TIME);
		return getResRate(mShort);
	}

	public Resources getLongTermRate() {
		double mLong = Math.min(this.longTerm, Simulator.CURRENT_TIME);
		return getResRate(mLong);
	}

	public void addResourcesList(Resources res) {
		this.receivedResourcesList.add(0, res);
	}

	public Resources computeShortTermShare() {
		double mShort = Math.min(this.shortTerm, Simulator.CURRENT_TIME + Globals.STEP_TIME - startTime);
		return computeShare(mShort, this.resShortTermGuartRate);
	}

	public Resources computeLongTermShare() {
		double mLong = Math.min(this.longTerm, Simulator.CURRENT_TIME + Globals.STEP_TIME - startTime);
		return computeShare(mLong,this.resLongTermGuartRate);
	}

	public Resources computeShare(double term, Resources guartRate) {
		Resources resQuota = new Resources();
		Resources received = this.getReceivedRes(term - Globals.STEP_TIME);
		Resources total = Resources.multiply(guartRate, (int) (Math.round(term / Globals.STEP_TIME)));
		resQuota = Resources.subtractPositivie(total, received);
		Resources resDemand = new Resources();
		for (BaseDag job : runningJobs) {
			resDemand = Resources.add(resDemand, job.getMaxDemand());
		}
		return Resources.piecewiseMin(resQuota, resDemand);
	}
	
	public Resources getResourceUsage(){
		Resources res = new Resources();
		for (BaseDag job : this.runningJobs) {
			res.addWith(job.rsrcInUse);
		}
		return res;
	}
	
	public String getResourceUsageStr(){
		String str = this.queueName;
		for (int i=0; i<Globals.NUM_DIMENSIONS; i++)
			str += "," + this.getResourceUsage().resource(i);
		return str;
	}
}
