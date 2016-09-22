package cluster.datastructures;

import java.util.LinkedList;
import java.util.Queue;

public class JobQueue {
	public Queue<BaseDag> runnableJobs;
	public Queue<BaseDag> runningJobs;
	public Queue<BaseDag> completedJobs;
	
	String queueName = "";
	public JobQueue(String queueName){
		this.queueName = queueName;
		runnableJobs = new LinkedList<BaseDag>();
		runningJobs = new LinkedList<BaseDag>();
		completedJobs = new LinkedList<BaseDag>();
	}
	public Object getQueueName() {
		return this.queueName;
	}
	
	public double avgCompletionTime(){
		if (runnableJobs.size() <= 0) {
			System.err.println("this queue is empty");
				return -1.0;
		}
		
		double avgTime = 0.0;
		for (BaseDag job: runnableJobs)
			avgTime += job.getCompletionTime();
		avgTime = avgTime/runnableJobs.size();
		
		return avgTime;
	}
}
