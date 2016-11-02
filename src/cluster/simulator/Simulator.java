package cluster.simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import cluster.cluster.Cluster;
import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.JobQueueList;
import cluster.datastructures.Resources;
import cluster.datastructures.Stage;
import cluster.datastructures.StageDag;
import cluster.resources.LeftOverResAllocator;
import cluster.schedulers.InterJobScheduler;
import cluster.schedulers.IntraJobScheduler;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.JobsArrivalPolicy;
import cluster.utils.Output;
import cluster.utils.Randomness;
import cluster.utils.Utils;

// implement the timeline server
public class Simulator {

  public static boolean DEBUG = true;

  public static double CURRENT_TIME = 0;

  public static Queue<BaseDag> runnableJobs = null;
  public static Queue<BaseDag> runningJobs = null;
  public static Queue<BaseDag> runningBatchJobs = new LinkedList<BaseDag>();
  public static Queue<BaseDag> completedJobs = null;

  private int burstyJobIdx = Globals.numBurstyJobPerQueue * Globals.numBurstyQueues;

  public static JobQueueList QUEUE_LIST = null;

  public static Cluster cluster = null;

  public static Randomness r;
  private double nextTimeToLaunchJob = 0;

  int totalReplayedJobs = 0;
  int lastCompletedJobs = 0;

  public static QueueScheduler queueSched = null;

  public static InterJobScheduler interJobSched = null;
  public static IntraJobScheduler intraJobSched = null;

  public static LeftOverResAllocator leftOverResAllocator = null;

  // dag_id -> list of tasks
  public static Map<Integer, Set<Integer>> tasksToStartNow = null;

  public static boolean ONLINE = true;
  
  public static boolean ADD_MORE = true;
  public static int user1_q_num = 1;
  public static int user2_q_num = 1;
  
  public static boolean IS_STOP = false;
  
  
  public static int completedJobCnt = 0;

  public static void duplicateJob(Queue<BaseDag> dags, int dagId) {
    StageDag dag = null;
    for (BaseDag d : dags) {
      if (d.dagId == dagId) {
        dag = (StageDag) d;
        break;
      }
    }

    if (dag == null)
      return;

    int newDagId = dags.size();
    try {
      StageDag newDag = (StageDag) dag.clone();
      newDag.dagId = newDagId;
      newDag.dagName = "" + newDagId;
      dags.add(newDag);
      Simulator.QUEUE_LIST.addRunnableJob2Queue(newDag, newDag.getQueueName());
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
  }

  public Simulator() {
    QUEUE_LIST = new JobQueueList();

    QUEUE_LIST.readQueue(Globals.PathToQueueInputFile);

    QUEUE_LIST.printQueueInfo();

    runnableJobs = StageDag.readDags(Globals.PathToInputFile);

    Output.debugln(DEBUG, "Print DAGs");
    // for (BaseDag dag : runnableJobs) {
    // ((StageDag) dag).viewDag();
    // }

    cluster = new Cluster(true, new Resources(Globals.MACHINE_MAX_RESOURCE));

    if (Globals.COMPUTE_STATISTICS) {
      double[] area_makespan = new double[Globals.NUM_DIMENSIONS];
      Output.debugln(DEBUG, "#dag_id maxCP area");
      double total_area = 0.0;
      for (BaseDag dag : runnableJobs) {
        StageDag ddag = (StageDag) dag;
        double[] bottlenecks = new double[Globals.NUM_DIMENSIONS];
        for (Stage stage : ddag.stages.values()) {
          bottlenecks[stage.vDemands.resBottleneck()] += 1;
        }
        Output.debugln(DEBUG, "dagName:" + ddag.dagName + " numOfStages:" + ddag.stages.values().size());
        for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
          Output.debug(DEBUG, " " + bottlenecks[i] / ddag.stages.values().size());
        }
        Output.debug(DEBUG, "\n");
      }
      // System.exit(-1); //TODO: why exit in the middle
      for (BaseDag dag : runnableJobs) {
        for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
          area_makespan[i] += dag.area().get(i);
        }
        double areaJob = (double) Collections.max(dag.area().values()) / Globals.MACHINE_MAX_RESOURCE;
        double maxCPJob = dag.getMaxCP();
        Output.debugln(DEBUG, dag.dagId + " " + maxCPJob + " " + areaJob);
        total_area += areaJob;
      }
      double max_area_makespan = Double.MIN_VALUE;
      for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
        max_area_makespan = Math.max(max_area_makespan, area_makespan[i] / Globals.MACHINE_MAX_RESOURCE);
      }
      Output.debugln(DEBUG, "makespan_lb: " + total_area + " " + max_area_makespan);
      // System.exit(-1); // TODO: why exit in the middle
    }

    totalReplayedJobs = runnableJobs.size();
    runningJobs = new LinkedList<BaseDag>();
    completedJobs = new LinkedList<BaseDag>();

    interJobSched = new InterJobScheduler();
    intraJobSched = new IntraJobScheduler();

    queueSched = new QueueScheduler();

    leftOverResAllocator = new LeftOverResAllocator();

    tasksToStartNow = new TreeMap<Integer, Set<Integer>>();

    r = new Randomness();

    // Initialize output & log files
    Output.write("", false, Globals.PathToResourceLog);
  }

  public void simulateMultiQueues() {
    Simulator.CURRENT_TIME = 0;
    while (true) {
      // for (Simulator.CURRENT_TIME = 0; Simulator.CURRENT_TIME <=
      // Globals.SIM_END_TIME; Simulator.CURRENT_TIME += Globals.STEP_TIME) {

      if (Simulator.CURRENT_TIME >= Globals.DEBUG_START && Simulator.CURRENT_TIME <= Globals.DEBUG_END) {
        DEBUG = true;
      } else
        DEBUG = false;

      Output.debugln(DEBUG, "\n==== STEP_TIME:" + Simulator.CURRENT_TIME + " ====\n");

      Simulator.CURRENT_TIME = Utils.round(Simulator.CURRENT_TIME, 2);
      tasksToStartNow.clear();

      // terminate any task if it can finish and update cluster available
      // resources; converting waiting tasks to runnable tasks
      Map<Integer, List<Integer>> finishedTasks = cluster.finishTasks();

      // update jobs status with newly finished tasks
      boolean jobCompleted = updateJobsStatus(finishedTasks);

      // handle jobs completion and arrivals
      boolean newJobArrivals = handleNewJobArrival4MultiQueues();

      // STOP condition
      if (stop()) {
        printReport();
        writeReport();
        // EXIT the loop
        break;
      }

      QUEUE_LIST.updateRunningQueues();

      // if(false)
      if (!jobCompleted && !newJobArrivals && finishedTasks.isEmpty() && Globals.ENABLE_PREEMPTION)
        Output.debugln(DEBUG, "----- Do nothing ----");
      else {
        Output.debugln(DEBUG,
            "[Simulator]: START work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());
        queueSched.schedule();
        Output.debugln(DEBUG,
            "[Simulator]: END work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());
      }

      for (BaseDag dag : Simulator.runningJobs) {
        dag.receivedService.addUsage(dag.getRsrcInUse());
      }

      Simulator.printUsedResources();
      Simulator.writeResourceUsage();
      Simulator.CURRENT_TIME += Globals.STEP_TIME;
    }
    System.out.println("\n==== END STEP_TIME:" + Simulator.CURRENT_TIME + " ====\n");
  }
  
  public void simulateDynamicQueues() {
    Simulator.CURRENT_TIME = 0;
    while (true) {
      // for (Simulator.CURRENT_TIME = 0; Simulator.CURRENT_TIME <=
      // Globals.SIM_END_TIME; Simulator.CURRENT_TIME += Globals.STEP_TIME) {

      if (Simulator.CURRENT_TIME >= Globals.DEBUG_START && Simulator.CURRENT_TIME <= Globals.DEBUG_END) {
        DEBUG = true;
      } else
        DEBUG = false;

      Output.debugln(DEBUG, "\n==== STEP_TIME:" + Simulator.CURRENT_TIME + " ====\n");

      Simulator.CURRENT_TIME = Utils.round(Simulator.CURRENT_TIME, 2);
      tasksToStartNow.clear();

      // terminate any task if it can finish and update cluster available
      // resources; converting waiting tasks to runnable tasks
      Map<Integer, List<Integer>> finishedTasks = cluster.finishTasks();

      // update jobs status with newly finished tasks
      boolean jobCompleted = updateJobsStatusDynamicQueues(finishedTasks);

      // handle jobs completion and arrivals
      boolean newJobArrivals = handleNewJobArrival4DynamicQueues();

      // STOP condition
      if (isStop()) {
        printReport();
        writeReport();
        // EXIT the loop
        break;
      }

      QUEUE_LIST.updateRunningQueues();

      // if(false)
      if (!jobCompleted && !newJobArrivals && finishedTasks.isEmpty() && Globals.ENABLE_PREEMPTION)
        Output.debugln(DEBUG, "----- Do nothing ----");
      else {
        Output.debugln(DEBUG,
            "[Simulator]: START work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());
        queueSched.schedule();
        Output.debugln(DEBUG,
            "[Simulator]: END work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());
      }

      for (BaseDag dag : Simulator.runningJobs) {
        dag.receivedService.addUsage(dag.getRsrcInUse());
      }

      Simulator.printUsedResources();
      Simulator.writeResourceUsage();
      Simulator.CURRENT_TIME += Globals.STEP_TIME;
    }
    System.out.println("\n==== END STEP_TIME:" + Simulator.CURRENT_TIME + " ====\n");
  }

  private boolean isStop() {
    if (Simulator.CURRENT_TIME >= Globals.SIM_END_TIME)
      return true;
    return IS_STOP;
  }

  private void printReport() {
    System.out.println("\n==== Final Report: Completed Jobs ====");
    TreeMap<Integer, Double> results = new TreeMap<Integer, Double>();
    double makespan = Double.MIN_VALUE;
    double average = 0.0;
    ArrayList<Double> avgCompletionTimePerQueue = new ArrayList<Double>();
    for (BaseDag dag : completedJobs) {
      // System.out.println("Dag:" + dag.dagId + " compl. time:"
      // + (dag.jobEndTime - dag.jobStartTime));
      double dagDuration = dag.getCompletionTime();
      makespan = Math.max(makespan, dagDuration);
      average += dagDuration;
      results.put(dag.dagId, dagDuration);
      System.out.println(dag.dagId + " " + dagDuration );
    }
    average /= completedJobs.size();
    System.out.println("---------------------");
    System.out.println("Avg. job compl. time:" + average);
    System.out.println("Makespan:" + makespan);
    // for (Integer dagId : results.keySet()) {
    // System.out.println(dagId + " " + results.get(dagId));
    // }
    System.out.println("---------------------");
    // for (JobQueue queue : QUEUE_LIST.getJobQueues()) {
    // System.out.println("Queue " + queue.getQueueName() + "'s job compl. time:
    // " + queue.avgCompletionTime()); //TODO:
    // }
    System.out.println("NUM_OPT:" + Globals.NUM_OPT + " NUM_PES:" + Globals.NUM_PES);
  }

  private void writeReport() {
    Output.writeln("JobId, startTime, endTime, duration, queueName", false);
    System.out.println("==== Final Report: Completed Jobs ====");
    TreeMap<Integer, Double> results = new TreeMap<Integer, Double>();
    double makespan = Double.MIN_VALUE;
    for (BaseDag dag : completedJobs) {
      double dagDuration = (dag.jobEndTime - dag.jobStartRunningTime);
      makespan = Math.max(makespan, dagDuration);
      results.put(dag.dagId, dagDuration);
      Output.writeln(dag.dagId + "," + dag.jobStartRunningTime + "," + dag.jobEndTime + "," + dagDuration + ","
          + dag.getQueueName());
    }
  }

  public static void printUsedResources() {
    for (BaseDag dag : runningJobs) {
      Output.debugln(DEBUG,
          "Dag Id " + dag.dagId + " in " + dag.getQueueName() + " -- dag.rsrcInUse: " + dag.getRsrcInUse());
      // Output.writeln(dag.dagId + ", " + dag.rsrcInUse, true,
      // Globals.PathToResourceLog);
      // Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- Resource Share: " +
      // dag.rsrcQuota);
      // Resources minReq =
      // dag.serviceCurve.getMinReqService(Simulator.CURRENT_TIME -
      // dag.jobStartTime);
      // boolean isSatisfied = dag.serviceCurve.isSatisfied(dag.receivedService,
      // Simulator.CURRENT_TIME);
      // if (!isSatisfied)
      // Output.debugln(DEBUG,
      // "Dag Id " + dag.dagId + " " + " is NOT satified" + " -- Received : " +
      // dag.receivedService);
      // Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- Received: " +
      // dag.receivedService + " -- S.Curve: " + minReq);
    }
  }

  public static void writeResourceUsage() {
    for (JobQueue q : QUEUE_LIST.getJobQueues()) {
      Output.writeln(q.getResourceUsageStr(), true, Globals.PathToResourceLog);
      Output.debugln(DEBUG, q.getResourceUsageStr());
    }
  }

  boolean stop() {
    if (Globals.DEBUG_ALL && Simulator.CURRENT_TIME >= Globals.SIM_END_TIME)
      return true;
    // return (runnableJobs.isEmpty() && runningJobs.isEmpty() &&
    // (completedJobs.size() == totalReplayedJobs));
    if (runningBatchJobs.isEmpty() && runnableJobs.isEmpty())
      System.err.println("You need to increase the number of bursty jobs");

    return (runningBatchJobs.isEmpty());
  }
  
  boolean updateJobsStatus(Map<Integer, List<Integer>> finishedTasks) {
    boolean someDagFinished = false;
    if (!finishedTasks.isEmpty()) {
      Iterator<BaseDag> iter = runningJobs.iterator();
      while (iter.hasNext()) {
        BaseDag crdag = iter.next();
        if (finishedTasks.get(crdag.dagId) == null) {
          continue;
        }

        Output.debugln(DEBUG, "DAG:" + crdag.dagId + ": " + finishedTasks.get(crdag.dagId).size()
            + " tasks finished at time:" + Simulator.CURRENT_TIME);
        boolean thisDagFinished = ((StageDag) crdag).finishTasks(finishedTasks.get(crdag.dagId), false);

        if (thisDagFinished) {
          Output.debugln(DEBUG, "DAG:" + crdag.dagId + " finished at time:" + Simulator.CURRENT_TIME);
          runningBatchJobs.remove(crdag);
          completedJobs.add(crdag);

          QUEUE_LIST.addCompletionJob2Queue(crdag, crdag.getQueueName());

          iter.remove();
          someDagFinished = true;
        }
      }
    }
    return someDagFinished; // return true if one of the running jobs are
                            // finished.
  }
  
  boolean updateJobsStatusDynamicQueues(Map<Integer, List<Integer>> finishedTasks) {
    boolean someDagFinished = false;
    if (!finishedTasks.isEmpty()) {
      Iterator<BaseDag> iter = runningJobs.iterator();
      while (iter.hasNext()) {
        BaseDag crdag = iter.next();
        if (finishedTasks.get(crdag.dagId) == null) {
          continue;
        }

        Output.debugln(DEBUG, "DAG:" + crdag.dagId + ": " + finishedTasks.get(crdag.dagId).size()
            + " tasks finished at time:" + Simulator.CURRENT_TIME);
        boolean thisDagFinished = ((StageDag) crdag).finishTasks(finishedTasks.get(crdag.dagId), false);

        if (thisDagFinished) {
          Output.debugln(DEBUG, "DAG:" + crdag.dagId + " finished at time:" + Simulator.CURRENT_TIME);
          runningBatchJobs.remove(crdag);
          completedJobs.add(crdag);

          QUEUE_LIST.addCompletionJob2Queue(crdag, crdag.getQueueName());

          iter.remove();
          someDagFinished = true;
          
          if (crdag.getQueueName().equals("user1_0")) {
            completedJobCnt++;
            if (completedJobCnt==Globals.INTERVAL_JOB_NUM){
              ADD_MORE = true;
              System.out.println("ADD MORE at " + Simulator.CURRENT_TIME);
              completedJobCnt = 0;
              user1_q_num = Math.min(++user1_q_num, Globals.USER1_Q_NUM);
              user2_q_num = Math.min(++user2_q_num, Globals.USER2_Q_NUM);
            }
          }
          
          if (user2_q_num==Globals.USER2_Q_NUM && runningJobs.isEmpty()){
            IS_STOP = true;
          }
        }
      }
    }
    return someDagFinished; // return true if one of the running jobs are
                            // finished.
  }

  boolean handleNewJobArrival4MultiQueues() {
    // flag which specifies if jobs have inter-arrival times or starts at t=0
    Output.debugln(DEBUG,
        "handleNewJobArrival; currentTime:" + Simulator.CURRENT_TIME + " nextTime:" + nextTimeToLaunchJob);

    if (runnableJobs.isEmpty()) {
      return false;
    }

    // for batch jobs.
    // start all batch jobs at time = 0
    if (Globals.BATCH_JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.All) {
      Set<BaseDag> newlyStartedJobs = new HashSet<BaseDag>();
      for (BaseDag newJob : runnableJobs) {
        if (!newJob.getQueue().isInteractive) {
          newlyStartedJobs.add(newJob);
          Simulator.QUEUE_LIST.addRunningJob2Queue(newJob, newJob.getQueueName());
          newJob.jobStartTime = Simulator.CURRENT_TIME;
        }
        Output.debugln(DEBUG, "Started job:" + newJob.dagId + " at time:" + Simulator.CURRENT_TIME);
      }
      runnableJobs.removeAll(newlyStartedJobs);
      runningJobs.addAll(newlyStartedJobs);
      runningBatchJobs.addAll(newlyStartedJobs);
    } else if (Globals.BATCH_JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.Trace) {
      Set<BaseDag> newlyStartedJobs = new HashSet<BaseDag>();
      for (BaseDag dag : runnableJobs) {
        if (!dag.getQueue().isInteractive) {
          if (dag.arrivalTime == Simulator.CURRENT_TIME) {
            dag.jobStartTime = Simulator.CURRENT_TIME;
            // dag.jobStartRunningTime = dag.jobStartTime;
            newlyStartedJobs.add(dag);
            Simulator.QUEUE_LIST.addRunningJob2Queue(dag, dag.getQueueName());
            Output.debugln(DEBUG, "Started job:" + dag.dagId + " at time:" + Simulator.CURRENT_TIME);
          }
        }
      }
      // clear the datastructures
      runnableJobs.removeAll(newlyStartedJobs);
      runningJobs.addAll(newlyStartedJobs);
      runningBatchJobs.addAll(newlyStartedJobs);
    } else {
      System.err.println("BATCH_JOBS_ARRIVAL_POLICY: " + Globals.BATCH_JOBS_ARRIVAL_POLICY);
    }

    // for bursty jobs
    if (Globals.BURSTY_JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.Trace) {
      Set<BaseDag> newlyStartedJobs = new HashSet<BaseDag>();
      for (BaseDag dag : runnableJobs) {
        if (dag.getQueue().isInteractive) {
          if (dag.arrivalTime == Simulator.CURRENT_TIME) {
            dag.jobStartTime = Simulator.CURRENT_TIME;
            // dag.jobStartRunningTime = dag.jobStartTime;
            newlyStartedJobs.add(dag);
            StageDag reBurtyJob = StageDag.clone((StageDag) dag);
            reBurtyJob.dagId = burstyJobIdx++;
            runnableJobs.add(reBurtyJob);
            Simulator.QUEUE_LIST.addRunningJob2Queue(dag, dag.getQueueName());
            Output.debugln(DEBUG, "Started job:" + dag.dagId + " at time:" + Simulator.CURRENT_TIME);
          }
        }
        // clear the datastructures
        runnableJobs.removeAll(newlyStartedJobs);
        runningJobs.addAll(newlyStartedJobs);
      }
    } else if (Globals.BURSTY_JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.Period) {
      if (nextTimeToLaunchJob == Simulator.CURRENT_TIME) {
        BaseDag burstJob = null;
        for (BaseDag newJob : runnableJobs) {
          if (newJob.getQueue().isInteractive) {
            assert newJob != null;
            newJob.jobStartTime = Simulator.CURRENT_TIME;
            // newJob.jobStartRunningTime = newJob.jobStartTime;
            runningJobs.add(newJob);
            Simulator.QUEUE_LIST.addRunningJob2Queue(newJob, newJob.getQueueName());
            burstJob = newJob;
            nextTimeToLaunchJob = Simulator.CURRENT_TIME + Globals.PERIODIC_INTERVAL;
            Output.debugln(DEBUG, "Started job:" + newJob.dagId + " at time:" + Simulator.CURRENT_TIME
                + " next job arrives at time:" + nextTimeToLaunchJob);
            break;
          }
        }
        if (burstJob != null) {
          runnableJobs.remove(burstJob);
          StageDag reBurtyJob = StageDag.clone((StageDag) burstJob);
          reBurtyJob.dagId = burstyJobIdx++;
          runnableJobs.add(reBurtyJob);
        }
      }

    } else {
      System.err.println("BURSTY_JOBS_ARRIVAL_POLICY: " + Globals.BURSTY_JOBS_ARRIVAL_POLICY);
    }

    return true;
  }
  
  boolean handleNewJobArrival4DynamicQueues() {
    // flag which specifies if jobs have inter-arrival times or starts at t=0
    Output.debugln(DEBUG,
        "handleNewJobArrival; currentTime:" + Simulator.CURRENT_TIME + " nextTime:" + nextTimeToLaunchJob);

    if (runnableJobs.isEmpty()) {
      return false;
    }

    if (Globals.JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.JobPeriod) {
      
      if (ADD_MORE){
        Set<BaseDag> newlyStartedJobs = new HashSet<BaseDag>();
        // add jobs for user1
        for (int i=0; i<user1_q_num; i++) {
          int jobCount = 0;
          for (BaseDag newJob : runnableJobs) {
            String queueName="user1_"+i;
            if (newJob.getQueueName().equals(queueName)) {
              newlyStartedJobs.add(newJob);
              Simulator.QUEUE_LIST.addRunningJob2Queue(newJob, newJob.getQueueName());
              newJob.jobStartTime = Simulator.CURRENT_TIME;
              jobCount++;
              if(jobCount==Globals.INTERVAL_JOB_NUM)
                break;
            }
          }
        }
        // add jobs for user2
        for (int i=0; i<user2_q_num; i++) {
          int jobCount = 0;
          for (BaseDag newJob : runnableJobs) {
            String queueName="user2_"+i;
            if (newJob.getQueueName().equals(queueName)) {
              newlyStartedJobs.add(newJob);
              Simulator.QUEUE_LIST.addRunningJob2Queue(newJob, newJob.getQueueName());
              newJob.jobStartTime = Simulator.CURRENT_TIME;
              jobCount++;
              if(jobCount==Globals.INTERVAL_JOB_NUM)
                break;
            }
          }
        }
        
        runnableJobs.removeAll(newlyStartedJobs);
        runningJobs.addAll(newlyStartedJobs);
        ADD_MORE = false;
      }
    } else if (Globals.JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.Trace) {
      Set<BaseDag> newlyStartedJobs = new HashSet<BaseDag>();
      for (BaseDag dag : runnableJobs) {
        if (dag.arrivalTime == Simulator.CURRENT_TIME) {
          dag.jobStartTime = Simulator.CURRENT_TIME;
          // dag.jobStartRunningTime = dag.jobStartTime;
          newlyStartedJobs.add(dag);
          Simulator.QUEUE_LIST.addRunningJob2Queue(dag, dag.getQueueName());
          Output.debugln(DEBUG, "Started job:" + dag.dagId + " at time:" + Simulator.CURRENT_TIME);
        }
      }
      // clear the datastructures
      runnableJobs.removeAll(newlyStartedJobs);
      runningJobs.addAll(newlyStartedJobs);
      runningBatchJobs.addAll(newlyStartedJobs);
    } else {
      System.err.println("JOBS_ARRIVAL_POLICY: " + Globals.JOBS_ARRIVAL_POLICY);
    }

    return true;
  }

  boolean handleNewJobCompleted() {
    int currCompletedJobs = completedJobs.size();
    if (lastCompletedJobs < currCompletedJobs) {
      lastCompletedJobs = currCompletedJobs;
      return true;
    }
    return false;
  }

  public static StageDag getDag(int dagId) {
    for (BaseDag dag : Simulator.runningJobs) {
      if (dag.dagId == dagId) {
        return (StageDag) dag;
      }
    }
    return null;
  }

}
