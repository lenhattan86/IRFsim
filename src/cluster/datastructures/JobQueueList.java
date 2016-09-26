package cluster.datastructures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.Interval;
import cluster.utils.Output;
import cluster.utils.Randomness;

public class JobQueueList {
	private Queue<JobQueue> jobQueues = null;
	private List<JobQueue> runningQueues = null; 
	
	public  Queue<JobQueue>  getJobQueues(){
		return jobQueues;
	}
	
	public JobQueueList(){
		jobQueues = new LinkedList<JobQueue>();
		runningQueues = new LinkedList<JobQueue>();
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
		if (queue==null){
			queue = new JobQueue(queueName);
			jobQueues.add(queue);
		}
		return queue;
	}
	
	public JobQueue addJobQueue(JobQueue queue){
		JobQueue q = getJobQueue(queue.queueName);
		if (q==null){
			jobQueues.add(queue);
		}
		return queue;
	}

	public void addRunnableJob2Queue(BaseDag newJob, String queueName) {
		JobQueue queue = getJobQueue(queueName);
		queue.runnableJobs.add(newJob);
	}
	
	public void addRunningJob2Queue(BaseDag newJob, String queueName) {
		JobQueue queue = getJobQueue(queueName);
		queue.runningJobs.add(newJob);
		queue.runnableJobs.remove(newJob);
	}
	
	public void addCompletionJob2Queue(BaseDag newJob, String queueName) {
		JobQueue queue = getJobQueue(queueName);
		queue.runningJobs.remove(newJob);
		queue.completedJobs.add(newJob);
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
	
	public void addRunningQueue(JobQueue runningQueue){
		if (runningQueue.isInteractive)
			this.runningQueues.add(0,runningQueue);
		else
			this.runningQueues.add(runningQueue);
	}
	
	public boolean removeRunningQueue(String queueName){
		boolean res =  false;
		for (JobQueue q:runningQueues){
			if (q.queueName.equals(queueName)){
				this.runningQueues.remove(q);
				res = true;
				break;
			}
		}
		return res;
	}
	
	public boolean removeRunningQueue(JobQueue q){
		boolean res =  false;
		if (this.runningQueues.contains(q)){
			this.runningQueues.remove(q);
			res = true;
		}
		return res;
	}
	
	public List<JobQueue> updateRunningQueues(){
		this.runningQueues = new LinkedList<JobQueue>();
		for (JobQueue q:jobQueues){
			if (q.runningJobs.size() > 0){
				this.addRunningQueue(q);
			}
		}
		return this.runningQueues;
	}
	
	public List<JobQueue> getRunningQueues(){
		return runningQueues;
	}
	
	public void readQueue(String filePathString){
		File file = new File(filePathString);
    assert (file.exists() && !file.isDirectory());
    String[] args = null;
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      String queueName = "default";
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.startsWith("#")) {
        	continue; // if comments, pass
        }
        
        args = line.split(" ");
        queueName = args[0];
      	queueName = queueName.trim();
      	
      	JobQueue queue = new JobQueue(queueName);
        Simulator.QUEUE_LIST.addJobQueue(queue);

        int numOfSlopes;
        line = br.readLine();
        numOfSlopes = Integer.parseInt(line.trim());
        assert (numOfSlopes >= 0);
        for (int i = 0; i < numOfSlopes; ++i) {
        	args = br.readLine().split(" ");
          assert (args.length < numOfSlopes) : 
            "Incorrect entry for service curves";

          double duration = Double.parseDouble(args[0]), slope = Double.parseDouble(args[1]);
          if(i==numOfSlopes-1) duration = Double.MAX_VALUE;
          queue.serviceCurve.addSlope(slope, duration);
        }
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Catch exception: " + e);
    }
	}
	
}
