package cluster.simulator;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Logger;

import cluster.data.QueueData;
import cluster.data.SessionData;
import cluster.datastructures.BaseDag;
import cluster.simulator.Main.Globals.JobsArrivalPolicy;
import cluster.simulator.Main.Globals.Method;
import cluster.simulator.Main.Globals.PredMode;
import cluster.simulator.Main.Globals.Runmode;
import cluster.simulator.Main.Globals.SetupMode;
import cluster.simulator.Main.Globals.WorkLoadType;
import cluster.utils.GenInput;
import cluster.utils.Utils;

public class Main {

  private static Logger LOG = Logger.getLogger(Main.class.getName());

  public static class Globals {

    public static WorkLoadType workload = WorkLoadType.BB;

    public static String TRACE_FILE = "workload/queries_bb_FB_distr.txt"; // BigBench

    public static int SMALL_JOB_TASK_NUM_THRESHOLD = 250; // for 80 for 2 BB
                                                          // TPCDS,
    // => TPC-H --> 250

    public static final int TRACE_CLUSTER_SIZE = 25;

    public enum WorkLoadType {
      BB, TPC_DS, TPC_H, SIMPLE
    };

    public enum ArrivalType {
      PERIOD, DISTRIBUTION, ALL_IN_ONE
    }

    public static ArrivalType batchArrivalType = ArrivalType.ALL_IN_ONE;
    public static ArrivalType burstyArrivalType = ArrivalType.PERIOD;

    public enum SetupMode {
      VeryShortInteractive, ShortInteractive, LongInteractive, VeryLongInteractive, others
    };

    public enum Runmode {
      SingleRun, MultipleBatchQueueRun, AvgTaskDuration, ScaleBatchTaskDuration, MultipleBurstyQueues, ScaleUpBurstyJobs, EstimationErrors, TrialRun, NONE
    }

    public static boolean USE_TRACE = true;

    public static final int TASK_ARRIVAL_RANGE = 50;

    public static final int BATCH_START_ID = 100000;

    public static final double User2QueueInterval = 200.0;

    public static SessionData SESSION_DATA = null;

    public static double DEBUG_START = 0.0;
    public static double DEBUG_END = -1.0;

    public static double SCALE_UP_BURSTY_JOB = 50;
    public static double SCALE_UP_BURSTY_JOB_DEFAULT = 50;
    public static double SCALE_UP_BATCH_JOB = 1;
    public static double AVG_TASK_DURATION = -1.0;

    public static double ESTIMASION_ERRORS = 0.0;

    public static double WORKLOAD_AVG_TASK_DURATION = -1.0; // computed
                                                            // separately from
                                                            // the traces.

    public static double SMALL_JOB_DUR_THRESHOLD = 40.0;
    public static double LARGE_JOB_MAX_DURATION = 0.0;

    public static String DIST_FILE = "dist_gen/poissrnd.csv";

    public static Runmode runmode = Runmode.MultipleBatchQueueRun;

    public static boolean DEBUG_ALL = false;
    public static boolean DEBUG_LOCAL = true;

    public static int SCALE_UP_FACTOR = 1;

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
      All, One, Trace, Period, JobPeriod;
    }

    public enum PredMode {
      PerfectPrediction, WrongPrediction, StaticPrediction, GoodPrediction
    };

    public static PredMode PRED_MODE = PredMode.PerfectPrediction;

    public static JobsArrivalPolicy BATCH_JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.Trace;
    public static JobsArrivalPolicy BURSTY_JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.Period;

    public static JobsArrivalPolicy JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.JobPeriod;

    public static boolean GEN_JOB_ARRIVAL = true;

    public static int NUM_MACHINES = 1; // TODO: NUM_MACHINES > 1 may
    // results in
    // low utilization this simulation.
    public static int NUM_DIMENSIONS = 6; // TODO: change to 6
    public static double MACHINE_MAX_RESOURCE;
    // public static int DagIdStart, DagIdEnd;

    public static Method METHOD = Method.SpeedFair;
    public static double DRFW_weight = 4.0;
    public static double STRICT_WEIGHT = (Double.MAX_VALUE / 100.0);
    // public static double STRICT_WEIGHT = 100000.00;

    public static int TOLERANT_ERROR = 1; // 10^(-TOLERANT_ERROR)

    public static boolean ADJUST_FUNGIBLE = false;
    public static double ZERO = 0.001;

    public static double SIM_END_TIME = 1000000;

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
    public static double ERROR = 0.0;
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

    public static int numBurstyQueues = 1, numBurstyJobPerQueue = 25,
        numInteractiveTask = 0;
    public static int numBatchJobs = 200;
    public static int numBatchQueues = 1;
    public static int numbatchTask = 10000;

    public static double STEP_TIME = 1.0;

    public static boolean ENABLE_PREEMPTION = false;

    public static int LARGE_JOB_TASK_NUM_THRESHOLD = 0;

    public static double SCALE_BURSTY_DURATION = 1.0;
    public static double SCALE_BATCH_DURATION = 1.0;

    public static int JOB_NUM_PER_QUEUE_CHANGE;

    public static int USER1_MAX_Q_NUM;
    public static int USER2_MAX_Q_NUM;
    public static int USER1_START_IDX = 0;
    public static int USER2_START_IDX = 100000;
    public static int[] user2_q_nums = null;

    public static double CAPACITY = 1.0;

    public static int PERIODIC_INTERVAL = 100;

    public static String EXTRA = "";

    public static void setupParameters(SetupMode setup,
        double scaleUpBursty) {
      COMPUTE_STATISTICS = false;

      SCALE_BURSTY_DURATION = 1.0;
      SCALE_BATCH_DURATION = 1.0;

      SCALE_UP_BATCH_JOB = 1;
      AVG_TASK_DURATION = -1.0;

      switch (workload) {
      case BB:
        Globals.WORKLOAD_AVG_TASK_DURATION = 7.796763659404396;
        Globals.TRACE_FILE = "workload/queries_bb_FB_distr.txt"; // BigBench
        switch (setup) {
        case VeryShortInteractive:
          Globals.numBurstyJobPerQueue = 20;
          Globals.SCALE_UP_BURSTY_JOB_DEFAULT = 12.5;
          Globals.SCALE_BURSTY_DURATION = 1 / 30.0;
          Globals.SCALE_BATCH_DURATION = 1 / 5.0;
          Globals.STEP_TIME = 0.1;
          break;
        case ShortInteractive:
          Globals.SMALL_JOB_DUR_THRESHOLD = 50.0;
          Globals.SMALL_JOB_TASK_NUM_THRESHOLD = 80;
          Globals.SCALE_UP_BURSTY_JOB_DEFAULT = 12.5;
          Globals.SCALE_BURSTY_DURATION = 1;
          // we can improve performance by reduce batch duration
          break;
        case LongInteractive:
          Globals.numBurstyJobPerQueue = 25; // the larger
          Globals.SMALL_JOB_DUR_THRESHOLD = 30.0;
          Globals.SMALL_JOB_TASK_NUM_THRESHOLD = 80;
          // Globals.LARGE_JOB_TASK_NUM_THRESHOLD = 300;
          Globals.SCALE_UP_BURSTY_JOB_DEFAULT = (int) (12.5 * scaleUpBursty);
          Globals.SCALE_BURSTY_DURATION = 1 / 2.0;
          break;
        default:
          Globals.SCALE_UP_BURSTY_JOB_DEFAULT = 50;
          Globals.SCALE_BURSTY_DURATION = 1 / 3.0;
          Globals.SMALL_JOB_TASK_NUM_THRESHOLD = 80;
          // Globals.LARGE_JOB_TASK_NUM_THRESHOLD = 300;
        }
        break;
      case TPC_DS:
        Globals.WORKLOAD_AVG_TASK_DURATION = 31.60574050691386;
        Globals.TRACE_FILE = "workload/queries_tpcds_FB_distr_new.txt"; // TPC-DS
        switch (setup) {
        case ShortInteractive:
          Globals.SCALE_UP_BURSTY_JOB_DEFAULT = 20;
          Globals.SMALL_JOB_DUR_THRESHOLD = 30.0;
          Globals.SMALL_JOB_TASK_NUM_THRESHOLD = 50;
          // Globals.SCALE_BURSTY_DURATION = 1 / 2.0;
          break;
        default:
          Globals.SCALE_UP_BATCH_JOB = 1;
        }
        break;
      case TPC_H:
        Globals.WORKLOAD_AVG_TASK_DURATION = 39.5366249014282;
        Globals.TRACE_FILE = "workload/queries_tpch_FB_distr.txt"; // TPC-H -->
        switch (setup) {
        case ShortInteractive:
          Globals.numBurstyJobPerQueue = 25;
//          Globals.SCALE_UP_BURSTY_JOB_DEFAULT = 1.4; // from 1.4 to 1.48
          Globals.SCALE_UP_BURSTY_JOB_DEFAULT = 1.5;
          Globals.SMALL_JOB_DUR_THRESHOLD = 50.0;
          Globals.SMALL_JOB_TASK_NUM_THRESHOLD = 250;
          Globals.SCALE_BURSTY_DURATION = 1 / 2.0;
          break;
        default:
          Globals.SCALE_UP_BATCH_JOB = 1;
        }
        break;
      case SIMPLE:
        Globals.TRACE_FILE = null;
        Globals.numBurstyJobPerQueue = 25;
        Globals.STEP_TIME = 1.0;
        Globals.USE_TRACE = false;
        Globals.numInteractiveTask = (int) ((int) 20 * NUM_MACHINES
            * MACHINE_MAX_RESOURCE);
        Globals.numbatchTask = (int) ((int) 50 * NUM_MACHINES
            * MACHINE_MAX_RESOURCE);
        break;
      default:
        Globals.numBatchJobs = 30;
        Globals.numInteractiveTask = 2000;
      }
      

      Globals.CAPACITY = Globals.MACHINE_MAX_RESOURCE * Globals.NUM_MACHINES;

      double scaleUp = (double) (Globals.NUM_MACHINES
          * Globals.MACHINE_MAX_RESOURCE) / (double) Globals.TRACE_CLUSTER_SIZE;
      
      Globals.SCALE_UP_BATCH_JOB = Math.floor((double) 1 * scaleUp);
      
      Globals.SCALE_UP_BURSTY_JOB = Globals.SCALE_UP_BURSTY_JOB_DEFAULT * scaleUp * (1+Globals.ESTIMASION_ERRORS);
    }
    
  }

  public static void runSimulationScenario(boolean genInputOnly) {

    Globals.SESSION_DATA = new SessionData();

    long tStart = System.currentTimeMillis();
    String extraName = "";
    String extra = Globals.SCALE_UP_FACTOR > 1
        ? "_" + Globals.SCALE_UP_FACTOR + "x" : "";

    if (Globals.runmode == Runmode.ScaleUpBurstyJobs) {
      extraName = "_s" + Globals.SCALE_UP_BURSTY_JOB / 50;

    } else if (Globals.runmode == Runmode.ScaleBatchTaskDuration
        || Globals.runmode == Runmode.AvgTaskDuration
        || Globals.runmode == Runmode.EstimationErrors) {
      extraName = Globals.EXTRA;
    } else
      extraName = "_" + Globals.numBurstyQueues + "_" + Globals.numBatchQueues
          + '_' + (int) Globals.MACHINE_MAX_RESOURCE;

    Globals.DataFolder = "input_gen";
    Globals.FileInput = "jobs_input_" + Globals.numBurstyQueues + '_'
        + Globals.numBatchQueues + '_' + (int) Globals.MACHINE_MAX_RESOURCE
        + '_' + Globals.workload + extra + Globals.EXTRA + ".txt";
    Globals.QueueInput = "queue_input_" + Globals.numBurstyQueues + '_'
        + Globals.numBatchQueues + '_' + (int) Globals.MACHINE_MAX_RESOURCE
        + '_' + Globals.workload + Globals.EXTRA + ".txt";

    if (Globals.METHOD.equals(Method.DRF)) {
      Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
      Globals.FileOutput = "DRF-output" + extraName + ".csv";
    } else if (Globals.METHOD.equals(Method.SpeedFair)) {
      Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.SpeedFair;
      Globals.FileOutput = "SpeedFair-output" + extraName + ".csv";
    } else if (Globals.METHOD.equals(Method.DRFW)) {
      Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
      Globals.FileOutput = "DRF-W-output" + extraName + ".csv";
    } else if (Globals.METHOD.equals(Method.Strict)) {
      Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
      Globals.FileOutput = "Strict-output" + extraName + ".csv";
    } else {
      System.err.println("Error! test case");
      return;
    }

    Globals.PathToInputFile = Globals.DataFolder + "/" + Globals.FileInput;
    Globals.PathToQueueInputFile = Globals.DataFolder + "/"
        + Globals.QueueInput;
    Globals.PathToOutputFile = Globals.outputFolder + "/" + Globals.FileOutput;
    Globals.PathToResourceLog = "log" + "/" + Globals.FileOutput;

    if (Globals.IS_GEN) {
      if (Globals.USE_TRACE) {
        Queue<BaseDag> tracedJobs = GenInput
            .readWorkloadTrace(Globals.TRACE_FILE);
        GenInput.genInputFromWorkload(Globals.numBurstyQueues,
            Globals.numBurstyJobPerQueue, Globals.numInteractiveTask,
            Globals.numBatchQueues, Globals.numBatchJobs, tracedJobs);
      } else
        GenInput.genInput(Globals.numBurstyQueues, Globals.numBurstyJobPerQueue,
            Globals.numInteractiveTask, Globals.numBatchQueues,
            Globals.numBatchJobs);
    }

    // print ALL parameters for the record
    System.out.println("=====================");
    System.out.println("Simulation Parameters");
    System.out.println("=====================");
    System.out.println("Runmode              = " + Globals.runmode);
    System.out.println("METHOD              = " + Globals.METHOD);
    System.out.println("Cluster Size        = " + Globals.NUM_MACHINES);
    System.out.println("Server Capacity     = " + Globals.MACHINE_MAX_RESOURCE);
    System.out.println("Workload            = " + Globals.TRACE_FILE);
    System.out.println("numBatchJobs        = " + Globals.numBatchJobs);
    // System.out.println("Speedup Durations = " + Globals.SPEEDUP_DURATIONS);
    // System.out.println("Speedup Rates = " + Globals.SPEEDUP_RATES);
    System.out.println("PathToInputFile     = " + Globals.PathToInputFile);
    System.out.println("PathToQueueInputFile= " + Globals.PathToQueueInputFile);
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
    System.out
        .print("========== " + (duration / (1000)) + " seconds ==========\n");
  }

  public static void runDynamicQueueNumber() {
    long tStart = System.currentTimeMillis();
    String extraName = "u" + Globals.USER1_MAX_Q_NUM + "_u"
        + Globals.USER2_MAX_Q_NUM;

    if (Globals.PRED_MODE == Globals.PredMode.PerfectPrediction) {
      Globals.FileOutput = "_output" + extraName + "_perfect" + ".csv";
    } else if (Globals.PRED_MODE == Globals.PredMode.GoodPrediction) {
      Globals.FileOutput = "_output" + extraName + "_good" + ".csv";
    } else if (Globals.PRED_MODE == Globals.PredMode.WrongPrediction) {
      Globals.FileOutput = "_output" + extraName + "_wrong" + ".csv";
    } else if (Globals.PRED_MODE == Globals.PredMode.StaticPrediction) {
      Globals.FileOutput = "_output" + extraName + "_static" + ".csv";
    } else {
      System.err.println("Error! test case");
      return;
    }
    Globals.FileOutput = Globals.QUEUE_SCHEDULER + Globals.FileOutput;

    Globals.DataFolder = "input_gen";
    Globals.FileInput = "jobs_input_" + extraName + ".txt";
    Globals.QueueInput = "queue_input_" + extraName + ".txt";

    Globals.PathToInputFile = Globals.DataFolder + "/" + Globals.FileInput;
    Globals.PathToQueueInputFile = Globals.DataFolder + "/"
        + Globals.QueueInput;
    Globals.PathToOutputFile = Globals.outputFolder + "/" + Globals.FileOutput;
    Globals.PathToResourceLog = "log" + "/" + Globals.FileOutput;

    if (Globals.IS_GEN) {
      Queue<BaseDag> tracedJobs = GenInput
          .readWorkloadTrace(Globals.TRACE_FILE);
      GenInput.genInputFromWorkload(Globals.USER1_MAX_Q_NUM,
          Globals.USER2_MAX_Q_NUM, Globals.JOB_NUM_PER_QUEUE_CHANGE,
          tracedJobs);
    }

    // print ALL parameters for the record
    System.out.println("==============================================");
    System.out.println("Simulation Parameters");
    System.out.println("==============================================");
    System.out.println("user1's queue num   = " + Globals.USER1_MAX_Q_NUM);
    System.out.println("user2's queue num   = " + Globals.USER2_MAX_Q_NUM);
    System.out.println("Workload            = " + Globals.TRACE_FILE);
    System.out.println("PathToInputFile     = " + Globals.PathToInputFile);
    System.out.println("PathToOutputFile    = " + Globals.PathToOutputFile);
    System.out.println("SIMULATION_END_TIME = " + Globals.SIM_END_TIME);
    System.out.println("STEP_TIME           = " + Globals.STEP_TIME);
    System.out.println("QUEUE_SCHEDULER     = " + Globals.QUEUE_SCHEDULER);
    System.out.println("=====================\n");

    System.out.println("Start simulation ...");
    System.out.println("Please wait ...");
    Simulator simulator = new Simulator(1);
    simulator.simulateDynamicQueues();
    System.out.println("\nEnd simulation ...");
    long duration = System.currentTimeMillis() - tStart;
    System.out
        .print("========== " + (duration / (1000)) + " seconds ==========\n");
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
      runmode = Globals.Runmode.MultipleBatchQueueRun;
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
    Utils.createUserDir("log");
    Utils.createUserDir("output");
    Utils.createUserDir("input_gen");

    System.out.println("Started Simulation....");
    System.out.println("........" + now() + ".....");

    Globals.NUM_DIMENSIONS = 2;
    Globals.MACHINE_MAX_RESOURCE = 1000;
    Globals.numBatchJobs = 500;
    Globals.DRFW_weight = 4.0;

    Globals.BATCH_JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.All;
    Globals.BURSTY_JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.Trace;

    Globals.workload = Globals.WorkLoadType.BB;

    // Globals.workload = workloadMenu();
    // if (Globals.workload == null) {
    // return;
    // }

    if (args.length >= 1) {
      String stWorkload = args[0];
      if (stWorkload.equals("BB")) {
        Globals.workload = Globals.WorkLoadType.BB;
      } else if (stWorkload.equals("TPC-H")) {
        Globals.workload = Globals.WorkLoadType.TPC_H;
      } else if (stWorkload.equals("TPC-DS")) {
        Globals.workload = Globals.WorkLoadType.TPC_DS;
      }
    }

    Globals.runmode = Runmode.EstimationErrors;

    if (args.length >= 2) {
      String stRunmode = args[0];
      if (stRunmode.equals("MultipleBurstyQueues")) {
        Globals.runmode = Runmode.MultipleBurstyQueues;
      } else if (stRunmode.equals("MultipleBatchQueueRun")) {
        Globals.runmode = Runmode.MultipleBatchQueueRun;
      } else if (stRunmode.equals("ScaleBatchTaskDuration")) {
        Globals.runmode = Runmode.ScaleBatchTaskDuration;
      } else if (stRunmode.equals("ScaleUpBurstyJobs")) {
        Globals.runmode = Runmode.ScaleUpBurstyJobs;
      } else if (stRunmode.equals("AvgTaskDuration")) {
        Globals.runmode = Runmode.AvgTaskDuration;
      } else if (stRunmode.equals("EstimationErrors")) {
        Globals.runmode = Runmode.EstimationErrors;
      } else {
        System.err.println("usage [workload] [runmode]");
        return;
      }
    }

    // Globals.runmode = Runmode.TrialRun;
    if (Globals.runmode.equals(Runmode.SingleRun)) {
      Globals.SIM_END_TIME = 800.0;
      // Globals.METHOD = Method.DRFW;
      // Globals.METHOD = Method.Strict;
      // Globals.METHOD = Method.DRF;
      Globals.METHOD = Method.SpeedFair;
      // Globals.SIM_END_TIME = 1000000;
      Globals.NUM_MACHINES = 1;
      Globals.MACHINE_MAX_RESOURCE = 1000;
      Globals.numBatchQueues = 1;
      Globals.numBurstyQueues = 3;
      Globals.numBatchJobs = 100;
      // Globals.SCALE_UP_BATCH_JOB = 1;
      Globals.DEBUG_LOCAL = true;
      Globals.workload = Globals.WorkLoadType.BB;

      Globals.setupParameters(Globals.SetupMode.ShortInteractive, 1);

      System.out.println(
          "=================================================================");
      System.out.println("Run METHOD: " + Globals.METHOD + " with "
          + Globals.numBatchQueues + " batch queues.");
      runSimulationScenario(false);
      System.out.println();
    } else if (Globals.runmode.equals(Runmode.MultipleBurstyQueues)) {
      // Globals.DEBUG_START = 150.0;
      // Globals.DEBUG_END = 250.0;
      Globals.SIM_END_TIME = 800.0;

      Method[] methods = { Method.DRF, Method.Strict, Method.SpeedFair };
      // Method[] methods = { Method.SpeedFair};
      Globals.NUM_MACHINES = 1;
      Globals.MACHINE_MAX_RESOURCE = 1000;
      Globals.numBatchQueues = 1;
      Globals.numBurstyQueues = 3;
      Globals.numBatchJobs = 100;
      Globals.DEBUG_LOCAL = true;
      Globals.workload = Globals.WorkLoadType.BB;

      Globals.setupParameters(Globals.SetupMode.ShortInteractive, 1);

      for (int i = 0; i < methods.length; i++) {
        // Trigger garbage collection to clean up memory.
        System.out.println("[INFO] Wait for garbage collectors ...");
        freeMemory();

        if (i == 0)
          Globals.IS_GEN = true;
        else
          Globals.IS_GEN = false;

        Globals.METHOD = methods[i];

        System.out.println(
            "=================================================================");
        System.out.println("Run METHOD: " + Globals.METHOD + " with "
            + Globals.numBatchQueues + " batch queues.");
        runSimulationScenario(false);
        System.out.println();
      }

    } else if (Globals.runmode.equals(Runmode.MultipleBatchQueueRun)) {
      Globals.SetupMode mode = Globals.SetupMode.ShortInteractive;
      Method[] methods = { Method.DRF, Method.DRFW, Method.Strict,
          Method.SpeedFair };
      int[] batchQueueNums = { 1, 2, 4, 8, 16, 32 };

      Globals.setupParameters(mode, 1);

      for (int j = 0; j < batchQueueNums.length; j++) {
        for (int i = 0; i < methods.length; i++) {
          // Trigger garbage collection to clean up memory.
          System.out.println("[INFO] Wait for garbage collectors ...");
          freeMemory();

          if (i == 0)
            Globals.IS_GEN = true;
          else
            Globals.IS_GEN = false;

          Globals.METHOD = methods[i];
          Globals.numBatchQueues = batchQueueNums[j];
          System.out.println(
              "=================================================================");
          System.out.println("Run METHOD: " + Globals.METHOD + " with "
              + Globals.numBatchQueues + " batch queues.");
          runSimulationScenario(false);
          System.out.println(
              "==================================================================");
        }
      }
    } else if (Globals.runmode.equals(Runmode.ScaleUpBurstyJobs)) {
      Globals.SetupMode mode = Globals.SetupMode.LongInteractive;
      Globals.numBatchQueues = 8;

      Method[] methods = { Method.DRF, Method.DRFW, Method.Strict,
          Method.SpeedFair };
      int[] scaleFactors = { 1, 2, 4, 6, 8, 10 };

      for (int j = 0; j < scaleFactors.length; j++) {

        Globals.setupParameters(mode, scaleFactors[j]);

        for (int i = 0; i < methods.length; i++) {
          if (i == 0)
            Globals.IS_GEN = true;
          else
            Globals.IS_GEN = false;

          Globals.METHOD = methods[i];
          System.out.println(
              "=================================================================");
          System.out.println("Run METHOD: " + Globals.METHOD
              + " with scale-up factor= " + scaleFactors[j]);
          runSimulationScenario(false);
          System.out.println(
              "==================================================================");
        }
      }
    } else if (Globals.runmode.equals(Runmode.AvgTaskDuration)) {
      Globals.STEP_TIME = 1.0;
      // Globals.SIM_END_TIME = 5000;
      Globals.SetupMode mode = Globals.SetupMode.ShortInteractive;
      // double[] avgTaskDurations = { 2.0, 4.0, 6.0, 8.0, 10.0, 15.0, 20.0,
      // 30.0, 40.0, 60.0, 80.0, 100.0};
      double[] avgTaskDurations = {2.0, 6.0, 10.0, 15.0, 100.0};
      // double[] avgTaskDurations = { 4.0, 8.0, 20, 30, 40, 60, 80, 100.0};
      // double[] avgTaskDurations = {2.0};

      // Globals.SCALE_BURSTY_DURATION = 1.0/20;
      // Globals.SCALE_UP_BURSTY_JOB = 50*20;

      Globals.MACHINE_MAX_RESOURCE = 1000;
      Globals.METHOD = Method.SpeedFair;
      Globals.setupParameters(mode, 1);
      Globals.numBatchQueues = 4;

      Globals.IS_GEN = true;
      Globals.numBatchJobs = 100;

      for (int i = 0; i < avgTaskDurations.length; i++) {
        // Trigger garbage collection to clean up memory.
        Globals.AVG_TASK_DURATION = avgTaskDurations[i];
        // double temp =
        // (Globals.WORKLOAD_AVG_TASK_DURATION/Globals.AVG_TASK_DURATION);
        // Globals.numBatchJobs = (int) (numberOfBatchJobs*temp);
        System.out.println("[INFO] Wait for garbage collectors ...");
        freeMemory();
        Globals.EXTRA = "_avg" + avgTaskDurations[i] + "";
        System.out.println(
            "=================================================================");
        System.out.println("Run METHOD: " + Globals.METHOD
            + " with avg task duration= " + Globals.AVG_TASK_DURATION);
        System.out.println("Run METHOD: " + Globals.METHOD + " with "
            + Globals.numBatchQueues + " batch queues.");
        runSimulationScenario(false);
        System.out.println(
            "==================================================================");
      }
    } else if (Globals.runmode.equals(Runmode.ScaleBatchTaskDuration)) {
      Globals.SetupMode mode = Globals.SetupMode.ShortInteractive;
      double[] scaleUps = { 1.0 / 32.0, 1.0 / 16.0, 1.0 / 8.0, 1.0 / 4.0,
          1.0 / 2.0, 1, 2, 4, 8, 16, 32 };
      Globals.METHOD = Method.SpeedFair;
      Globals.numBatchQueues = 8;
      Globals.setupParameters(mode, 1);
      Globals.IS_GEN = true;
      
//       Globals.SCALE_BURSTY_DURATION = 1.0/20;
//       Globals.SCALE_UP_BURSTY_JOB = 50*20;

      for (int i = 0; i < scaleUps.length; i++) {
        // Trigger garbage collection to clean up memory.
        Globals.SCALE_BATCH_DURATION = scaleUps[i];
        Globals.SCALE_UP_BATCH_JOB = (double) 1.0 / scaleUps[i];
        System.out.println("[INFO] Wait for garbage collectors ...");
        freeMemory();
        Globals.EXTRA = "_t" + Globals.SCALE_BATCH_DURATION + "x";
        System.out.println(
            "=================================================================");
        System.out.println("Run METHOD: " + Globals.METHOD
            + " with scale-up factor= " + Globals.SCALE_BATCH_DURATION);
        System.out.println("Run METHOD: " + Globals.METHOD + " with "
            + Globals.numBatchQueues + " batch queues.");
        runSimulationScenario(false);
        System.out.println(
            "==================================================================");
      }
    } else if (Globals.runmode.equals(Runmode.EstimationErrors)) {
      Globals.SetupMode mode = Globals.SetupMode.ShortInteractive;
       double[] errors = { -0.9, -0.8, -0.7,  -0.6,-0.5, -0.4, -0.3, -0.2, -0.1, 0.0, 0.1, 0.2, 0.3,
       0.4, 0.5, 0.5, 0.6, 0.7, 0.8, 0.9};
      
//      double[] errors = { -0.9, 0 -0.2, 0.0, 0.2 , 0.9};
//      Globals.numBatchJobs = 100; Globals.MACHINE_MAX_RESOURCE = 100;     
//      Globals.workload = Globals.WorkLoadType.TPC_H;
      
      Globals.METHOD = Method.SpeedFair;
      Globals.numBatchQueues = 8;
      
      Globals.IS_GEN = true;

      for (int i = 0; i <= errors.length; i++) {
        // Trigger garbage collection to clean up memory.
        if (i<errors.length){
          Globals.ESTIMASION_ERRORS = errors[i];
          Globals.EXTRA = "_err" + errors[i];
        }
        else {
          Globals.ESTIMASION_ERRORS = 0.0;
          Globals.numBatchQueues = 0;
          Globals.EXTRA = "_err_base";
        }
          
        Globals.setupParameters(mode, 1);
        
        System.out.println("[INFO] Wait for garbage collectors ...");
        freeMemory();
        
        System.out.println(
            "=================================================================");
        System.out.println("Run METHOD: " + Globals.METHOD
            + " with EstimationErrors= " + Globals.EXTRA);
        System.out.println("Run METHOD: " + Globals.METHOD + " with "
            + Globals.numBatchQueues + " batch queues.");
        runSimulationScenario(false);
        System.out.println(
            "==================================================================");
      }
    } else if (Globals.runmode.equals(Runmode.TrialRun)) {
      /*
       * user 1 runs streaming jobs user 2 keep changing its number of queues
       */
      Globals.IS_GEN = true;
      Globals.numBurstyQueues = 0;
      // Globals.DEBUG_LOCAL = true;
      // Globals.DEBUG_START = 0.0;
      // Globals.DEBUG_END = 10.0;
      // Globals.SIM_END_TIME = 300;
      Globals.USER1_MAX_Q_NUM = 1;
      Globals.USER2_MAX_Q_NUM = 10;
      Globals.SIM_END_TIME = 10000;
      Globals.user2_q_nums = QueueData.QUEUE_NUM_FB;

      Globals.JOB_NUM_PER_QUEUE_CHANGE = (int) (10 * Globals.User2QueueInterval
          / 100);
      Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
      Globals.PredMode[] predModes = { PredMode.PerfectPrediction,
          PredMode.GoodPrediction, PredMode.WrongPrediction,
          PredMode.StaticPrediction };
      // Globals.PredMode[] predModes = {PredMode.PerfectPrediction,
      // PredMode.GoodPrediction };

      for (int i = 0; i < predModes.length; i++) {
        Globals.PRED_MODE = predModes[i];
        Globals.IS_GEN = true;
        Globals.setupParameters(SetupMode.others, 1);
        System.out.println(
            "=================================================================");
        System.out.println("Run Queue Scheduler: " + Globals.QUEUE_SCHEDULER);
        System.out.println("Run PredMode: " + Globals.PRED_MODE);
        runDynamicQueueNumber();
        System.out.println(
            "==================================================================");
      }
    } else {
      int numberOfServers = 1;
      Globals.NUM_MACHINES = 1;
      Globals.MACHINE_MAX_RESOURCE = numberOfServers;
      Globals.workload = Globals.WorkLoadType.BB;
      // Globals.workload = Globals.WorkLoadType.TPC_DS;
      // Globals.workload = Globals.WorkLoadType.TPC_H;

      Globals.SCALE_UP_FACTOR = 1;
      Globals.NUM_DIMENSIONS = 2;

      Globals.setupParameters(Globals.SetupMode.ShortInteractive, 1);

      Globals.SCALE_UP_BURSTY_JOB = Globals.SCALE_UP_FACTOR
          * Globals.SCALE_UP_BURSTY_JOB;

      Globals.numBatchQueues = 8;
      Globals.numBurstyQueues = 1;
      Globals.numBatchJobs = 100;
      Globals.numBurstyJobPerQueue = 50;

      runSimulationScenario(true);
      System.out.println();
    }

    System.out.println("\n");
    System.out.println("........FINISHED ./.");
    System.out.println("........" + now() + ".....");
  }

  public static String now() {
    Calendar cal = Calendar.getInstance();
    return cal.getTime().toString();
  }
}