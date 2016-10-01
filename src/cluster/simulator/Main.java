package cluster.simulator;

import java.util.logging.Logger;

import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.JobsArrivalPolicy;
import cluster.simulator.Main.Globals.Method;
import cluster.simulator.Main.Globals.Runmode;
import cluster.simulator.Main.Globals.SetupMode;
import cluster.simulator.Main.Globals.SchedulingPolicy;
import cluster.simulator.Main.Globals.SharingPolicy;
import cluster.utils.GenInput;
import cluster.utils.Output;
import cluster.utils.Utils;

public class Main {

	private static Logger LOG = Logger.getLogger(Main.class.getName());

	public static class Globals {

		public enum SetupMode {
			Mosharaf, Tan, CommandLine, GenerateTrace
		};

		public enum Runmode {
			SingleRun, MultipleRun
		};

		// public static RunMode runmode = RunMode.CommandLine;
		public static SetupMode setup = SetupMode.Tan;
		public static Runmode runmode = Runmode.MultipleRun;

		public static boolean DEBUG_ALL = false;
		public static boolean DEBUG_LOCAL = true;

		public static enum Method {
			DRF, DRFW, SpeedFair, Strict
		}

		public static enum QueueSchedulerPolicy {
			Fair, DRF, SpeedFair
		};

		public static QueueSchedulerPolicy QUEUE_SCHEDULER = QueueSchedulerPolicy.DRF;

		public static enum SchedulingPolicy {
			Random, BFS, CP, Tetris, Carbyne, SpeedFair, Yarn
		};

		public static SchedulingPolicy INTRA_JOB_POLICY = SchedulingPolicy.CP;

		public enum SharingPolicy {
			Fair, DRF, SJF, TETRIS_UNIVERSAL, SpeedFair
		};

		public static SharingPolicy INTER_JOB_POLICY = SharingPolicy.Fair;

		public enum JobsArrivalPolicy {
			All, One, Distribution, Trace;
		}

		public static JobsArrivalPolicy JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.All;

		public static int NUM_MACHINES = 1; // TODO: NUM_MACHINES > 1 may results in
																				// low utilization this simulation.
		public static int NUM_DIMENSIONS;
		public static double MACHINE_MAX_RESOURCE;
		public static int DagIdStart, DagIdEnd;

		public static Method METHOD = Method.SpeedFair;
		public static double DRFW_weight = 4.0;
		public static double STRICT_WEIGHT = Double.MAX_VALUE/20.0;

		public static boolean ADJUST_FUNGIBLE = false;

		public static double SIM_END_TIME = 10;
		public static double STEP_TIME = 1.0;

		public static int NUM_OPT = 0, NUM_PES = 0;

		public static int MAX_NUM_TASKS_DAG = 3000;

		public static boolean TETRIS_UNIVERSAL = false;
		/**
		 * these variables control the sensitivity of the simulator to various
		 * factors
		 */
		// between 0.0 and 1.0; 0.0 it means jobs are not pessimistic at all
		public static double LEVEL_OF_OPTIMISM = 0.0;

		public static boolean COMPUTE_STATISTICS = false;
		public static double ERROR = 0.0;
		public static boolean IS_GEN = true;

		/**
		 * these variables will be set by the static constructor based on runmode
		 */
		public static String DataFolder;
		public static String outputFolder = "output";
		public static String FileInput = "dags-input.txt";
		public static String QueueInput = "queue_input.txt";
		public static String FileOutput = "dags-output.txt";
		public static String PathToInputFile = DataFolder + "/" + FileInput;
		public static String PathToQueueInputFile;
		public static String PathToOutputFile = "";
		public static String PathToResourceLog = "";

		public static double[] RATES = { 200.0 };
		public static double[] RATE_DURATIONS = { 2.0 };

		public static int numInteractiveQueues = 1, numInteractiveJobPerQueue = 10, numInteractiveTask = 200;
		public static int numBatchQueues = 3, numBatchJobPerQueue = 10;
		public static int numbatchTask = 2000;

		public static void setupParameters() {
			switch (setup) {
			case Tan:
				DagIdStart = 0;
				DagIdEnd = 500;
				SIM_END_TIME = 5000;
				NUM_DIMENSIONS = 2;
				MACHINE_MAX_RESOURCE = 200;
				DRFW_weight = 4.0;

				DataFolder = "input";
				FileInput = "dags-input-simple.txt";
				QueueInput = "queue_input.txt";
				// FileInput = "dags-input-multiple-batches.txt"; QueueInput =
				// "queue_input_multi_batches.txt";
				// FileInput = "dags-input-multiple-batches-long-interactive.txt";
				// QueueInput = "queue_input_multi_batches.txt";
				// FileInput = "dags-input-multiple-interactives.txt"; QueueInput =
				// "queue_input_multi_interactives.txt";

				ADJUST_FUNGIBLE = false;
				JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.Trace;

				// sensitivity
				LEVEL_OF_OPTIMISM = 1.0;
				TETRIS_UNIVERSAL = false;
				COMPUTE_STATISTICS = false;
				ERROR = 0.0;
				break;
			case CommandLine:
				break;
			case GenerateTrace:
				break;
			default:
				System.err.println("Unknown runmode");
			}
		}
	}

	public static void main(String[] args) {

		Globals.setupParameters();
		Globals.runmode = Runmode.SingleRun;
		if (Globals.runmode.equals(Runmode.MultipleRun)) {
			// Method[] methods = { Method.DRF, Method.DRFW, Method.SpeedFair,
			// Method.Strict };
			// int[] batchQueueNums = { 1, 2, 3, 4 };
			Method[] methods = { Method.DRF };
			int[] batchQueueNums = { 1, 2 };
			Globals.numInteractiveTask = 400;
			Globals.numInteractiveJobPerQueue = 10;
			// Globals.numInteractiveTask = 2000; Globals.numInteractiveJobPerQueue =
			// 20; Globals.numBatchJobPerQueue = 10;
			// Globals.numInteractiveTask = 16000; Globals.numInteractiveJobPerQueue =
			// 40; Globals.numBatchJobPerQueue = 15;

			for (int j = 0; j < batchQueueNums.length; j++)
				for (int i = 0; i < methods.length; i++) {
					Globals.METHOD = methods[i];
					Globals.numBatchQueues = batchQueueNums[j];
					System.out.println("========================================================================");
					System.out.println("Run METHOD: " + Globals.METHOD + " with " + Globals.numBatchQueues + " batch queues.");
					runSimulationScenario();
					System.out.println("========================================================================");
				}
		} else if (Globals.runmode.equals(Runmode.SingleRun)) {
//			Globals.METHOD = Method.DRFW;
			Globals.METHOD = Method.DRF;
			// Globals.numInteractiveTask = 400; Globals.numInteractiveJobPerQueue =
			// 10;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 3;
			Globals.numInteractiveTask = 100;
			Globals.numbatchTask = 200;
			Globals.numInteractiveJobPerQueue = 5;
			Globals.numBatchJobPerQueue = 3;
			Globals.numBatchQueues = 1;
			System.out.println("========================================================================");
			System.out.println("Run METHOD: " + Globals.METHOD + " with " + Globals.numBatchQueues + " batch queues.");
			runSimulationScenario();
			System.out.println("========================================================================");
		}

		System.out.println("\n");
		System.out.println("........FINISHED ./.");
	}

	public static void runSimulationScenario() {

//		if (Globals.METHOD.equals(Method.DRF) && Globals.IS_GEN) {
		if (Globals.METHOD.equals(Method.SpeedFair) && Globals.IS_GEN) {
			GenInput.genInput(Globals.numInteractiveQueues, Globals.numInteractiveJobPerQueue, Globals.numInteractiveTask,
					Globals.numBatchQueues, Globals.numBatchJobPerQueue);
		}

		if (Globals.METHOD.equals(Method.DRF)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.INTRA_JOB_POLICY = SchedulingPolicy.Yarn;
			Globals.FileOutput = "DRF-output" + "_" + Globals.numBatchQueues + ".csv";
		} else if (Globals.METHOD.equals(Method.SpeedFair)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.SpeedFair;
			Globals.INTRA_JOB_POLICY = SchedulingPolicy.Yarn;
			Globals.FileOutput = "SpeedFair-output" + "_" + Globals.numBatchQueues + ".csv";
		} else if (Globals.METHOD.equals(Method.DRFW)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.INTRA_JOB_POLICY = SchedulingPolicy.Yarn;
			Globals.FileOutput = "DRF-W-output" + "_" + Globals.numBatchQueues + ".csv";
		} else if (Globals.METHOD.equals(Method.Strict)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.INTRA_JOB_POLICY = SchedulingPolicy.Yarn;
			Globals.FileOutput = "Strict-output" + "_" + Globals.numBatchQueues + ".csv";
		} else {
			System.err.println("Error! test case");
			return;
		}

		if (Globals.IS_GEN) {
			Globals.DataFolder = "input_gen";
			Globals.FileInput = "jobs_input_1_" + Globals.numBatchQueues + ".txt";
			Globals.QueueInput = "queue_input_1_" + Globals.numBatchQueues + ".txt";
		}

		Globals.PathToInputFile = Globals.DataFolder + "/" + Globals.FileInput;
		Globals.PathToQueueInputFile = Globals.DataFolder + "/" + Globals.QueueInput;
		Globals.PathToOutputFile = Globals.outputFolder + "/" + Globals.FileOutput;
		Globals.PathToResourceLog = "log" + "/" + Globals.FileOutput;

		// print ALL parameters for the record
		System.out.println("=====================");
		System.out.println("Simulation Parameters");
		System.out.println("=====================");
		System.out.println("PathToInputFile     = " + Globals.PathToInputFile);
		System.out.println("SIMULATION_END_TIME = " + Globals.SIM_END_TIME);
		System.out.println("STEP_TIME           = " + Globals.STEP_TIME);
		System.out.println("QUEUE_SCHEDULER    = " + Globals.QUEUE_SCHEDULER);
		System.out.println("=====================\n");

		System.out.println("Start simulation ...");
		System.out.println("Please wait ...");
		Simulator simulator = new Simulator();
		simulator.simulateMultiQueues();
		System.out.println("\nEnd simulation ...");
	}
}
