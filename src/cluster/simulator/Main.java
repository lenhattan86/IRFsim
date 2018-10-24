package cluster.simulator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Logger;

//import com.mathworks.engine.MatlabEngine;

import cluster.data.JobData;
import cluster.data.SessionData;
import cluster.datastructures.BaseJob;
import cluster.simulator.Main.Globals;
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
		public static long  SIM_START = 0;
		public final static boolean EnableMatlab = false;
		public static boolean EnableProfiling = true;
		public static boolean EnableGroupProfiling = true; // jobs are ready when all profiling jobs arriving at the same time finish.
		public final static boolean EnablePreemption = true;
		public static boolean EnableIncreaseAlpha = true;
		public static int MAX_N_JOB_AlloX = 30;
		public static int CPU_PROFILING_JOB1 = -100000; //-10000 -> 0
		public static int CPU_PROFILING_JOB2 = CPU_PROFILING_JOB1*2;
		public static int GPU_PROFILING_JOB1 = CPU_PROFILING_JOB1*3;
		public static int GPU_PROFILING_JOB2 = CPU_PROFILING_JOB1*4;
		public static double cpu_prof_1 = 0.01;
		public static double cpu_prof_2 = 0.02;
		public static double gpu_prof_1 = 0.01;
		public static double gpu_prof_2 = 0.02;
		
		public static JobScheduling JOB_SCHEDULER = JobScheduling.SJF; 
		
//		public static MatlabEngine MATLAB = null;
		
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
			Google, BB, TPC_DS, TPC_H, SIMPLE, SIMPLE_GOOD, SIMPLE_BAD, Google_2, Tensorflow, MayBeGood, Experiment
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
		
		public static Runmode getRunMode(String runMode) {
			if (runMode.equals("MultipleRuns")){
				return Runmode.MultipleRuns;
			} else if (runMode.equals("Analysis_Alpha")) {
				return Runmode.Analysis_Alpha;
			} else if (runMode.equals("Analysis_misest")) {
				return Runmode.Analysis_misest;
			} else if (runMode.equals("Analysis_capacity")) {
				return Runmode.Analysis_capacity;
			} else if (runMode.equals("Analysis_overhead")) {
				return Runmode.Analysis_overhead;
			} else if (runMode.equals("Experiment")) {
				return Runmode.Experiment;
			}
			return null;
		}

		public enum Runmode {
			MultipleRuns, BetaErrors, Analysis_Alpha, Analysis_misest, Analysis_overhead, Analysis_capacity, SmallScale, Experiment, NONE
		}

		public static boolean USE_TRACE = false;
		
		public static double alpha = 0.05; // ratio of user set to be scheduled.

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
			DRFFIFO, DRF, DRFExt, FDRF, DRFW, ES, MaxMinMem, SpeedUp, Pricing, SJF, AlloX, SRPT, AlloXopt
		}
		
		public static int PERIOD_FS = 1; 
		
		public static enum DemandChangeType {
			Running, Arrival, Total
		}
		
		public static enum ChangeDetectionMethod {
			Periodic, Adaptive, GLR  
		}

		public static enum QueueSchedulerPolicy {
			DRF, DRFExt, ES, MaxMinMem, SpeedUp, Pricing, AlloX, SJF, FS, SRPT, AlloXopt
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
		public static double GPU_MEM_MAX = 16;
		public static double MACHINE_MAX_GPU;
		public static double CPU_TO_GPU_RATIO = 1;
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

		public static int[] numJobs = null;
		public static int numSmallJobs = 4000; 
		public static int numLargeJobs = 100;
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
		
		public static boolean EnableProfiling(){
			return Globals.EnableProfiling && (!Globals.METHOD.equals(Globals.Method.DRFFIFO));
		}

		public static void setupParameters() {
			
			Globals.MACHINE_MAX_CPU = Globals.MACHINE_MAX_GPU*CPU_PER_NODE*CPU_TO_GPU_RATIO;
			
			Globals.MACHINE_MAX_MEM = (Globals.MACHINE_MAX_GPU + Globals.MACHINE_MAX_GPU*CPU_TO_GPU_RATIO)  *MEM_PER_NODE;
			
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
				Globals.TRACE_FILE = "input/job_google.txt";
				break;
			case MayBeGood:
				Globals.TRACE_FILE = "input/job_google_10_20_d1020.txt"; 
				break;
			case Experiment:
				Globals.TRACE_FILE = "input/experiment_large_23p_verify.txt";
//				Globals.TRACE_FILE = "input/experiment_large_23p_drffifo.txt";
//				Globals.TRACE_FILE = "input/experiment_large_23p_drfext.txt";
//				Globals.TRACE_FILE = "input/experiment_large_23p_es.txt";
//				Globals.TRACE_FILE = "input/experiment_large_23p_allox.txt";
//				Globals.TRACE_FILE = "input/experiment_large_23p_drf.txt";
				break;
			case Google_2:
				Globals.TRACE_FILE = "input/job_google_2.txt"; 
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

		Globals.SIM_START = System.currentTimeMillis();
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
		
		Globals.JOB_SCHEDULER = JobScheduling.SJF;
//		Globals.EnableProfiling = true;
		if (Globals.METHOD.equals(Method.DRF)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.FileOutput = "DRF-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.DRFFIFO)) {
			Globals.JOB_SCHEDULER = JobScheduling.FIFO;
//			Globals.EnableProfiling = false;
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.FileOutput = "DRFFIFO-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.DRFExt)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRFExt;
			Globals.FileOutput = "DRFExt-output" + extraName + ".csv";
		}  else if (Globals.METHOD.equals(Method.ES)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.ES;
			Globals.FileOutput = "ES-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.AlloX)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.AlloX;
			Globals.FileOutput = "AlloX-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.AlloXopt)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.AlloXopt;
			Globals.FileOutput = "AlloXopt-output" + extraName + ".csv";
		} else if (Globals.METHOD.equals(Method.SRPT)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.SRPT;
			Globals.FileOutput = "SRPT-output" + extraName + ".csv";
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
			} 
//			else
//				GenInput.genInput(Globals.numQueues, Globals.numJobs);
		}

		// print ALL parameters for the record
		System.out.println("=====================");
		System.out.println("Simulation Parameters");
		System.out.println("=====================");
		System.out.println("Runmode             = " + Globals.runmode);
		System.out.println("METHOD              = " + Globals.METHOD);
		System.out.println("alpha               = " + Globals.alpha);
		System.out.println("Cluster Size        = " + Globals.NUM_MACHINES);
		System.out.println("Server Capacity     = (" + Globals.MACHINE_MAX_CPU +" cpus,"+ Globals.MACHINE_MAX_GPU+" gpus,"+ Globals.MACHINE_MAX_MEM +" Gi)");
		System.out.println("Workload            = " + Globals.TRACE_FILE);
//		System.out.println("numJobs        = " + Globals.numJobs);
		System.out.println("numSmallJobs        = " + Globals.numSmallJobs);
		System.out.println("numLargeJobs        = " + Globals.numLargeJobs);
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
		long duration = System.currentTimeMillis() - Globals.SIM_START;
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
//		if (Globals.EnableMatlab)
//			try {
//				Globals.MATLAB = MatlabEngine.startMatlab();
//			} catch (Exception e) {
//				System.out.println("[Error] Matlab is not supported.");
//			}

		Utils.createUserDir("log");
		Utils.createUserDir("output");
		Utils.createUserDir("input_gen");

		System.out.println("Started Simulation....");
		System.out.println("........" + now() + ".....");

		Globals.runmode = Runmode.MultipleRuns;
		Globals.ENABLE_CPU_CMPT_ERROR = false;
		Globals.EXTRA = "_debug";
		
		if (args.length > 0){
			String runmode = args[0];
			Globals.runmode = Globals.getRunMode(runmode);
			if (Globals.runmode==null){ 
				System.err.println("This runmode is not available " + runmode);
				ArrayList<Globals.Runmode> modes = new ArrayList<Globals.Runmode>();
				modes.add(Runmode.Analysis_Alpha);
				modes.add(Runmode.MultipleRuns);
				modes.add(Runmode.Analysis_capacity);
				modes.add(Runmode.Analysis_misest);
				modes.add(Runmode.Analysis_overhead);
				modes.add(Runmode.Experiment);
				System.out.println(modes);
				return;
			} else {
				Globals.EXTRA = "";
			}
			if (args.length > 1){
				System.out.println("============"+args[1]+"==========");
				Globals.EXTRA = "_" + args[1];
			}
		}
		if (Globals.EXTRA.contains("debug")){
			System.out.print("============[Debug mode]==========");
		}
		
		//////////////////// COMMON SETTINGS ////////////////////
		Globals.IS_GEN= true;
		Globals.USE_TRACE=true;
		
//		Globals.MACHINE_MAX_GPU = 100; Globals.numQueues = 20; Globals.numJobs = Globals.numQueues*100;Globals.alpha = 0.05;
//		Globals.MACHINE_MAX_GPU = 20; Globals.numQueues = 10; Globals.numJobs = Globals.numQueues*1000;Globals.alpha = 0.1;
//		Globals.MACHINE_MAX_GPU = 20; Globals.numQueues = 5; Globals.numJobs = Globals.numQueues*5000;Globals.alpha = 0.2;
		// fairness:
		Globals.MACHINE_MAX_GPU = 20; Globals.numQueues = 10; Globals.alpha = 0.1;
		
		Globals.numSmallJobs = 1000; Globals.numLargeJobs = 1000;
//		Globals.numJobs = new int[]{Globals.numSmallJobs, Globals.numSmallJobs, Globals.numSmallJobs, Globals.numSmallJobs, Globals.numSmallJobs, 
//				Globals.numLargeJobs, Globals.numLargeJobs, Globals.numLargeJobs, Globals.numLargeJobs, Globals.numLargeJobs};
//		Globals.numSmallJobs = 4000; Globals.numLargeJobs = 100;
		Globals.numJobs = new int[]{Globals.numSmallJobs, Globals.numSmallJobs, Globals.numSmallJobs, Globals.numSmallJobs, Globals.numSmallJobs, 
			Globals.numSmallJobs, Globals.numSmallJobs,  Globals.numSmallJobs,  Globals.numSmallJobs,  Globals.numLargeJobs};
		
//		Globals.workload = WorkLoadType.Google;
		Globals.workload = WorkLoadType.MayBeGood;
		
		double errStd = 0.1;
		Globals.jobData = new JobData();
		Globals.jobData.cpuErrs = Randomness.scaleErr(Globals.jobData.cpuErrs, errStd, -0.99, 0.99);
		Globals.jobData.gpuErrs = Randomness.scaleErr(Globals.jobData.gpuErrs, errStd, -0.99, 0.99);
		
		Globals.MEMORY_SCALE_DOWN = 1;
		Globals.NUM_MACHINES = 1;
		Globals.SIM_END_TIME = 50000.0;
		
		if (Globals.runmode.equals(Runmode.MultipleRuns)) {			
//			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES,Method.DRFExt,Method.SRPT, Method.AlloX};
//			Globals.Method[] methods = {Method.ES,Method.SRPT, Method.DRFFIFO, Method.DRF, Method.DRFExt, Method.AlloX, Method.AlloXopt};
//			Globals.Method[] methods = {Method.ES,Method.SRPT, Method.DRFFIFO, Method.DRF, Method.DRFExt, Method.AlloXopt};
			Globals.Method[] methods = {Method.AlloX};
//			Globals.Method[] methods = {Method.AlloXopt};
//			Globals.Method[] methods = {Method.ES};
//			Globals.Method[] methods = {Method.ES};
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

			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 10000.0;
//			Globals.Method[] methods = { Method.DRF, Method.ES, Method.DRFExt,  Method.SJF, Method.FS, Method.SRPT};
			Globals.Method[] methods = {Method.AlloX};
			Globals.MACHINE_MAX_GPU = 2;
			Globals.CPU_PER_NODE = 20;
			Globals.numQueues = 2;
			Globals.numJobs = new int[]{4,4,4,4};
			double errStdTemp = 0.0;
//			Globals.jobData.cpuErrs = Randomness.getNormalDistribution(Globals.numJobs, 0, errStdTemp, -0.99, 0.99);
//			Globals.jobData.gpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, errStdTemp, -0.99, 0.99);

			for (Globals.Method method : methods) {
				Globals.METHOD = method;
				Globals.setupParameters();
				runSimulationScenario(false);
				Globals.IS_GEN = false;
				System.out.println();
			}
		}  else if (Globals.runmode.equals(Runmode.Analysis_Alpha)) {
			double alphas[] = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
//			double alphas[] = {0.05, 0.2, 0.4, 0.6, 0.8, 1.0};
//			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES, Method.DRFExt, Method.SRPT, Method.AlloX};
//			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES, Method.DRFExt, Method.SRPT};
			Globals.Method[] methods = {Method.AlloX};			
			
//			Globals.jobData.cpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, 0, -0.99, 0.99);
//			Globals.jobData.gpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, 0, -0.99, 0.99);
			
			for (double alpha : alphas){
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
		} else if (Globals.runmode.equals(Runmode.Analysis_capacity)) {
			int caps[] = {10, 11, 12, 13, 14, 15, 20, 25, 30};
			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES, Method.DRFExt, Method.SRPT, Method.AlloX};
//			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES, Method.DRFExt, Method.SRPT};
//			Globals.Method[] methods = {Method.AlloX};
			
//			Globals.jobData.cpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, 0, -0.99, 0.99);
//			Globals.jobData.gpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, 0, -0.99, 0.99);
			
			for (int i=caps.length-1; i>=0; i--){
				int cap = caps[i];
				Globals.MACHINE_MAX_GPU = cap;
				Globals.EXTRA = "_c"+cap;
				Globals.IS_GEN= true;
				for (Globals.Method method : methods) {
					Globals.METHOD = method;
					Globals.setupParameters();
					runSimulationScenario(false);
					System.out.println();
					Globals.IS_GEN= false;
				}
			}
		} else if (Globals.runmode.equals(Runmode.Analysis_misest)) {
			// TODO: not done yet
			double misEst[] = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6};
			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES, Method.DRFExt, Method.SRPT, Method.AlloX};
			Globals.jobData = new JobData();
//			double[] cpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, 0.1, -0.99, 0.99);
//			double[] gpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, 0.1, -0.99, 0.99);
			double[] cpuErrs = Globals.jobData.cpuErrs;
			double[] gpuErrs = Globals.jobData.gpuErrs;
			for (double e : misEst){
				Globals.IS_GEN = true;
				// generate beta errors:
				Globals.jobData.cpuErrs = Randomness.scaleErr(cpuErrs, e, -0.99, 0.99);
				Globals.jobData.gpuErrs = Randomness.scaleErr(gpuErrs, e, -0.99, 0.99);
				Globals.EXTRA = "_e"+e;
				for (Globals.Method method : methods) {
					Globals.METHOD = method;
					Globals.setupParameters();
					runSimulationScenario(false);
					Globals.IS_GEN = false;
					System.out.println();
				}
			}
		} else if (Globals.runmode.equals(Runmode.Analysis_overhead)) {
			// TODO: not done yet
//			errStd = 0.1;
//			Globals.jobData = new JobData();
//			Globals.jobData.cpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, errStd, -0.99, 0.99);
//			Globals.jobData.gpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, errStd, -0.99, 0.99);
//			Globals.jobData.cpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, 0, -0.99, 0.99);
//			Globals.jobData.gpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, 0, -0.99, 0.99);
			
			double overheads[] = {0, 0.01, 0.02, 0.03, 0.04, 0.05};
			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES, Method.DRFExt, Method.SRPT, Method.AlloX};
//			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES, Method.DRFExt, Method.SRPT};
//			Globals.Method[] methods = {Method.AlloX};
			
			for (double overhead : overheads){
				Globals.IS_GEN= true;
				Globals.EnableProfiling = true;
				if (overhead > 0) {
					Globals.cpu_prof_1 = overhead;
					Globals.cpu_prof_2 = overhead*2;
					Globals.gpu_prof_1 = overhead;
					Globals.gpu_prof_2 = overhead*2;
					Globals.EXTRA = "_o"+overhead;
				} else {
					Globals.EnableProfiling = false;
					Globals.EXTRA = "_o0.00";
				}
				for (Globals.Method method : methods) {
					Globals.METHOD = method;
					Globals.setupParameters();
					runSimulationScenario(false);
					Globals.IS_GEN = false;
					System.out.println();
				}
			}
		}  else if (Globals.runmode.equals(Runmode.Experiment)) {			
			Globals.JOB_SCHEDULER = JobScheduling.SJF; 
			Globals.IS_GEN= false;
			Globals.USE_TRACE=true;
			Globals.alpha = 0.5;
			Globals.workload = WorkLoadType.Experiment;
			Globals.MEMORY_SCALE_DOWN = 1;
			Globals.NUM_MACHINES = 1;
			Globals.SIM_END_TIME = 20000.0;
			Globals.Method[] methods = {Method.DRFFIFO, Method.DRF, Method.ES,Method.DRFExt, Method.SRPT, Method.AlloX};
			Globals.MACHINE_MAX_GPU = 4;			
			Globals.CPU_PER_NODE = 20; 
			Globals.CPU_TO_GPU_RATIO = 2;
			Globals.numQueues = 4;
			Globals.numJobs = new int[]{10, 10, 10, 10};
			double errStdTemp = 0.0;
			Globals.jobData = new JobData();
//			Globals.jobData.cpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, errStdTemp, -1, 1);
//			Globals.jobData.gpuErrs = Randomness.getNormalDistribution(Globals.numJobs , 0, errStdTemp, -1, 1);

			for (Globals.Method method : methods) {
				Globals.METHOD = method;
				Globals.setupParameters();
				runSimulationScenario(false);
				Globals.IS_GEN = false;
				System.out.println();
			}
		} 

		System.out.println("\n");
		System.out.println("........FINISHED ./.");
		System.out.println("........" + now() + ".....");
		
		endEndSimulation();
	}
	
	public static void endEndSimulation(){
//		if (Globals.EnableMatlab)
//			try {
//				Globals.MATLAB.close();
//			} catch (Exception e) {
//				System.out.println("[Error] Matlab is not supported.");
//			}
		freeMemory();
	}

	public static String now() {
		Calendar cal = Calendar.getInstance();
		return cal.getTime().toString();
	}
}