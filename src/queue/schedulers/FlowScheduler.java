package queue.schedulers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobArrivalComparator;
import cluster.datastructures.JobQueue;
import cluster.datastructures.QueueComparator;
import cluster.datastructures.Resource;
import cluster.datastructures.Task;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;

public class FlowScheduler implements Scheduler {
	private String schedulePolicy;

	static Resource clusterTotCapacity = null;
	static Resource clusterAvailRes = null;
	static double[] L;
	static int numberOfNodes = 0;
	private static double alphaFairness = 1;
	private static int numSchedule = 0;
	
	public FlowScheduler(double alpha) {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
//		alphaFairness = Globals.alpha;
		numberOfNodes = (int) (Globals.MACHINE_MAX_GPU + (Globals.MACHINE_MAX_CPU/Globals.CPU_PER_NODE) );
	}

	@Override
	public void computeResShare() {
		int numQueuesRuning = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs().size();
		if (numQueuesRuning == 0) {
			return;
		}
		
		List<JobQueue> activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		
		List<Integer> availableMachines = new LinkedList<Integer>();
		List<Integer> busyMachines = new LinkedList<Integer>();
		Map<Integer, Double> availableTimes = Simulator.cluster.availableTimes;
		
		double minAvailableTime = Integer.MAX_VALUE;
		for (double availTime : availableTimes.values()) {
			if(availTime < minAvailableTime)
				minAvailableTime = availTime;
		}
		
		Map<Task, Double> runningTasks = Simulator.cluster.getCurrentRunningTasks(); 
		for (Map.Entry<Task, Double> entry : runningTasks.entrySet()) {
			Task t = entry.getKey();
			int machineId = Simulator.getDag(t.dagId).machineId;
			busyMachines.add(machineId);
		}
		for (int i=0; i<numberOfNodes; i++){
			if (!busyMachines.contains(i))
				availableMachines.add(i);
		}
			
//		System.out.println("[INFO] available machines " + availableMachines + " at " + Simulator.CURRENT_TIME);
		
		boolean isReschedule = false;
//		if (minAvailableTime <= Simulator.CURRENT_TIME)
//			isReschedule = true;
		if (Simulator.CURRENT_TIME % Globals.PERIOD_FS == 0)
			isReschedule = true;
		
		if (isReschedule){
			compute_fs(clusterTotCapacity, activeQueues, alphaFairness);
//			System.out.println("number of FS schedule "+numSchedule++);
		}
		allocate_fs(availableMachines);
	}
	
	private boolean isResourceAvailable(){
		clusterAvailRes = Simulator.cluster.getClusterResAvail();
		if (clusterAvailRes.resource(0)> 0 || clusterAvailRes.resource(1)> 0)
			return true;
		return false;
	}
	
	// trigger only when a job finishes
	private static boolean compute_fs(Resource clusterTotCapacity, List<JobQueue>activeQueues, double alphaFairness) {
	// Globals.MACHINE_MAX_GPU first nodes are GPUs, later are CPUs
		Map<Integer, Double> availableTimes = Simulator.cluster.availableTimes;
		for (int iM=numberOfNodes-1; iM>=0; iM--) {
				Simulator.cluster.scheduledJobs.put(iM, new LinkedList<BaseJob>());
				availableTimes.put(iM, Simulator.CURRENT_TIME);
		}
		
		// step 1: Get current time and a time array where each element represents the time when 
		// that machine finishes its current job
		// 
		// no definition yet, assume AvailTime[i] = max(current_time, time when current job will be finished on i) for all
		// i in machineCpuQueues and machineGpuQueues
		Map<Task, Double> runningTasks = Simulator.cluster.getCurrentRunningTasks(); 

		for (Map.Entry<Task, Double> entry : runningTasks.entrySet()) {
			Task t = entry.getKey();
			int machineId = Simulator.getDag(t.dagId).machineId;
			availableTimes.put(machineId, entry.getValue());
		}
		
//		for (int machine: availableTimes.keySet()){
//			double aTime = Math.max(availableTimes.get(machine), Simulator.CURRENT_TIME);
//			availableTimes.put(machine, aTime);
//		}		
		
		// step 2: Consider all jobs from the activeQueues where the fairness score of the owner falls
		// within $\alpha$ percent. 		
		Collections.sort(activeQueues, new QueueComparator());
		int lowestFairNQueues = (int) Math.ceil(alphaFairness * activeQueues.size());
		List<JobQueue> queuesWithLowestFairness = new ArrayList<JobQueue>();
		for (int i = 0; i < lowestFairNQueues; i++)
			queuesWithLowestFairness.add(activeQueues.get(i));

		// Add all jobs from queue to job set W
		List<BaseJob> jobs = new ArrayList<BaseJob>();
		for (JobQueue jobQueue : queuesWithLowestFairness) {
			jobs.addAll(jobQueue.getQueuedUpJobs());
		}	
		int numOfJobs = jobs.size();
		if (numOfJobs ==0) 
				return true;
		
		// Create Delay Matrix & Create processing time matrix
		double[][] D = new double[numberOfNodes][numOfJobs];
		double[][] P = new double[numberOfNodes][numOfJobs];
		for (int i=0; i<numberOfNodes; i++){
			for (int j=0; j<numOfJobs; j++){
				D[i][j] = availableTimes.get(i) - jobs.get(j).arrivalTime;
				if (i< Globals.MACHINE_MAX_GPU ){
					P[i][j] = jobs.get(j).getDemand().gpuCompl;
				} else {
					P[i][j] = jobs.get(j).getDemand().cpuCompl;
				}
			}
		}
		
		// create input matrix Q
		double[][] Q = new double[numOfJobs*numberOfNodes][numOfJobs];
		for (int i=0; i<numOfJobs*numberOfNodes; i++){
			for (int j=0; j<numOfJobs; j++){
				int iN = i/numberOfNodes + 1;
				Q[i][j] = D[i%numberOfNodes][j] + iN* P[i%numberOfNodes][j];
			}
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
		
		// cares about the available nodes
		
		
		int[] sols = new HungarianAlgorithm(Q).execute();
		//TODO: how to decide CPU or GPU for a job as 2 configurations may be selected on either CPU or GPU. 
		
		// add to the scheduled jobs to the queues for scheduling later.
//		for (Integer iM : availableMachines ){
		for (int iM=numberOfNodes-1; iM>=0; iM--){
			for (int k=numOfJobs-1; k>=0; k--){
				// if job k is chosen on iM and machine iM is available.  
				if(sols[k*numberOfNodes +iM] >= 0){
					BaseJob job = jobs.get(sols[k*numberOfNodes +iM]);
					Simulator.cluster.scheduledJobs.get(iM).add(job);
					boolean isGpu = iM < Globals.MACHINE_MAX_GPU;
					double processingTime = isGpu?job.getDemand().gpuCompl:job.getDemand().cpuCompl;
					double currentAvailTime = availableTimes.get(iM);
					availableTimes.put(iM, currentAvailTime+processingTime);
				}
			}
		}
		return true;
	}
	
	private static void allocate_fs(List<Integer> availableMachines){
		for (Integer iM : availableMachines ){
			BaseJob job = Simulator.cluster.scheduledJobs.get(iM).poll();
			if (job != null)
				if (iM < Globals.MACHINE_MAX_GPU){
					boolean res = QueueScheduler.allocateResToJob(job, false);
//					System.out.println("[INFO] "+job.dagId + " starts on " + iM + " at " + Simulator.CURRENT_TIME);
					if (!res)
						System.out.println("[ERROR] cannot allocate resources to job "+job.dagId + " on " + iM);
					job.machineId = iM;
					job.onStart(clusterTotCapacity);
				} else {
					boolean res = QueueScheduler.allocateResToJob(job, true);
//					System.out.println("[INFO] "+job.dagId + " starts on " + iM + " at " + Simulator.CURRENT_TIME);
					if (!res)
						System.out.println("[ERROR] cannot allocate resources to job "+job.dagId + " on " + iM);
					job.machineId = iM;
					job.onStart(clusterTotCapacity);
				}
		}
	}
	
	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
