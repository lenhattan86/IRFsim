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
import cluster.simulator.Simulator;

public class SJFScheduler implements Scheduler {
	private String schedulePolicy;

	static Resource clusterTotCapacity = null;
	static Resource clusterAvailRes = null;
	static double[] L;

	public SJFScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SJF";
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
		
		while(clusterAvailRes.resource(1)>0 && activeQueues.size() > 0){
			online_srpt(clusterTotCapacity, activeQueues);
			clusterAvailRes = Simulator.cluster.getClusterResAvail();
			activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		}
	}
	
	public static boolean online_srpt(Resource resCapacity, List<JobQueue> activeQueues) {
		Collections.sort(activeQueues, new QueueComparator());
		// Create set W of remaining processing times.
		List<ProcessingTime> W = new ArrayList<ProcessingTime>();
		for (JobQueue jobQueue : activeQueues) {
			for (BaseJob job: jobQueue.getActiveJobs()){
				W.add(new ProcessingTime(true, job)); // reported processing time on CPU
				W.add(new ProcessingTime(false, job)); // reported processing time on GPU
			}
		}
		
		int nJobs = W.size();
		if (nJobs <= 0)
			return false;
		
		Collections.sort(W, new ProcessingTimesComparator());		
		// preempt all the jobs.
		
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

	
	public void computeResShare_prev() {
		int numQueuesRuning = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs().size();
		if (numQueuesRuning == 0) {
			return;
		}

		for (JobQueue q : Simulator.QUEUE_LIST.getQueuesWithQueuedJobs()) {
			Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobArrivalComparator());
		}

		clusterAvailRes = Simulator.cluster.getClusterResAvail();
		List<JobQueue> activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		
		while(clusterAvailRes.resource(1)>0 && activeQueues.size() > 0){
			AlloXScheduler.online_allox(clusterTotCapacity, activeQueues, 1);
			clusterAvailRes = Simulator.cluster.getClusterResAvail();
			activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		}
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
