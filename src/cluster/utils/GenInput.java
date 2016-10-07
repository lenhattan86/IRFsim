package cluster.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.Dependency;
import cluster.datastructures.Stage;
import cluster.datastructures.StageDag;
import cluster.simulator.Main.Globals;

public class GenInput {

	public static double[] NaN = {};
	public static double weight = 1.0;
	public static String queueFile = "input_gen/queue_input";
	public static String jobFile = "input_gen/jobs_input";
	
	public static Randomness rand = new Randomness();

	// test geninput
	public static void main(String[] args) {
		int numInteractiveQueues = 1, numInteractiveJobsPerQueue = 10;
		int numInteractiveTask = 200, numBatchQueues = 3, numBatchJobsPerQueue = 20;
		// genInput(numInteractiveQueues, numInteractiveJobsPerQueue,
		// numInteractiveTask, numBatchQueues,
		// numBatchJobsPerQueue);

		Queue<BaseDag> jobs = readWorkloadTrace("workload/" + "queries_bb_FB_distr.txt");
//		System.out.println("Print Jobs");
//		for (BaseDag job : jobs) {
//			String str = ((StageDag) job).viewDag();
//			System.out.println(str);
//		}

		genInputFromWorkload(numInteractiveQueues, numInteractiveJobsPerQueue, numInteractiveTask,
		    numBatchQueues, numBatchJobsPerQueue, jobs);

	}

	public static void genQueueInput(int numInteractiveQueues, int numBatchQueues) {
		String file = GenInput.queueFile + "_" + numInteractiveQueues + "_" + numBatchQueues + ".txt";
		Output.write("", false, file);
		for (int i = 0; i < numInteractiveQueues; i++) {
			int queueId = i;
			String toWrite = GenInput.genSingleQueueInfo(queueId, "interactive" + queueId,
			    Globals.SpeedFair_WEIGHT, weight, Globals.RATES, Globals.RATE_DURATIONS);
			Output.writeln(toWrite, true, file);
		}

		for (int i = 0; i < numBatchQueues; i++) {
			int queueId = i;
			String toWrite = GenInput.genSingleQueueInfo(queueId, "batch" + queueId, weight, weight, NaN,
			    NaN);
			Output.writeln(toWrite, true, file);
		}
	}

	public static void genJobInput(int numInteractiveQueues, int numInteractiveJobsPerQueue,
	    int numInteractiveTask, int numBatchQueues, int numBatchJobsPerQueue) {

		double[] resources1 = { 0.1, 0.05, 0.0, 0.0, 0.0, 0.0 };

		String file = GenInput.jobFile + "_" + numInteractiveQueues + "_" + numBatchQueues + ".txt";
		Output.write("", false, file);

		for (int i = 0; i < numInteractiveQueues; i++) {
			int arrivalTime = 0 + i;
			for (int j = 0; j < numInteractiveJobsPerQueue; j++) {
				arrivalTime = j * 10 + i;
				int jobId = i * numInteractiveJobsPerQueue + j;
				String toWrite = genSingleJobInfo(jobId, "interactive" + i, jobId + "", arrivalTime,
				    numInteractiveTask, Globals.STEP_TIME, resources1);
				Output.writeln(toWrite, true, file);
			}
		}

		double[] resources2 = { 0.1, 0.15, 0.0, 0.0, 0.0, 0.0 };

		int batchStartId = numInteractiveQueues * numInteractiveJobsPerQueue;
		for (int i = 0; i < numBatchQueues; i++) {
			int arrivalTime = 0 + i;
			for (int j = 0; j < numBatchJobsPerQueue; j++) {
				arrivalTime = j * 1 + i;
				int jobId = i * numBatchJobsPerQueue + j + batchStartId;
				String toWrite = genSingleJobInfo(jobId, "batch" + (i), jobId + "", arrivalTime,
				    Globals.numbatchTask, Globals.STEP_TIME, resources2);
				Output.writeln(toWrite, true, file);
			}
		}
	}

	public static String genSingleJobInfo(int jobId, String queueName, String jobName,
	    int arrivalTime, int numOfTasks, double taskDur, double[] resources) {
		int numOfStage = 1;
		String str = "";
		str += "# " + jobId + "\n";
		str += "" + numOfStage + " " + jobId + " " + arrivalTime + " " + queueName + "\n";
		str += "Stage_0 " + taskDur;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			str += " " + resources[i];
		}
		str += " " + numOfTasks + "\n";
		str += "0";
		return str;
	}

	public static String genSingleJobInfo(int jobId, String queueName, StageDag job, int arrivalTime, int scale) {
		String str = "";
		str += "# " + jobId + "\n";
		//TODO: customize the job arrival time
		str += "" + job.numStages + " " + jobId + " " + arrivalTime + " " + queueName + "\n"; 
		for (Map.Entry<String, Stage> entry : job.stages.entrySet()) {
			Stage stage = entry.getValue();
			str += stage.name + " " + stage.vDuration;
			for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
				str += " " + stage.vDemands.resource(i);
			}
			str += " " + stage.vids.length()*scale + "\n";
		}
		str += job.numEdgesBtwStages;
		for (Map.Entry<String, Stage> entry : job.stages.entrySet()) {
			Stage stage = entry.getValue();
			if (!stage.children.isEmpty()){
				for (Map.Entry<String, Dependency> child : stage.children.entrySet()) 
					str += "\n"+stage.name + " " + child.getKey() + " ata";
			}
		}
		return str;
	}

	public static String genSingleQueueInfo(int queueId, String queueName, double speedFairWeight,
	    double weight, double[] rates, double[] durations) {
		String str = "";
		str += "# " + queueId + "\n";
		str += "" + queueName + " " + speedFairWeight + " " + weight + "\n";
		int rateLen = rates.length;
		str += "" + rates.length;
		if (rateLen > 0)
			str += "\n";
		for (int i = 0; i < rateLen; i++) {
			str += "" + durations[i] + " " + rates[i];
			if (i < rateLen - 1)
				str += "\n";
		}
		return str;
	}

	public static void genInputFromWorkload(int numInteractiveQueues, int numInteractiveJobsPerQueue,
	    int numInteractiveTask, int numBatchQueues, int numBatchJobsPerQueue, Queue<BaseDag> jobs) {

		genQueueInput(numInteractiveQueues, numBatchQueues);

		customizeJobs(numInteractiveQueues, numInteractiveJobsPerQueue, numBatchQueues, numBatchJobsPerQueue, 
				jobs);
	}

	private static void customizeJobs(int numInteractiveQueues, int numInteractiveJobsPerQueue, int numBatchQueues, int numBatchJobsPerQueue, Queue<BaseDag> jobs) {
		
		String file = GenInput.jobFile + "_" + numInteractiveQueues + "_" + numBatchQueues + ".txt";
		Output.write("", false, file);
		// TODO: pick the short jobs for the interactive queue.
		Queue<BaseDag> shortJobs = getJobs(jobs, Globals.SMALL_JOB_MAX_DURATION , Globals.SMALL_JOB_THRESHOLD, true);
		Iterator<BaseDag> jobIter1 = shortJobs.iterator();
		for (int i = 0; i < numInteractiveQueues; i++) {
			int arrivalTime = 0 + i;
			for (int j = 0; j < numInteractiveJobsPerQueue; j++) {
				if (jobIter1.hasNext()){
					StageDag job = (StageDag) jobIter1.next();
					arrivalTime = j * Globals.PERIODIC_INTERVAL + i;
					int newJobId = i * numInteractiveJobsPerQueue + j;
					String toWrite = genSingleJobInfo(newJobId , "interactive" + (i), job, arrivalTime, Globals.SCALE_UP_INTERACTIV_JOB);
					Output.writeln(toWrite, true, file);
				} else {
					System.err.println("[GenInput] lack of the number of small jobs at "+shortJobs.size());
					break;
				}
			}
		}
		
		Queue<BaseDag> longJobs = getJobs(jobs, Globals.LARGE_JOB_MAX_DURATION, Globals.LARGE_JOB_THRESHOLD, false);
		Iterator<BaseDag> jobIter2 = longJobs.iterator();
		int batchStartId = numInteractiveQueues * numInteractiveJobsPerQueue;
		for (int i = 0; i < numBatchQueues; i++) {
			for (int j = 0; j < numBatchJobsPerQueue; j++) {
				int jobIdx = i * numBatchJobsPerQueue + j;
				if (jobIter2.hasNext()) {
					StageDag job = (StageDag) jobIter2.next();
					int newJobId = jobIdx + batchStartId;
					String toWrite = genSingleJobInfo(newJobId, "batch" + (i), job, job.arrivalTime, 1);
					Output.writeln(toWrite, true, file);
				} else {
					System.err.println("[GenInput] lack of the number of large jobs at "+longJobs.size());
					break;
				}
			}
		}
	}
	
	public static Queue<BaseDag> getJobs(Queue<BaseDag> jobs, double minComplTime, int numOfTasks, boolean isSmall){
		Queue<BaseDag> interactiveJobs = new LinkedList<BaseDag>();
		for (BaseDag job: jobs){
			if (job.minCompletionTime() < minComplTime && job.allTasks().size()<numOfTasks && isSmall)
				interactiveJobs.add(job);
			else if(job.minCompletionTime() > minComplTime && job.allTasks().size()>numOfTasks && !isSmall)
				interactiveJobs.add(job);
		}
		return interactiveJobs;
	}

	public static void genInput(int numInteractiveQueues, int numInteractiveJobsPerQueue,
	    int numInteractiveTask, int numBatchQueues, int numBatchJobsPerQueue) {
		genQueueInput(numInteractiveQueues, numBatchQueues);
		genJobInput(numInteractiveQueues, numInteractiveJobsPerQueue, numInteractiveTask,
		    numBatchQueues, numBatchJobsPerQueue);
	}

	public static Queue<BaseDag> readWorkloadTrace(String workloadFile) {
		Queue<BaseDag> jobs = new LinkedList<BaseDag>();
		jobs = StageDag.readDags(workloadFile, 0, 500); // change the parameters.
		return jobs;
	}
}
