package cluster.utils;

import cluster.simulator.Main.Globals;

public class GenInput {

	public static double[] NaN = {};
	public static double speedFairWeight = 0.2;
	public static double weight = 1.0;
	public static String queueFile = "input_gen/queue_input";
	public static String jobFile = "input_gen/jobs_input";

	public static int numbatchTask = 2000;

	public static void genQueueInput(int numInteractiveQueues, int numBatchQueues) {
		String file = GenInput.queueFile + "_" + numInteractiveQueues + "_" + numBatchQueues + ".txt";
		Output.write("", false, file);
		for (int i = 0; i < numInteractiveQueues; i++) {
			int queueId = i;
			String toWrite = GenInput.genSingleQueueInfo(queueId, "interactive" + queueId, speedFairWeight, weight, Globals.RATES,
					Globals.RATE_DURATIONS);
			Output.writeln(toWrite, true, file);
		}

		for (int i = 0; i < numBatchQueues; i++) {
			int queueId = i;
			String toWrite = GenInput.genSingleQueueInfo(queueId, "batch" + queueId, weight, weight, NaN, NaN);
			Output.writeln(toWrite, true, file);
		}
	}

	public static void genJobInput(int numInteractiveQueus, int numOfInteractiveJobsPerQueue, int numInteractiveTask,
			int numBatchQueues, int numOfBatchJobsPerQueue) {
		String file = GenInput.jobFile + "_" + numInteractiveQueus + "_" + numBatchQueues + ".txt";
		Output.write("", false, file);
		// TODO: generate random time arrivals, number of tasks

		for (int i = 0; i < numInteractiveQueus; i++) {
			int arrivalTime = 0 + i;
			for (int j = 0; j < numOfInteractiveJobsPerQueue; j++) {
				arrivalTime = j * 10 + i;
				int jobId = i * numOfInteractiveJobsPerQueue + j;
				String toWrite = genSingleJobInfo(jobId, "interactive" + i, jobId + "", arrivalTime, numInteractiveTask, 1);
				Output.writeln(toWrite, true, file);
			}
		}

		int batchStartId = numInteractiveQueus * numOfInteractiveJobsPerQueue;
		for (int i = 0; i < numBatchQueues; i++) {
			int arrivalTime = 0 + i;
			for (int j = 0; j < numOfBatchJobsPerQueue; j++) {
				arrivalTime = j * 1 + i;
				int jobId = i * numOfBatchJobsPerQueue + j + batchStartId;
				String toWrite = genSingleJobInfo(jobId, "batch" + (i), jobId + "", arrivalTime, numbatchTask, 1);
				Output.writeln(toWrite, true, file);
			}
		}
	}

	public static String genSingleJobInfo(int jobId, String queueName, String jobName, int arrivalTime, int numOfTasks,
			double taskDur) {
		int numOfStage = 1;
		double[] resources = { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
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

	public static String genSingleQueueInfo(int queueId, String queueName, double speedFairWeight, double weight,
			double[] rates, double[] durations) {
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

	// test geninput
	public static void main(String[] args) {
		int numInteractiveQueues = 1, numInteractiveJobPerQueue = 10, numInteractiveTask = 200, numBatchQueues = 3,
				numBatchJobPerQueue = 10;

		genInput(numInteractiveQueues, numInteractiveJobPerQueue, numInteractiveTask, numBatchQueues, numBatchJobPerQueue);
	}

	public static void genInput(int numInteractiveQueues, int numInteractiveJobPerQueue, int numInteractiveTask,
			int numBatchQueues, int numBatchJobPerQueue) {
		genQueueInput(numInteractiveQueues, numBatchQueues);
		genJobInput(numInteractiveQueues, numInteractiveJobPerQueue, numInteractiveTask, numBatchQueues,
				numBatchJobPerQueue);
	}
}
