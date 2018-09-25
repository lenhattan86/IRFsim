package cluster.simulator;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Logger;

import com.mathworks.engine.MatlabEngine;

import cluster.data.JobData;
import cluster.data.SessionData;
import cluster.datastructures.BaseJob;
import cluster.simulator.Main.Globals.JobScheduling;
import cluster.simulator.Main.Globals.Method;
import cluster.simulator.Main.Globals.Runmode;
import cluster.simulator.Main.Globals.WorkLoadType;
import cluster.utils.GenInput;
import cluster.utils.Randomness;
import cluster.utils.Utils;

public class Main {

	private static Logger LOG = Logger.getLogger(Main.class.getName());
	static {
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
	}

	public static class Globals {
		
		public final static boolean EnableMatlab = false;
		public final static boolean EnableProfiling = true;
		public final static boolean EnablePreemption = true;
		public static int CPU_PROFILING_JOB1 = -100000; //-10000 -> 0
		public static int CPU_PROFILING_JOB2 = CPU_PROFILING_JOB1*2;
		public static int GPU_PROFILING_JOB1 = CPU_PROFILING_JOB1*3;
		public static int GPU_PROFILING_JOB2 = CPU_PROFILING_JOB1*4;
		public static double cpu_prof_1 = 0.02;
		public static double cpu_prof_2 = 0.04;
		public static double gpu_prof_1 = 0.02;
		public static double gpu_prof_2 = 0.04;
		
		public static JobScheduling JOB_SCHEDULER = JobScheduling.SJF; 
		
		public static MatlabEngine MATLAB = null;
		
		public static double transferRateScale = 1;

		public static WorkLoadType workload = WorkLoadType.BB;

		public static String strStage = "stage";

		public static String TRACE_FILE = "workload/queries_bb_FB_distr.txt"; // BigBench

		public static int SMALL_JOB_TASK_NUM_THRESHOLD = 250; // for 80 for 2 BB
																													// TPCDS,
		// => TPC-H --> 250

		public static JobData jobData;
		public static int NumRandomSamples = 25000; 

		public static double RES_UNIT = 0.001;
		
		public static int NUM_JOBS_FOR_AVG_CMPL = 400;

		public static final int TRACE_CLUSTER_SIZE = 25;

		public enum WorkLoadType {
			Google, BB, TPC_DS, TPC_H, SIMPLE, SIMPLE_GOOD, SIMPLE_BAD, Google_2, Tensorflow,
		};

		public enum JobScheduling {
			FIFO, SJF
		}

		public enum SetupMode {
			VeryShortInteractive, ShortInteractive, LongInteractive, VeryLongInteractive, others
		};

		public static Scenario SCENARIO = null;

		public enum Scenario {
			lbeta_cpu, lbeta_mix, sbeta_cpu, sbeta_mix, mbeta_cpu, mbeta_mix, mbeta_lcpu,
		};

		public enum Runmode {
			MultipleRuns, BetaErrors, Analysis_Alpha, Analysis_speedup, Analysis_mu, Analysis_misest, Analysis_capacity, SmallScale, NONE
		}

		public static boolean USE_TRACE = false;
		
		public static double alpha = 0.66; // ratio of user set to be scheduled.

		public static final int TASK_ARRIVAL_RANGE = 50;

		public static final int JOB_START_ID = 0;
		
		public static boolean ENABLE_CPU_CMPT_ERROR = false;

		public static SessionData SESSION_DATA = null;

		public static double DEBUG_START = 0.0;
		public static double DEBUG_END = -1.0;

		public static double SCALE_UP_BATCH_JOB = 1;
		public static double AVG_TASK_DURATION = -1.0;

		public static double ESTIMASION_ERRORS = 0.0;

		public static double WORKLOAD_AVG_TASK_DURATION = -1.0; // computed
																														// separately from
		public static double SMALL_JOB_DUR_THRESHOLD = 40.0;
		public static double LARGE_JOB_MAX_DURATION = 0.0;
		public static double LONG_DURATION_TASK_TOBE_REMOVED = -1.0;

		public static String DIST_FILE = "dist_gen/poissrnd.csv";

		public static Runmode runmode = Runmode.NONE;

		public static boolean DEBUG_ALL = false;
		public static boolean DEBUG_LOCAL = true;

		public static int TASK_BROKEN_DOWN = 1; // 100 subtask per task
		public static double TIME_UNIT = 0.1; // seconds

		public static int SCALE_UP_FACTOR = 1;

		public static enum Method {
			DRF, DRFExt, FDRF, DRFW, ES, MaxMinMem, SpeedUp, Pricing, AlloX, SJF, FS, SRPT
		}
		
		public static int PERIOD_FS = 50; 
		
		public static enum DemandChangeType {
			Running, Arrival, Total
		}
		
		public static enum ChangeDetectionMethod {
			Periodic, Adaptive, GLR  
		}

		public static enum QueueSchedulerPolicy {
			DRF, DRFExt, ES, MaxMinMem, SpeedUp, Pricing, AlloX, SJF, FS, SRPT
		};

		public static QueueSchedulerPolicy QUEUE_SCHEDULER = QueueSchedulerPolicy.ES;

		public enum JobsArrivalPolicy {
			All, One, Trace, Period, JobPeriod;
		}

		public enum PredMode {
			PerfectPrediction, WrongPrediction, StaticPrediction, GoodPrediction
		};

		public static PredMode PRED_MODE = PredMode.PerfectPrediction;

		public static JobsArrivalPolicy JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.Trace;

		public static boolean GEN_JOB_ARRIVAL = false;

		public static int NUM_MACHINES = 1; // TODO: NUM_MACHINES > 1 may
		// results in
		// low utilization this simulation.
		public final static int NUM_DIMENSIONS = 3; // CPU, GPU ,MEM

		public static double MACHINE_MAX_CPU;
		public static double CPU_PER_NODE=32;
		public static double MACHINE_MAX_GPU;
		public static double GPU_PER_NODE=1;
		public static double MACHINE_MAX_MEM;
		public static double MEM_PER_NODE=64;
		// public static int DagIdStart, DagIdEnd;

		public static Method METHOD = Method.ES;
		public static double DRFW_weight = 4.0;
		public static double STRICT_WEIGHT = (Double.MAX_VALUE / 100.0);

		public static int TOLERANT_ERROR = 1; // 10^(-TOLERANT_ERROR)

		public static boolean ADJUST_FUNGIBLE = false;
		public static double ZERO = 0.001;

		public static double SIM_END_TIME = 7.0;

		public static int NUM_OPT = 0, NUM_PES = 0;

		public static int MAX_NUM_TASKS_DAG = 10000;

		public static boolean TETRIS_UNIVERSAL = false;

		/**
		 * these variables control the sensitivity of the simulator to various
		 * factors
		 */
		// between 0.0 and 1.0; 0.0 it means jobs are not pessimistic at all
		public static double LEVEL_OF_OPTIMISM = 0.0;

		public static boolean COMPUTE_STATISTICS = false;
		// public static double ERROR = 0.0;
		public static boolean IS_GEN = true;

		/**
		 * these variables will be set by the static constructor based on runmode
		 */
		public static String DataFolder = "input";
		public static String outputFolder = "output";
		public static String FileInput = "dags-input-simple.txt";
		public static String QueueInput = "queue_input.txt";
		public static String FileOutput = "dags-output.txt";

		public static String PathToInputFile = DataFolder + "/" + FileInput;
		public static String PathToQueueInputFile;
		public static String PathToOutputFile = "";
		public static String PathToResourceLog = "";

		public static String User1Input = DataFolder + "/" + FileInput;
		public static String User2Input = DataFolder + "/" + FileInput;

		public static int numJobs = 200;
		public static int numQueues = 1;
		public static int numbatchTask = 10000;

		public static double STEP_TIME = 1;

		public static boolean ENABLE_PREEMPTION = false;

		public static int LARGE_JOB_TASK_NUM_THRESHOLD = 0;

		public static double SCALE_BURSTY_DURATION = 1.0;
		public static double SCALE_BATCH_DURATION = 1.0;

		public static double MEMORY_SCALE_DOWN = 100;

		public static int JOB_NUM_PER_QUEUE_CHANGE;

		public static int USER1_MAX_Q_NUM;
		public static int USER2_MAX_Q_NUM;
		public static int USER1_START_IDX = 0;
		public static int USER2_START_IDX = 100000;
		public static int[] user2_q_nums = null;

		public static double CAPACITY_GPU = 1.0;
		public static double CAPACITY_CPU = 1.0;
		public static double CAPACITY_MEM = 1.0;

		public static int PERIODIC_INTERVAL = 100;

		public static String EXTRA = "";

		public static void setupParameters() {
			if (Globals.runmode.equals(Runmode.SmallScale)) {
				CPU_PER_NODE = 20;
			}
			Globals.MACHINE_MAX_CPU = Globals.MACHINE_MAX_GPU*CPU_PER_NODE;
			
			Globals.MACHINE_MAX_MEM = Globals.MACHINE_MAX_GPU*2*MEM_PER_NODE;
			
			COMPUTE_STATISTICS = false;

			SCALE_BURSTY_DURATION = 1.0;
			SCALE_BATCH_DURATION = 1.0;

			SCALE_UP_BATCH_JOB = 1;
			AVG_TASK_DURATION = -1.0;

			Globals.SMALL_JOB_DUR_THRESHOLD = 50.0;
			Globals.SMALL_JOB_TASK_NUM_THRESHOLD = 100;

			switch (workload) {
			case BB:
				Globals.WORKLOAD_AVG_TASK_DURATION = 7.796763659404396;
				Globals.TRACE_FILE = "workload/queries_bb_FB_distr.txt"; // BigBench
				break;
			case Google:
				Globals.TRACE_FILE = "input/job_google.txt"; // BigBench
				break;
			case Google_2:
				Globals.TRACE_FILE = "input/job_google_2.txt"; // BigBench
				break;
			case Tensorflow:
				Globals.TRACE_FILE = "input/tf_8_p100.txt";
				break;
			case TPC_DS:
				Globals.WORKLOAD_AVG_TASK_DURATION = 31.60574050691386;
				Globals.TRACE_FILE = "workload/queries_tpcds_FB_distr_new.txt"; // TPC-DS
				break;
			case TPC_H:
				Globals.WORKLOAD_AVG_TASK_DURATION = 39.5366249014282;
				Globals.TRACE_FILE = "workload/queries_tpch_FB_distr.txt"; // TPC-H -->
				break;
			case SIMPLE:
				Globals.TRACE_FILE = "input/simple.txt";
				break;
			case SIMPLE_GOOD:
				Globals.TRACE_FILE = "workload/simple_good.txt";
				double[] goodBetas = { 10, 0.1, 1 };
				Globals.jobData.reportBETAs = goodBetas;
				break;
			case SIMPLE_BAD:
				Globals.TRACE_FILE = "workload/simple_bad.txt";
				double[] badBetas = { 10, 0.1, 1 };
				Globals.jobData.reportBETAs = badBetas;
				break;
			default:
				Globals.numJobs = 30;
			}

			Globals.CAPACITY_CPU = Globals.MACHINE_MAX_CPU * Globals.NUM_MACHINES;
			Globals.CAPACITY_GPU = Globals.MACHINE_MAX_GPU * Globals.NUM_MACHINES;
			Globals.CAPACITY_MEM = Globals.MACHINE_MAX_MEM * Globals.NUM_MACHINES;
			
			/*double scaleUp = (double) (Globals.NUM_MACHINES * Globals.MACHINE_MAX_RESOURCE)
					/ (double) Globals.TRACE_CLUSTER_SIZE;*/

			// Globals.SCALE_UP_BATCH_JOB = Math.floor((double) 1 * scaleUp);
		}
	}

	public static void runSimulationScenario(boolean genInputOnly) {
		Globals.SESSION_DATA = new SessionData();

		long tStart = System.currentTimeMillis();
		String extraName = "";
		String extra = Globals.SCALE_UP_FACTOR > 1 ? "_" + Globals.SCALE_UP_FACTOR + "x" : "";

		extraName = "_" + Globals.numQueues + '_' + (int) Globals.MACHINE_MAX_GPU + Globals.EXTRA;

//		Globals.DataFolder = "input_gen";
//		Globals.FileInput = "jobs_input_" + Globals.numQueues + '_' + (int) Globals.MACHINE_MAX_GPU + '_'
//				+ Globals.workload + extra + Globals.EXTRA + ".txt";
//		Globals.QueueInput = "queue_input_" + Globals.numQueues + '_' + (int) Globals.MACHINE_MAX_GPU + '_'
//				+ Globals.workload + Globals.EXTRA + ".txt";
		
		Globals.FileInput = "jobs_input_" + Globals.numQueues + '_' 
				+ Globals.workload + extra + Globals.EXTRA + ".txt";
		Globals.QueueInput = "queue_input_" + Globals.numQueues+ '_'
				+ Globals.workload + Globals.EXTRA + ".txt";

		if (Globals.METHOD.equals(Method.DRF)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.FileOutput = "DRF-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.DRFExt)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRFExt;
			Globals.FileOutput = "DRFExt-output" + extraName + ".csv";
		}  else if (Globals.METHOD.equals(Method.FDRF)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.FileOutput = "FDRF-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.ES)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.ES;
			Globals.FileOutput = "ES-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.MaxMinMem)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.MaxMinMem;
			Globals.FileOutput = "MaxMinMem-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.SpeedUp)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.SpeedUp;
			Globals.FileOutput = "SpeedUp-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.Pricing)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.Pricing;
			Globals.FileOutput = "Pricing-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.AlloX)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.AlloX;
			Globals.FileOutput = "AlloX-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.SRPT)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.SRPT;
			Globals.FileOutput = "SRPT-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.FS)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.FS;
			Globals.FileOutput = "FS-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.SJF)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.SJF;
			Globals.FileOutput = "SJF-output" + extraName + ".csv";
		}else {
			System.err.println("[Main] Error! test case");
			return;
		}

		Globals.PathToInputFile = Globals.DataFolder + "/" + Globals.FileInput;
		Globals.PathToQueueInputFile = Globals.DataFolder + "/" + Globals.QueueInput;
		Globals.PathToOutputFile = Globals.outputFolder + "/" + Globals.FileOutput;
		Globals.PathToResourceLog = "log" + "/" + Globals.FileOutput;

		if (Globals.IS_GEN) {
			if (Globals.USE_TRACE) {
				Queue<BaseJob> tracedJobs = GenInput.readWorkloadTrace(Globals.TRACE_FILE);
				GenInput.genInputFromWorkload(Globals.numQueues, Globals.numJobs, tracedJobs);
			} else
				GenInput.genInput(Globals.numQueues, Globals.numJobs);
		}

		// print ALL parameters for the record
		System.out.println("=====================");
		System.out.println("Simulation Parameters");
		System.out.println("=====================");
		System.out.println("Runmode             = " + Globals.runmode);
		System.out.println("METHOD              = " + Globals.METHOD);
		System.out.println("Cluster Size        = " + Globals.NUM_MACHINES);
		System.out.println("Server Capacity     = (" + Globals.MACHINE_MAX_CPU +" cpus,"+ Globals.MACHINE_MAX_GPU+" gpus,"+ Globals.MACHINE_MAX_MEM +" Gi)");
		System.out.println("Workload            = " + Globals.TRACE_FILE);
		System.out.println("numJobs        = " + Globals.numJobs);
		System.out.println("PathToInputFile     = " + Globals.PathToInputFile);
		System.out.println("PathToQueueInputFile= " + Globals.PathToQueueInputFile);
		System.out.println("PathToResourceLog   = " + Globals.PathToResourceLog);
		System.out.println("PathToOutputFile    = " + Globals.PathToOutputFile);
		System.out.println("SIMULATION_END_TIME = " + Globals.SIM_END_TIME);
		System.out.println("STEP_TIME           = " + Globals.STEP_TIME);
		System.out.println("METHOD              = " + Globals.METHOD);
		System.out.println("QUEUE_SCHEDULER     = " + Globals.QUEUE_SCHEDULER);
		System.out.println("=====================\n");

		if (genInputOnly)
			return;

		System.out.println("Start simulation ...");
		System.out.println("Please wait ...");
		Simulator simulator = new Simulator();
		simulator.simulateMultiQueues();
		System.out.println("\nEnd simulation ...");
		long duration = System.currentTimeMillis() - tStart;
		System.out.print("========== " + (duration / (1000)) + " seconds ==========\n");
	}

	public static void freeMemory() {
		Object obj = new Object();
		WeakReference<Object> ref = new WeakReference<Object>(obj);
		obj = null;
		// wait for GC to run
		while (ref.get() != null) {
			System.gc();
		}
	}

	public static Globals.WorkLoadType workloadMenu() {
		Globals.WorkLoadType workload = null;
		String[] workloads = { "BB", "TPC-DS", "TPC-H" };
		System.out.println("Please select the workload type from: ");
		showMenu(workloads);
		System.out.print("Enter number: ");

		Scanner scanner = new Scanner(System.in);
		int choice = scanner.nextInt();

		switch (choice) {
		case 1:
			workload = WorkLoadType.BB;
			break;
		case 2:
			workload = WorkLoadType.TPC_DS;
			break;
		case 3:
			workload = WorkLoadType.TPC_H;
			break;
		default:
			//
		}
		System.out.println("You selected workload : " + workload);
		scanner.close();
		return workload;
	}

	public static Globals.Runmode runmodeMenu() {
		Globals.Runmode runmode = null;

		String[] workloads = { "MultipleBatchQueueRun" };
		System.out.println("Please select the experiment from: ");
		showMenu(workloads);
		System.out.print("Enter number: ");

		Scanner scanner = new Scanner(System.in);
		int choice = scanner.nextInt();

		switch (choice) {
		case 1:
			break;
		default:
			//
		}
		System.out.println("You selected experiment : " + runmode);
		scanner.close();
		return runmode;
	}

	public static void showMenu(String[] menuItems) {
		for (int i = 0; i < menuItems.length; i++) {
			System.out.println("\t" + (i + 1) + ": " + menuItems[i]);
		}
	}

	public static void main(String[] args) {
		
		Globals.MACHINE_MAX_CPU = Globals.MACHINE_MAX_GPU*32;
		if (Globals.EnableMatlab)
			try {
				Globals.MATLAB = MatlabEngine.startMatlab();
			} catch (Exception e) {
				System.out.println("[Error] Matlab is not supported.");
			}
		
		Globals.runmode = Runmode.SmallScale;

		Utils.createUserDir("log");
		Utils.createUserDir("output");
		Utils.createUserDir("input_gen");

		System.out.println("Started Simulation....");
		System.out.println("........" + now() + ".....");

//		Globals.workload = Globals.WorkLoadType.SIMPLE;
		Globals.ENABLE_CPU_CMPT_ERROR = false;
		if (Globals.runmode.equals(Runmode.MultipleRuns)) {			
			Globals.JOB_SCHEDULER = JobScheduling.SJF; 
			Globals.IS_GEN= true;
			Globals.USE_TRACE=true;
			Globals.workload = WorkLoadType.Google;
			Globals.jobData = new JobData();
			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 10000.0;
			double errStd = 0.1;
			Globals.jobData.errs = Randomness.getNormalDistribution(Globals.NumRandomSamples , 0, errStd, -1, 1);
//			Globals.Method[] methods = { Method.DRF, Method.DRFExt, Method.ES, Method.AlloX, Method.SJF, Method.FS};
			Globals.Method[] methods = { Method.DRF, Method.DRFExt, Method.ES, Method.AlloX, Method.SJF, Method.SRPT};
//			Globals.Method[] methods = {Method.FS };
//			Globals.Method[] methods = {Method.SRPT };
//			Globals.Method[] methods = {Method.DRF };
//			Globals.Method[] methods = {Method.DRF, Method.ES };
//			Globals.Method[] methods = {Method.DRFExt};
//			Globals.Method[] methods = {Method.ES, Method.AlloX};
			Globals.MACHINE_MAX_GPU = 10;
			Globals.workload = Globals.WorkLoadType.Google;
			Globals.numQueues = 15;
			Globals.numJobs = Globals.numQueues*60;

			for (Globals.Method method : methods) {
				Globals.METHOD = method;
				Globals.setupParameters();
				runSimulationScenario(false);
				Globals.IS_GEN = false;
				System.out.println();
				
			}
		} else if (Globals.runmode.equals(Runmode.SmallScale)) {
			Globals.JOB_SCHEDULER = JobScheduling.SJF; 
			Globals.IS_GEN= true;
			Globals.USE_TRACE=true;
//			Globals.workload = WorkLoadType.Google_2;
			Globals.alpha = 0.5;
			Globals.workload = WorkLoadType.Tensorflow;
			Globals.jobData = new JobData();
			double errStd = 0.0;
			Globals.jobData.errs = Randomness.getNormalDistribution(Globals.NumRandomSamples , 0, errStd, -1, 1);
			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 10000.0;
//			Globals.Method[] methods = { Method.DRF, Method.ES, Method.DRFExt,  Method.AlloX, Method.SJF, Method.FS, Method.SRPT};
			Globals.Method[] methods = {Method.FS};
			Globals.MACHINE_MAX_GPU = 2;
			Globals.numQueues = 2;
			Globals.numJobs = Globals.numQueues*4;

			for (Globals.Method method : methods) {
				Globals.METHOD = method;
				Globals.setupParameters();
				runSimulationScenario(false);
				Globals.IS_GEN = false;
				System.out.println();
			}
		} else if (Globals.runmode.equals(Runmode.Analysis_capacity)) {
			double caps[] = {50, 75, 100, 125, 150};
			Globals.JOB_SCHEDULER = JobScheduling.SJF; 
			Globals.jobData = new JobData();
			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 10000.0;
			Globals.Method[] methods = { Method.DRF, Method.DRFExt, Method.ES, Method.AlloX, Method.SJF};
			Globals.workload = Globals.WorkLoadType.Google;
			Globals.numQueues = 25;
			Globals.numJobs = Globals.numQueues*1000;
			Globals.USE_TRACE=true;
			for (double cap : caps){
				Globals.MACHINE_MAX_GPU = cap;
				for (Globals.Method method : methods) {
					Globals.METHOD = method;
					Globals.setupParameters();
					runSimulationScenario(false);
					System.out.println();
				}
			}
		} else if (Globals.runmode.equals(Runmode.Analysis_misest)) {
			// TODO: not done yet
			double misEst[] = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5};
//			double misEst[] = {0.1, 0.5};
			Globals.jobData = new JobData();
			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 2000.0;
			Globals.Method[] methods = { Method.AlloX };
			Globals.MACHINE_MAX_GPU = 100;
			
			Globals.workload = Globals.WorkLoadType.Google;
			Globals.numQueues = 25;
			Globals.numJobs = Globals.numQueues*1000;
			Globals.USE_TRACE=true;
			for (double errStd : misEst){
				// generate beta errors:
				Globals.jobData.errs = Randomness.getNormalDistribution(Globals.NumRandomSamples , 0, errStd, -1, 1);
				Globals.EXTRA = "_e"+errStd;
				for (Globals.Method method : methods) {
					Globals.METHOD = method;
					Globals.setupParameters();
					runSimulationScenario(false);
					Globals.IS_GEN = false;
					System.out.println();
				}
				Globals.IS_GEN = true;
			}
		} else if (Globals.runmode.equals(Runmode.Analysis_speedup)) {
			Globals.USE_TRACE=true;
			
			double scales[] = {0.1, 0.5, 1, 2, 4, 8, 16, 32};
			Globals.jobData = new JobData();
			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 10000.0;
			Globals.Method[] methods = { Method.DRF, Method.ES, Method.AlloX };
//			Globals.Method[] methods = { Method.ES, Method.AlloX };
//			Globals.Method[] methods = { Method.ES };
			Globals.MACHINE_MAX_GPU = 100;
			Globals.workload = Globals.WorkLoadType.Google;
			Globals.numQueues = 25;
			Globals.numJobs = Globals.numQueues*300;
			
			for (double speedUp : scales){
				Globals.transferRateScale =  speedUp;
				// generate beta errors:
				Globals.EXTRA = "_s"+speedUp;
				for (Globals.Method method : methods) {
					Globals.METHOD = method;
					Globals.setupParameters();
					runSimulationScenario(false);
					Globals.IS_GEN = false;
					System.out.println();
				}
				Globals.IS_GEN = true;
			}
		}  else if (Globals.runmode.equals(Runmode.Analysis_Alpha)) {
			Globals.USE_TRACE=true;
			double alphas[] = {0.1, 0.2, 0.4, 0.6, 0.8, 1.0};
//			double speedUps[] = {0.1, 0.5, 1};
			// scale the speedUp
//			double speedUps[] = {32};			
			Globals.jobData = new JobData();
			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 3000.0;
//			Globals.Method[] methods = { Method.DRF, Method.ES, Method.AlloX };
			Globals.Method[] methods = { Method.AlloX };
//			Globals.Method[] methods = { Method.ES };
			Globals.MACHINE_MAX_GPU = 100;
			Globals.workload = Globals.WorkLoadType.Google;
			Globals.numQueues = 25;
			Globals.numJobs = Globals.numQueues*100;
			
			for (double alpha : alphas){
//				Globals.speedUpScale =  speedUp;
				Globals.alpha = alpha;
				// generate beta errors:
				Globals.EXTRA = "_a"+alpha;
				Globals.IS_GEN = true;
				for (Globals.Method method : methods) {
					Globals.METHOD = method;
					Globals.setupParameters();
					runSimulationScenario(false);
					Globals.IS_GEN = false;
					System.out.println();
				}
			}
		} else {
			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 20.0;
			// Globals.METHOD = Method.ES;
			// Globals.METHOD = Method.MaxMinMem;
			Globals.METHOD = Method.SpeedUp;
			// Globals.METHOD = Method.Pricing;
			// Globals.METHOD = Method.DRF;
			Globals.MACHINE_MAX_CPU = 384;
			Globals.MACHINE_MAX_GPU = 12;
			Globals.MACHINE_MAX_MEM = 1152;
			Globals.workload = Globals.WorkLoadType.SIMPLE;
			Globals.IS_GEN = true;
			Globals.SCALE_UP_FACTOR = 1;
			Globals.setupParameters();
			Globals.numQueues = 3;
			Globals.numJobs = 9000;
			runSimulationScenario(false);
			System.out.println();
		}

		System.out.println("\n");
		System.out.println("........FINISHED ./.");
		System.out.println("........" + now() + ".....");
		
		endEndSimulation();
	}
	
	public static void endEndSimulation(){
		if (Globals.EnableMatlab)
			try {
				Globals.MATLAB.close();
			} catch (Exception e) {
				System.out.println("[Error] Matlab is not supported.");
			}
		freeMemory();
	}

	public static String now() {
		Calendar cal = Calendar.getInstance();
		return cal.getTime().toString();
	}
}