package cluster.datastructures;

import java.util.LinkedList;
import java.util.Queue;

public class JobQueueList {
	private Queue<JobQueue> jobQueues = null;
	
	public  Queue<JobQueue>  getJobQueues(){
		return jobQueues;
	}
	
	public JobQueueList(){
		jobQueues = new LinkedList<JobQueue>();
	}
	
	public JobQueue getJobQueue(String queueName){
		JobQueue queue = null;
		for (JobQueue q: this.jobQueues){
			if (q.getQueueName().equals(queueName)){
				queue = q;
				break;
			}
		}
		return queue;
	}
	
	public JobQueue addJobQueue(String queueName){
		JobQueue queue = getJobQueue(queueName);
		if (queue==null)
			queue = new JobQueue(queueName);
		jobQueues.add(queue);
		return queue;
	}

	public void addRunnableJob2Queue(BaseDag newJob, String queueName) {
		JobQueue queue = getJobQueue(queueName);
		queue.runnableJobs.add(newJob);
	}
	
	public double avgCompletionTime(JobQueue queue){
		
		if (queue==null || queue.runnableJobs.size() <= 0) {
			System.err.println("this queue is empty or does not exist.");
				return -1.0;
		}
		
		double avgTime = 0.0;
		for (BaseDag job: queue.runnableJobs)
			avgTime += job.getCompletionTime();
		avgTime = avgTime/queue.runnableJobs.size();
		
		return avgTime;
	}
}
