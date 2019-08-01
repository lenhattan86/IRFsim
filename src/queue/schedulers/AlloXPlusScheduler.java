package queue.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import cluster.datastructures.AlloXPlusComparator;
import cluster.datastructures.BaseJob;
import cluster.datastructures.JobArrivalComparator;
import cluster.datastructures.JobProcessingTimeComparator;
import cluster.datastructures.JobQueue;
import cluster.datastructures.ProcessingTime;
import cluster.datastructures.ProcessingTimesComparator;
import cluster.datastructures.QueueComparator;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.BetaComparator;
import cluster.utils.Utils;

public class AlloXPlusScheduler implements Scheduler {
	private String schedulePolicy;

	static Resource clusterTotCapacity = null;
	static Resource clusterAvailRes = null;
	static double[] L;
	private static boolean isComputed = false;
	private static Resource computedShares[] = null;
	private static double alphaFairness = 1;

	// implementation idea:
	// 1. for every queue, compute it's total resource demand vector

	public AlloXPlusScheduler(double alpha) {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "AlloXPlus";
		alphaFairness = Globals.alpha;
	}
	
	
	public AlloXPlusScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SJF";
		alphaFairness = 1;
	}

	@Override
	public void computeResShare() {
		int numQueuesRuning = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs().size();
		if (numQueuesRuning == 0) {
			return;
		}

		for (JobQueue q : Simulator.QUEUE_LIST.getQueuesWithQueuedJobs()) {
			Collections.sort((List<BaseJob>) q.getQueuedUpJobs(), new JobArrivalComparator());
		}
		
		clusterAvailRes = Simulator.cluster.getClusterResAvail();
		List<JobQueue> activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		
		boolean flag = true;
		
		while(activeQueues.size() > 0){
			flag = online_allox(clusterTotCapacity, activeQueues, alphaFairness);
			if (!flag)
				break;
			activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		}
		
	}
	
	public static void online_allox_ilp(Resource resCapacity, List<JobQueue> activeQueues, double fairnessRatio) {
		// Create user set U with lowest fairnessRatio
		Collections.sort(activeQueues, new QueueComparator());
		int lowestFairNQueues = (int) Math.ceil(fairnessRatio * Globals.numQueues);
		List<JobQueue> queuesWithLowestFairness = new ArrayList<JobQueue>();
		for (int i = 0; i < lowestFairNQueues; i++)
			queuesWithLowestFairness.add(activeQueues.get(i));

		// Add all jobs from queue to job set W
		List<BaseJob> jobs = new ArrayList<BaseJob>();
		for (JobQueue jobQueue : queuesWithLowestFairness) {
			jobs.addAll(jobQueue.getQueuedUpJobs());
		}
		int nJobs = jobs.size();
		
		if (nJobs <= 0)
			return;

		// solve ILP problem to schedule jobs in W
		double M = Double.MIN_VALUE;
		for (BaseJob job : jobs) {
			double temp = Math.max(job.getDemand().cpuCompl, job.getDemand().gpuCompl);
			M = Math.max(temp, M);
		}
		M = 2 * M;

		double[] c = new double[3 * nJobs];
		double[][] A = new double[1 + 1 + 1 + nJobs][3 * nJobs];
		double[] b = new double[1 + 1 + 1 + nJobs];
		double[][] Aeq = new double[nJobs][3 * nJobs];
		double[] beq = new double[nJobs];

		for (int i = 0; i < nJobs; i++) {
			c[i] = jobs.get(i).getDemand().cpuCompl;
			c[i + nJobs] = jobs.get(i).getDemand().gpuCompl;
			c[i + 2 * nJobs] = M;
		}

		for (int i = 0; i < nJobs; i++) {
			// constraint 1: sum(x*c) <= C1-A1
			A[0][i] = jobs.get(i).getDemand().cpu;
			b[0] = clusterAvailRes.resource(0);

			// constraint 2: sum(x*c) <= C2-A2
			A[1][i + nJobs] = jobs.get(i).getDemand().gpu;
			b[1] = clusterAvailRes.resource(1);

			// constraint 3: sum(x*c) <= C1-A1
			A[2][i] = jobs.get(i).getDemand().mem;
			A[2][i + nJobs] = jobs.get(i).getDemand().gpuMem;
			b[2] = clusterAvailRes.resource(2);

			// constraint 4: xi + yi <=1
			A[3 + i][i] = 1;
			A[3 + i][i + nJobs] = 1;
			b[3 + i] = 1;
			// constraint 5: xi + yi + zi = 1
			Aeq[i][i] = 1;
			Aeq[i][i + nJobs] = 1;
			Aeq[i][i + 2 * nJobs] = 1;
			beq[i] = 1;
		}

		int[] sols  = null; 
//		if (Globals.EnableMatlab)
//			sols = Utils.biprog_matlab(c, A, b, Aeq, beq); 
//		else
//			sols = Utils.biprog_joptimizer(c, A, b, Aeq, beq);
		
		if (sols != null)
			// for each job in W, update fair score for each queue
			for (int i = 0; i < nJobs; i++) {
				BaseJob job = jobs.get(i);
				if (sols[i] == 1) {
					// schedule job i on CPU
					QueueScheduler.allocateResToJob(job, true);
					job.onStart(clusterTotCapacity);
				}
				if (sols[i + nJobs] == 1) {
					// schedule job i on GPU
					QueueScheduler.allocateResToJob(jobs.get(i), false);
					job.onStart(clusterTotCapacity);
				}
			}
		sols = null;
	}
	
//fairnessLevel = ~0% strict fairness
	public static boolean online_allox(Resource resCapacity, List<JobQueue> activeQueues, double fairnessRatio) {
		int nQueues = activeQueues.size();
		// Create user set U with lowest fairnessRatio
		Collections.sort(activeQueues, new QueueComparator());
		int lowestFairNQueues = (int) Math.ceil(fairnessRatio * nQueues);
		List<JobQueue> queuesWithLowestFairness = new ArrayList<JobQueue>();
		for (int i = 0; i < lowestFairNQueues; i++)
			queuesWithLowestFairness.add(activeQueues.get(i));

		// Create set W of processing times
		List<ProcessingTime> W = new ArrayList<ProcessingTime>();
		for (JobQueue jobQueue : queuesWithLowestFairness) {
			for (BaseJob job: jobQueue.getQueuedUpFullJobs()){
				W.add(new ProcessingTime(true, job)); // reported processing time on CPU
				W.add(new ProcessingTime(false, job)); // reported processing time on GPU
			}
		}
		
		int nJobs = W.size();
		if (nJobs <= 0)
			return false;
		
		Collections.sort(W, new AlloXPlusComparator());
		
		// for each job in W, update fair score for each queue		
		int numScheduledJobs = 0;
		for (ProcessingTime p : W){
			if (p.job.wasScheduled)
				continue;
			int jobId = p.job.dagId;
			Resource availRes = Simulator.cluster.getClusterResAvail(); // todo: bug
			if (!p.isCpu && availRes.resource(1) >= 1) {
				
				boolean res = QueueScheduler.allocateResToJob(p.job, false);
				availRes = Simulator.cluster.getClusterResAvail(); 
				if (res) {					
					p.job.onStart(resCapacity);
					numScheduledJobs++;
					break;
				}
			} else if (p.isCpu && availRes.resource(0) >= 1) {
				boolean res = QueueScheduler.allocateResToJob(p.job, true);
				if (res) {
					p.job.onStart(resCapacity);
					numScheduledJobs++;
					break;
				} 
			}
		}
		return numScheduledJobs>=1;
	}

	// fairnessLevel = ~0% strict fairness
	public static void online_allox_v01(Resource resCapacity, List<JobQueue> activeQueues, double fairnessRatio) {
		int nQueues = activeQueues.size();
		// Create user set U with lowest fairnessRatio
		Collections.sort(activeQueues, new QueueComparator());
		int lowestFairNQueues = (int) Math.ceil(fairnessRatio * nQueues);
		List<JobQueue> queuesWithLowestFairness = new ArrayList<JobQueue>();
		for (int i = 0; i < lowestFairNQueues; i++)
			queuesWithLowestFairness.add(activeQueues.get(i));

		// Add all jobs from queue to job set W
		List<BaseJob> jobs = new ArrayList<BaseJob>();
		for (JobQueue jobQueue : queuesWithLowestFairness) {
			jobs.addAll(jobQueue.getQueuedUpJobs());
		}
		
		Collections.sort(jobs, new JobProcessingTimeComparator());
		int nJobs = jobs.size();
		
		if (nJobs <= 0)
			return;

		double M = Double.MIN_VALUE;
		
		// for each job in W, update fair score for each queue
		LinkedList<BaseJob> job2BeOnGPU = new LinkedList<BaseJob>();
		for (int i = 0; i < nJobs; i++) {
			BaseJob job = jobs.get(i);
	    Resource availRes = Simulator.cluster.getClusterResAvail();
			if (availRes.resource(1) >= 1) {
				// schedule job i on GPU
				boolean res = QueueScheduler.allocateResToJob(job, false);
				if (res) {
					job.onStart(clusterTotCapacity);
					job2BeOnGPU.add(job);
				}
			} 
		}
		jobs.removeAll(job2BeOnGPU);
		nJobs = jobs.size();
		for (int i = nJobs; i > 0; i--) {
			BaseJob job = jobs.get(i-1);
	    Resource availRes = Simulator.cluster.getClusterResAvail();
	    double smallVal = 1;
			if (availRes.resource(0) > smallVal) {
				// schedule job i on CPU
				boolean res =QueueScheduler.allocateResToJob(job, true);
				if (res)
					job.onStart(clusterTotCapacity);
			}
		}
	}

	public static void onJobStart(BaseJob job, int iQueue) {
		// find the owner of the job
		double p1 = job.getDemand().cpuCompl;
		double p2 = job.getDemand().gpuCompl;
		if (p1 < p2) {
			if (job.isOnCPU()) {
				job.fairVal = Math.max(job.getDemand().cpu / clusterTotCapacity.resource(0),
						job.getDemand().mem / clusterTotCapacity.resource(2));
			} else {
				job.fairVal = p1 / p2 * Math.max(job.getDemand().cpu / clusterTotCapacity.resource(0),
						job.getDemand().mem / clusterTotCapacity.resource(2));
			}
		} else {
			if (job.isOnCPU()) {
				job.fairVal = p2 / p1 * Math.max(job.getDemand().gpu / clusterTotCapacity.resource(1),
						job.getDemand().gpuMem / clusterTotCapacity.resource(2));
			} else {
				job.fairVal = Math.max(job.getDemand().gpu / clusterTotCapacity.resource(1),
						job.getDemand().gpuMem / clusterTotCapacity.resource(2));
			}
		}
		L[iQueue] += job.fairVal;
	}

	public static void onJobFinished(BaseJob job, int iQueue) {
		L[iQueue] -= job.fairVal;
	}

	public static void allox_static(Resource resCapacity, List<JobQueue> runningQueues) {
		// sort queues based on beta
		int numberOfQueues = runningQueues.size();
		// double SHARES[][] = { { 3520, 0, 880 }, { 320, 28.57, 217.14 }, { 0,
		// 31.43, 62.86 } };
		// double SHARES[][] = { { 35200, 0, 8800 }, { 3200, 285.7, 2171.4 }, { 0,
		// 314.3, 628.6 } };
		double SHARES[][] = { { 3520, 0, 880 }, { 320, 28.57, 217.14 }, { 0, 31.43, 62.86 } };
		for (int i = 0; i < numberOfQueues; i++) {
			JobQueue q = runningQueues.get(i);
			double shares[] = SHARES[i];
			if (!isComputed) {
				System.out.println("i=" + i + "(" + shares[0] + "," + shares[1] + "," + shares[2] + ")");
			}
			QueueScheduler.allocateResToQueue(q, shares, true);
		}
		isComputed = true;
	}


	public static void allox_heuristic(Resource resCapacity, List<JobQueue> runningQueues) {
		// sort queues based on beta
		int numberOfQueues = runningQueues.size();
		Collections.sort((List<JobQueue>) runningQueues, new BetaComparator());
		double[] betas = new double[runningQueues.size()];
		for (int i = 0; i < runningQueues.size(); i++) {
			betas[i] = runningQueues.get(i).getReportBeta();
			// betas[i] = runningQueues.get(i).computeBetaOnRunningJobs();
		}

		// initialization
		double price[] = new double[2];
		JobQueue lastQueue = runningQueues.get(numberOfQueues - 1);
		price[0] = 1;
		price[1] = betas[numberOfQueues - 1];
		Resource useralloc[] = userAlloc(betas, price);
		Resource currLoad = Resources.sum(useralloc);
		currLoad = Resources.divideVector(currLoad, resCapacity);

		int gpumin = numberOfQueues - 1;
		boolean flag = true;

		if (numberOfQueues == 0) {
			System.err.println("numberOfQueues is too small");
			return;
		} else if (numberOfQueues == 1) {
			flag = false;
		}

		while (flag) {
			if (currLoad.resources[0] <= currLoad.resource(1)) {
				double Y = (currLoad.resource(1) - currLoad.resource(0))
						/ (betas[gpumin] / resCapacity.resource(0) + 1.0 / resCapacity.resource(1));
				Y = Math.min(1 / betas[gpumin], Y);
				useralloc[gpumin].resources[1] = useralloc[gpumin].resources[1] - Y;
				useralloc[gpumin].resources[0] = betas[gpumin] * Y;
				currLoad = Resources.sum(useralloc);
				currLoad = Resources.divideVector(currLoad, resCapacity);
				break;
			}

			gpumin = gpumin - 1;
			if (gpumin < 0)
				System.err.println("gpumin is negative");

			price[0] = 1;
			price[1] = betas[gpumin];

			useralloc = userAlloc(betas, price);
			currLoad = Resources.sum(useralloc);
			currLoad = Resources.divideVector(currLoad, resCapacity);
		}

		Resource sumAlloc = Resources.sum(useralloc);
		// step 3: allocate the resources.
		if (!isComputed) {
			System.out.println("betas=(" + betas[0] + "," + betas[1] + "," + betas[2] + ")");
		}
		for (int i = 0; i < numberOfQueues; i++) {
			JobQueue q = runningQueues.get(i);
			double shares[] = { 0, 0, 0 };
			shares[0] = Utils.roundBase(useralloc[i].resource(0) * resCapacity.resource(0) / sumAlloc.resource(0), 2);
			shares[1] = Utils.roundBase(useralloc[i].resource(1) * resCapacity.resource(1) / sumAlloc.resource(1), 2);
			// shares[0] = useralloc[i].resource(0) * resCapacity.resource(0) /
			// sumAlloc.resource(0);
			// shares[1] = useralloc[i].resource(1) * resCapacity.resource(1) /
			// sumAlloc.resource(1);
			shares[2] = shares[0] / q.getDemand().cpu * q.getDemand().mem
					+ shares[1] / q.getDemand().gpu * q.getDemand().gpuMem;
			if (!isComputed) {
				System.out.println("i=" + i + "(" + shares[0] + "," + shares[1] + "," + shares[2] + ")");
			}
			QueueScheduler.allocateResToQueue(q, shares, true);
		}
		isComputed = true;
	}

	private static Resource[] userAlloc(double[] betas, double currentPrices[]) {
		Resource userAlloc[] = Resources.NONEs(betas.length);

		for (int j = 0; j < betas.length; j++) {
			double beta = betas[j];
			if (beta < currentPrices[1]) {
				userAlloc[j].resources[0] = 1;
				userAlloc[j].resources[1] = 0;
			} else { // if beta = price, put it in GPU.
				userAlloc[j].resources[0] = 0;
				userAlloc[j].resources[1] = 1 / currentPrices[1];
			}
		}
		return userAlloc;
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
