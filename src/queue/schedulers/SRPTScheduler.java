package queue.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.JobArrivalComparator;
import cluster.datastructures.JobQueue;
import cluster.datastructures.ProcessingTime;
import cluster.datastructures.ProcessingTimesComparator;
import cluster.datastructures.QueueComparator;
import cluster.datastructures.Resource;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;

public class SRPTScheduler implements Scheduler {
	private String schedulePolicy;

	static Resource clusterTotCapacity = null;
	static Resource clusterAvailRes = null;
	static double[] L;

	public SRPTScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SRPT";
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
		online_srpt();
	}
	
	public static void online_srpt() {
		// preempt all the jobs.
		if (Globals.EnablePreemption)
			Simulator.cluster.preemptAllTasks();
		List<JobQueue> runningQueues = Simulator.QUEUE_LIST.getRunningQueues();
		
		Collections.sort(runningQueues, new QueueComparator());
		// Create set W of remaining processing times.
		List<ProcessingTime> W = new ArrayList<ProcessingTime>();
		for (JobQueue jobQueue : runningQueues) {
			for (BaseJob job: jobQueue.getActiveJobs()){
				W.add(new ProcessingTime(true, job)); // reported processing time on CPU
				W.add(new ProcessingTime(false, job)); // reported processing time on GPU
			}
		}
		
		int nJobs = W.size();
		if (nJobs <= 0)
			return;
		
		Collections.sort(W, new ProcessingTimesComparator());		
		
		// then schedule all jobs to the servers.	
		// for each job in W, update fair score for each queue		
		int numScheduledJobs = 0;
		for (ProcessingTime p : W){
			if (p.job.wasScheduled)
				continue;
			int jobId = p.job.dagId;
			Resource availRes = Simulator.cluster.getClusterResAvail();
			if (!p.isCpu && availRes.resource(1) >= 1) {
				boolean res = QueueScheduler.allocateResToJob(p.job, false);
				availRes = Simulator.cluster.getClusterResAvail(); 
				if (res) {			
					System.out.println("[INFO] place job "+ p.job.dagId + " at " + Simulator.CURRENT_TIME + " on GPU");
					p.job.wasScheduled = true;
					numScheduledJobs++;
				} 
			} else if (p.isCpu && availRes.resource(0) >= 1) {
				boolean res = QueueScheduler.allocateResToJob(p.job, true);
				if (res) {
					System.out.println("[INFO] place job "+ p.job.dagId + " at " + Simulator.CURRENT_TIME + " on CPU");
					p.job.wasScheduled = true;
					numScheduledJobs++;
				} 
			}
		}
	}

	
	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
