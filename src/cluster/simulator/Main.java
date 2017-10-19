package cluster.simulator;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Logger;

import cluster.data.JobData;
import cluster.data.SessionData;
import cluster.datastructures.BaseJob;
import cluster.simulator.Main.Globals.Method;
import cluster.simulator.Main.Globals.Runmode;
import cluster.simulator.Main.Globals.WorkLoadType;
import cluster.utils.GenInput;
import cluster.utils.Utils;

public class Main {

  private static Logger LOG = Logger.getLogger(Main.class.getName());
  static {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
  }

  public static class Globals {

    public static WorkLoadType workload = WorkLoadType.BB;

    public static String TRACE_FILE = "workload/queries_bb_FB_distr.txt"; // BigBench

    public static int SMALL_JOB_TASK_NUM_THRESHOLD = 250; // for 80 for 2 BB
                                                          // TPCDS,
    // => TPC-H --> 250

    public static double RES_UNIT = 0.001;

    public static final int TRACE_CLUSTER_SIZE = 25;

    public enum WorkLoadType {
      BB,
      TPC_DS,
      TPC_H,
      SIMPLE,
      SIMPLE_GOOD,
      SIMPLE_BAD
    };

    public enum ArrivalType {
      PERIOD,
      DISTRIBUTION,
      ALL_IN_ONE
    }

    public static ArrivalType batchArrivalType = ArrivalType.ALL_IN_ONE;
    public static ArrivalType burstyArrivalType = ArrivalType.PERIOD;

    public enum SetupMode {
      VeryShortInteractive,
      ShortInteractive,
      LongInteractive,
      VeryLongInteractive,
      others
    };

    public enum Runmode {
      Truthfulness,
      SimpleComparison,
      SingleRun,
      NONE
    }

    public static boolean USE_TRACE = true;

    public static final int TASK_ARRIVAL_RANGE = 50;

    public static final int JOB_START_ID = 0;

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

    // SP: Strict Priority, BPF: Bounded Priority Fairness
    public static enum Method {
      DRF,
      DRFW,
      ES,
      MaxMinMem,
      SpeedUp,
      Pricing
    }

    public static enum QueueSchedulerPolicy {
      DRF,
      EC,
      MaxMinMem,
      SpeedUp,
      Pricing
    };

    public static QueueSchedulerPolicy QUEUE_SCHEDULER = QueueSchedulerPolicy.EC;

    public enum JobsArrivalPolicy {
      All,
      One,
      Trace,
      Period,
      JobPeriod;
    }

    public enum PredMode {
      PerfectPrediction,
      WrongPrediction,
      StaticPrediction,
      GoodPrediction
    };

    public static PredMode PRED_MODE = PredMode.PerfectPrediction;

    public static JobsArrivalPolicy JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.All;

    public static boolean GEN_JOB_ARRIVAL = true;

    public static int NUM_MACHINES = 1; // TODO: NUM_MACHINES > 1 may
    // results in
    // low utilization this simulation.
    public final static int NUM_DIMENSIONS = 3; // CPU, MEM, GPU
    public static int[] INTERCHANGABLE_RESOURCE = {
        0,
        2 };

    public static double MACHINE_MAX_RESOURCE;
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

    public static int numBatchJobs = 200;
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

    public static double CAPACITY = 1.0;

    public static int PERIODIC_INTERVAL = 100;

    public static String EXTRA = "";

    public static void setupParameters() {
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
      case TPC_DS:
        Globals.WORKLOAD_AVG_TASK_DURATION = 31.60574050691386;
        Globals.TRACE_FILE = "workload/queries_tpcds_FB_distr_new.txt"; // TPC-DS
        break;
      case TPC_H:
        Globals.WORKLOAD_AVG_TASK_DURATION = 39.5366249014282;
        Globals.TRACE_FILE = "workload/queries_tpch_FB_distr.txt"; // TPC-H -->
        break;
      case SIMPLE:
        Globals.TRACE_FILE = "workload/simple.txt";
        Globals.USE_TRACE = true;
        break;
      case SIMPLE_GOOD:
        Globals.TRACE_FILE = "workload/simple_good.txt";
        Globals.USE_TRACE = true;
        double[] goodBetas = {
            10,
            0.1,
            1 };
        JobData.BETAs = goodBetas;
        JobData.reportBETAs=goodBetas;
        break;
      case SIMPLE_BAD:
        Globals.TRACE_FILE = "workload/simple_bad.txt";
        Globals.USE_TRACE = true;
        double[] badBetas = {
            10,
            0.1,
            1 };
        JobData.BETAs = badBetas;
        JobData.reportBETAs=badBetas;
        break;
      default:
        Globals.numBatchJobs = 30;
      }

      Globals.CAPACITY = Globals.MACHINE_MAX_RESOURCE * Globals.NUM_MACHINES;

      double scaleUp = (double) (Globals.NUM_MACHINES * Globals.MACHINE_MAX_RESOURCE)
          / (double) Globals.TRACE_CLUSTER_SIZE;

      // Globals.SCALE_UP_BATCH_JOB = Math.floor((double) 1 * scaleUp);
    }

  }

  public static void runSimulationScenario(boolean genInputOnly) {
    Globals.SESSION_DATA = new SessionData();

    long tStart = System.currentTimeMillis();
    String extraName = "";
    String extra = Globals.SCALE_UP_FACTOR > 1 ? "_" + Globals.SCALE_UP_FACTOR + "x" : "";

    extraName = "_" + Globals.numQueues + '_' + (int) Globals.MACHINE_MAX_RESOURCE;

    Globals.DataFolder = "input_gen";
    Globals.FileInput = "jobs_input_" + Globals.numQueues + '_' + (int) Globals.MACHINE_MAX_RESOURCE
        + '_' + Globals.workload + extra + Globals.EXTRA + ".txt";
    Globals.QueueInput = "queue_input_" + Globals.numQueues + '_'
        + (int) Globals.MACHINE_MAX_RESOURCE + '_' + Globals.workload + Globals.EXTRA + ".txt";

    if (Globals.METHOD.equals(Method.DRF)) {
      Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
      Globals.FileOutput = "DRF-output" + extraName + ".csv";
    } else if (Globals.METHOD.equals(Method.ES)) {
      Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.EC;
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
    } else {
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
        GenInput.genInputFromWorkload(Globals.numQueues, Globals.numBatchJobs, tracedJobs);
      } else GenInput.genInput(Globals.numQueues, Globals.numBatchJobs);
    }

    // print ALL parameters for the record
    System.out.println("=====================");
    System.out.println("Simulation Parameters");
    System.out.println("=====================");
    System.out.println("Runmode             = " + Globals.runmode);
    System.out.println("METHOD              = " + Globals.METHOD);
    System.out.println("Cluster Size        = " + Globals.NUM_MACHINES);
    System.out.println("Server Capacity     = " + Globals.MACHINE_MAX_RESOURCE);
    System.out.println("Workload            = " + Globals.TRACE_FILE);
    System.out.println("numBatchJobs        = " + Globals.numBatchJobs);
    System.out.println("PathToInputFile     = " + Globals.PathToInputFile);
    System.out.println("PathToQueueInputFile= " + Globals.PathToQueueInputFile);
    System.out.println("PathToOutputFile    = " + Globals.PathToOutputFile);
    System.out.println("SIMULATION_END_TIME = " + Globals.SIM_END_TIME);
    System.out.println("STEP_TIME           = " + Globals.STEP_TIME);
    System.out.println("METHOD              = " + Globals.METHOD);
    System.out.println("QUEUE_SCHEDULER     = " + Globals.QUEUE_SCHEDULER);
    System.out.println("=====================\n");

    if (genInputOnly) return;

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
    String[] workloads = {
        "BB",
        "TPC-DS",
        "TPC-H" };
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

    String[] workloads = {
        "MultipleBatchQueueRun" };
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
    Utils.createUserDir("log");
    Utils.createUserDir("output");
    Utils.createUserDir("input_gen");

    System.out.println("Started Simulation....");
    System.out.println("........" + now() + ".....");

    Globals.MACHINE_MAX_RESOURCE = 1000;
    Globals.numBatchJobs = 500;
    Globals.DRFW_weight = 4.0;

    Globals.workload = Globals.WorkLoadType.SIMPLE;

    if (args.length >= 1) {
      String stWorkload = args[0];
      if (stWorkload.equals("BB")) {
        Globals.workload = Globals.WorkLoadType.BB;
      } else if (stWorkload.equals("TPC-H")) {
        Globals.workload = Globals.WorkLoadType.TPC_H;
      } else if (stWorkload.equals("TPC-DS")) {
        Globals.workload = Globals.WorkLoadType.TPC_DS;
      } else {
        System.err.println("usage [workload]");
        return;
      }
    }

    Globals.runmode = Runmode.NONE;

    if (args.length >= 2) {
      String stRunmode = args[1];
      if (stRunmode.equals("SingleRun")) {
      } else {
        System.err.println("usage [workload] [runmode]");
        return;
      }
    }

    if (Globals.runmode.equals(Runmode.SingleRun)) {
      Globals.METHOD = Method.ES;
      Globals.NUM_MACHINES = 10;
      Globals.MACHINE_MAX_RESOURCE = 40;
      Globals.numQueues = 8;
      Globals.numBatchJobs = 100;
      Globals.DEBUG_LOCAL = true;
      Globals.workload = Globals.WorkLoadType.TPC_DS;
      Globals.setupParameters();
      System.out.println("=================================================================");
      System.out.println(
          "Run METHOD: " + Globals.METHOD + " with " + Globals.numQueues + " batch queues.");
      runSimulationScenario(false);
      System.out.println();
    } else if (Globals.runmode.equals(Runmode.SimpleComparison)) {
      Globals.Method[] methods = {
          Method.ES,
          Method.MaxMinMem };
      Globals.NUM_MACHINES = 1;
      Globals.SIM_END_TIME = 100.0;
      Globals.numBatchJobs = 100;
      Globals.MACHINE_MAX_RESOURCE = 100;
      Globals.numQueues = 3;
      Globals.workload = Globals.WorkLoadType.SIMPLE_GOOD;
      Globals.IS_GEN = true;
      for (Globals.Method method : methods) {
        Globals.METHOD = method;
        Globals.setupParameters();
        runSimulationScenario(false);
        Globals.IS_GEN = false;
        System.out.println();
      }
    } else {
      Globals.MEMORY_SCALE_DOWN = 1;
      Globals.NUM_MACHINES = 1;
      Globals.SIM_END_TIME = 100.0;
//       Globals.METHOD = Method.ES;
//      Globals.METHOD = Method.MaxMinMem;
//      Globals.METHOD = Method.SpeedUp;
//      Globals.METHOD = Method.Pricing;
      Globals.METHOD = Method.DRF;
      Globals.MACHINE_MAX_RESOURCE = 100;
      Globals.workload = Globals.WorkLoadType.SIMPLE;
      Globals.IS_GEN = true;
      Globals.SCALE_UP_FACTOR = 1;
      Globals.setupParameters();
      Globals.numQueues = 3;
      Globals.numBatchJobs = 4500;
      runSimulationScenario(false);
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
