package queue.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import cluster.datastructures.BaseJob;
import cluster.datastructures.JobArrivalComparator;
import cluster.datastructures.JobQueue;
import cluster.datastructures.QueueComparator;
import cluster.datastructures.Resource;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;

public class FlowScheduler implements Scheduler {
	private String schedulePolicy;

	static Resource clusterTotCapacity = null;
	static Resource clusterAvailRes = null;
	static double[] L;
	static ArrayList<Queue<BaseJob>> machineCpuQueues;
	static ArrayList<Queue<BaseJob>> machineGpuQueues;
	private static double alphaFairness = 1;
	
	public FlowScheduler(double alpha) {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "FS";
		int numOfQueue = (int) (clusterTotCapacity.resource(0)/24);
		machineGpuQueues = new ArrayList<Queue<BaseJob>>(numOfQueue);
		machineCpuQueues = new ArrayList<Queue<BaseJob>>(numOfQueue);
		alphaFairness = Globals.alpha;
	}

	@Override
	public void computeResShare() {
		int numQueuesRuning = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs().size();
		if (numQueuesRuning == 0) {
			return;
		}

		for (JobQueue q : Simulator.QUEUE_LIST.getQueuesWithQueuedJobs()) {
			Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobArrivalComparator());
		}

		clusterAvailRes = Simulator.cluster.getClusterResAvail();
		List<JobQueue> activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		
		online_fs();
	}
	
	// trigger only when a job finishes
	private static void online_fs() {
		// step 1: Get current time and a time array where each element represents the time when 
		// that machine finishes its current job
		// 
		// no definition yet, assume AvailTime[i] = max(current_time, time when current job will be finished on i) for all
		// i in machineCpuQueues and machineGpuQueues
		
				
		// step 2: Consider all jobs from the activeQueues where the fairness score of the owner falls
		// within $\alpha$ percent. 		
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
		
		
		
		// step 3: solve an assignment problem based on hungarian method, generate the matrix Q:
		// size(Q) = size(size(jobs) * 2*numOfQueue), jobs)
		// Delay matrix: current delay of machine $i$ to schedule job $j$ immediately, so it is a (2*numOfQueue,size(jobs)) matrix,
		// D(i,j) =  AvailTime[i] - Arrival_time(j);
		
		// Processing time matrix, size is also (2*numOfQueue,size(jobs))
		// Q = [D;D;D;...] + [P;2P;3P;...nP] where n is size(jobs)
		// solve Q based on blablabla method;
		// from results Q: if machine $i$ is available, then we look back to scan row from 2*(y-1)+i where y starts from 2*numOfQueue to 1
		// until we have a non (-1) solution
		// if so, we just schedule that job (start from 0, so if number is 4, schedule job 5 on machine i)
		// update AvailTime[i]
		// update fairness score[i];
		// if all entries are (-1), skip until a new arrival or a job finishes
		
		
		
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
	
	
	
	
	
	
	
	
	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
