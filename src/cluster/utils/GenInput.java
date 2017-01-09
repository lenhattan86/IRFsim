package cluster.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import cluster.data.SessionData;
import cluster.datastructures.BaseDag;
import cluster.datastructures.Dependency;
import cluster.datastructures.Resource;
import cluster.datastructures.Session;
import cluster.datastructures.Sessions;
import cluster.datastructures.Stage;
import cluster.datastructures.StageDag;
import cluster.simulator.Main.Globals;

public class GenInput {

  public static double[] NaN_1 = {};
  public static double[][] NaN_2 = {};
  public static double weight = 1.0;
  public static String queueFile = "input_gen/queue_input";
  public static String jobFile = "input_gen/jobs_input";

  public static Randomness rand = new Randomness();

  // test geninput
  public static void main(String[] args) {

    writeTaskDurationStatistics("workload/queries_bb_FB_distr.txt",
        "pdf/" + "queries_bb_FB_distr.csv");
    writeTaskDurationStatistics("workload/queries_tpcds_FB_distr_new.txt",
        "pdf/" + "queries_tpcds_FB_distr_new.csv");
    writeTaskDurationStatistics("workload/queries_tpch_FB_distr.txt",
        "pdf/" + "queries_tpch_FB_distr.csv");

    int numInteractiveQueues = 1, numInteractiveJobsPerQueue = 1;
    int numInteractiveTask = 200, numBatchQueues = 3, numBatchJobsPerQueue = 1;
    // genInput(numInteractiveQueues, numInteractiveJobsPerQueue,
    // numInteractiveTask, numBatchQueues, numBatchJobsPerQueue);

    Globals.NUM_DIMENSIONS = 2;
    Queue<BaseDag> jobs = readWorkloadTrace(
        "input_gen/" + "jobs_input_1_4.txt");
    Queue<BaseDag> subJobs = new LinkedList<BaseDag>();
    subJobs.add(jobs.poll());
    genInputFromWorkload(numInteractiveQueues, numInteractiveJobsPerQueue,
        numInteractiveTask, numBatchQueues, numBatchJobsPerQueue, subJobs);

  }

  public static void genQueueInput(int numInteractiveQueues,
      int numBatchQueues) {
    String file = Globals.PathToQueueInputFile;
    Output.write("", false, file);
    
    for (int i = 0; i < numInteractiveQueues; i++) {
      int queueId = i;
      String toWrite = GenInput.genSingleQueueInfo(queueId, "bursty" + queueId,
          weight, true, SessionData.SESSION_DATA.sessionsArray[i]);
      Output.writeln(toWrite, true, file);
    }

    for (int i = 0; i < numBatchQueues; i++) {
      int queueId = i;
      String toWrite = GenInput.genSingleQueueInfo(queueId+numInteractiveQueues, "batch" + queueId,
          weight, false, null);
      Output.writeln(toWrite, true, file);
    }
  }

  public static void genJobInput(int numInteractiveQueues,
      int numInteractiveJobsPerQueue, int numInteractiveTask,
      int numBatchQueues, int numBatchJobsPerQueue) {

    double[] resources1 = { 0.1, 0.1, 0.0, 0.0, 0.0, 0.0 };

    String file = GenInput.jobFile + "_" + numInteractiveQueues + "_"
        + numBatchQueues + ".txt";
    Output.write("", false, file);

    for (int i = 0; i < numInteractiveQueues; i++) {
      int arrivalTime = 0 + i;
      
      for (int j = 0; j < numInteractiveJobsPerQueue; j++) {
        arrivalTime = j * Globals.PERIODIC_INTERVAL + i;
        int jobId = i * numInteractiveJobsPerQueue + j;
        String toWrite = genSingleJobInfo(jobId, "bursty" + i, jobId + "",
            arrivalTime, numInteractiveTask, Globals.STEP_TIME, resources1);
        Output.writeln(toWrite, true, file);
      }
    }

    double[] resources2 = { 0.1, 0.1, 0.0, 0.0, 0.0, 0.0 };

    int batchStartId = Globals.BATCH_START_ID;
    int[] arrivalTimes = readRandomProcess(Globals.DIST_FILE);
    int arrivalIdx = 0;
    for (int j = 0; j < numBatchJobsPerQueue; j++) {
      for (int i = 0; i < numBatchQueues; i++) {
        int jobId = j * numBatchQueues + i + batchStartId;
        String toWrite = genSingleJobInfo(jobId, "batch" + (i), jobId + "",
            arrivalTimes[arrivalIdx++], Globals.numbatchTask, Globals.STEP_TIME,
            resources2);
        // System.out.println(toWrite);
        Output.writeln(toWrite, true, file);
      }
    }
  }

  public static String genSingleJobInfo(int jobId, String queueName,
      String jobName, int arrivalTime, int numOfTasks, double taskDur,
      double[] resources) {
    int numOfStage = 1;
    String str = "";
    str += "# " + jobId + "\n";
    str += "" + numOfStage + " " + jobId + " " + arrivalTime + " " + queueName
        + "\n";
    str += "Stage_0 " + taskDur;
    int dim = Globals.NUM_DIMENSIONS;
    if (Globals.NUM_DIMENSIONS < 2) {
      dim = 2;
    }
    for (int i = 0; i < dim; i++) {
      if (i >= Globals.NUM_DIMENSIONS)
        str += " " + (float) 0.0;
      else
        str += " " + resources[i];
    }
    str += " " + numOfTasks + "\n";
    str += "0";
    return str;
  }

  public static String genSingleJobInfo(int jobId, String queueName,
      StageDag job, int arrivalTime, double taskNumScale, double durScale, int sIdx) {
    String str = "";
    str += "# " + jobId + "\n";
    // TODO: customize the job arrival time
    str += "" + job.numStages + " " + jobId + " " + arrivalTime + " "
        + queueName + " " +sIdx + "\n";
    for (Map.Entry<String, Stage> entry : job.stages.entrySet()) {
      Stage stage = entry.getValue();
      double duration = stage.vDuration * durScale / Globals.STEP_TIME;
      duration = Utils.round(duration, 0) * Globals.STEP_TIME;
      duration = Utils.round(duration, 2);
      duration = Math.max(duration, Globals.STEP_TIME);
      // TODO: hardcode
      // duration = 5.0;
      str += stage.name + " " + duration;

      int dim = Globals.NUM_DIMENSIONS;
      if (Globals.NUM_DIMENSIONS < 2) {
        dim = 2;
      }
      for (int i = 0; i < dim; i++) {
        if (i >= Globals.NUM_DIMENSIONS)
          str += " " + (float) 0.0;
        else
          str += " " + stage.vDemands.resource(i);
      }
      int taskNum = (int) (stage.vids.length() * taskNumScale);
      if (taskNum == 0)
        taskNum = 1;
      str += " " + taskNum + "\n";
    }
    str += job.numEdgesBtwStages;
    for (Map.Entry<String, Stage> entry : job.stages.entrySet()) {
      Stage stage = entry.getValue();
      if (!stage.children.isEmpty()) {
        for (Map.Entry<String, Dependency> child : stage.children.entrySet())
          str += "\n" + stage.name + " " + child.getKey() + " ata";
      }
    }
    return str;
  }

  /*
   * public static String genSingleQueueInfo(int queueId, String queueName,
   * double weight, boolean isLQ, ArrayList<Session> sessions) { double
   * startTime = 0.0; String str = ""; str += "# " + queueId + "\n"; str += "" +
   * queueName + " " + weight + " " + startTime + " " + period + "\n"; int
   * rateLen = rates.length; str += "" + rates.length; if (rateLen > 0) str +=
   * "\n"; for (int i = 0; i < rateLen; i++) { str += "" + durations[i]; for
   * (int j = 0; j < Globals.NUM_DIMENSIONS; j++) str += " " + rates[i][j]; if
   * (i < rateLen - 1) str += "\n"; } return str; }
   */

  public static String genSingleQueueInfo(int queueId, String queueName,
      double weight, boolean isLQ, Sessions sessions) {
    String str = "";
    str += "# " + queueId + "\n";
    if (!isLQ) {
      str += "" + queueName + " 0 \n";
      str += "" + weight;
    } else {
      str += "" + queueName + " 1 \n";
      int numSession = sessions.toList().size();
      str += "" + numSession;
      if (numSession > 0)
        str += "\n";
      for (int i = 0; i < numSession; i++) {
        Session s = sessions.toList().get(i);
        
        str += "" + s.getStartTime();
        str += " " + s.getNumOfJobs();
        str += " " + s.getAlphaDuration();
        str += " " + s.getPeriod();
        for (int j = 0; j < Globals.NUM_DIMENSIONS; j++)
          str += " " + s.getAlpha().resource(j);
        if (i < numSession - 1)
          str += "\n";
      }
    }
    return str;
  }

  public static void genInputFromWorkload(int numInteractiveQueues,
      int numInteractiveJobsPerQueue, int numInteractiveTask,
      int numBatchQueues, int numBatchJobs, Queue<BaseDag> jobs) {

    genQueueInput(numInteractiveQueues, numBatchQueues);

    customizeJobs(numInteractiveQueues, numBatchQueues, numBatchJobs, jobs);
  }

  public static void genInputFromWorkload(int user1QueueNum, int user2QueueNum,
      int intervalJobNum, Queue<BaseDag> jobs) {

    genQueueInput(user1QueueNum, user2QueueNum);

    customizeJobs(user1QueueNum, user2QueueNum, intervalJobNum, jobs);

  }


  private static void customizeJobs(int numInteractiveQueues, int numBatchQueues, int numBatchJobs,
      Queue<BaseDag> jobs) {

    String file = Globals.PathToInputFile;
    Output.write("", false, file);
    // TODO: pick the short jobs for the bursty queue.
    Queue<BaseDag> shortJobs = getJobs(jobs, Globals.SMALL_JOB_DUR_THRESHOLD,
        Globals.SMALL_JOB_TASK_NUM_THRESHOLD, true);
    Iterator<BaseDag> jobIter1 = shortJobs.iterator();
    int newJobId = 0 ;
    if (shortJobs.size() == 0 && numInteractiveQueues > 0) {
      System.err.println("shortJobs jobs are empty.");
      return;
    } else
      for (int i = 0; i < numInteractiveQueues; i++) {
        Sessions ss = SessionData.SESSION_DATA.sessionsArray[i];
        int numberSessions = ss.toList().size();
        for (int sIdx = 0; sIdx <numberSessions; sIdx++){
          
          Session s = ss.toList().get(sIdx);
          int numInteractiveJobsPersession = s.getNumOfJobs();
          
          for (int j = 0; j < numInteractiveJobsPersession; j++) {
            if (jobIter1.hasNext()) {
              StageDag job = (StageDag) jobIter1.next();
              int arrivalTime = (int)s.getStartTime() + j * (int) s.getPeriod();
              String toWrite = genSingleJobInfo(newJobId, "bursty" + (i), job,
                  arrivalTime, Globals.SCALE_UP_BURSTY_JOB,
                  Globals.SCALE_BURSTY_DURATION, sIdx);
              Output.writeln(toWrite, true, file);
              
              newJobId++;
            } else {
              System.err.println("[GenInput] lack of the number of small jobs at "
                  + shortJobs.size());
              jobIter1 = shortJobs.iterator();
              j--;
            }
          }
        }
      }
    
    Queue<BaseDag> longJobs = getJobs(jobs, Globals.LARGE_JOB_MAX_DURATION,
        Globals.LARGE_JOB_TASK_NUM_THRESHOLD, false);
    Iterator<BaseDag> jobIter2 = longJobs.iterator();
    int batchStartId = Globals.BATCH_START_ID;
    int[] arrivalTimes = readRandomProcess(Globals.DIST_FILE);
    int arrivalIdx = 0;
    if (longJobs.size() == 0 && numBatchQueues > 0) {
      System.err.println("long jobs are empty.");
      return;
    }
    if (numBatchQueues == 0) {
      return;
    }
    for (int i = 0; i < numBatchJobs; i++) {
      int batchQueueIdx = i % numBatchQueues;
      int jobIdx = i + batchStartId;
      if (jobIter2.hasNext()) {
        StageDag job = (StageDag) jobIter2.next();
        String toWrite = "";
        if (!Globals.GEN_JOB_ARRIVAL)
          toWrite = genSingleJobInfo(jobIdx, "batch" + (batchQueueIdx), job,
              job.arrivalTime, 1, Globals.SCALE_BATCH_DURATION, 0);
        else {
          if (arrivalIdx >= arrivalTimes.length)
            arrivalIdx = 0;
          toWrite = genSingleJobInfo(jobIdx, "batch" + (batchQueueIdx), job,
              arrivalTimes[arrivalIdx++], Globals.SCALE_UP_BATCH_JOB,
              Globals.SCALE_BATCH_DURATION, 0);
        }
        Output.writeln(toWrite, true, file);
      } else {
        jobIter2 = longJobs.iterator();
      }
    }
  }

  public static int[] readRandomProcess(String filePathStr) {
    int row = 0; // only first line
    int[] res = null;
    File file = new File(filePathStr);
    assert (file.exists() && !file.isDirectory());
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      int rowIdx = 0;
      while ((line = br.readLine()) != null) {
        if (row == rowIdx) {
          String[] args = line.split(",");
          int len = args.length;
          int[] arrivals = new int[len + 1];
          int arrivalTime = 0;
          arrivals[0] = arrivalTime;
          for (int i = 0; i < len; i++) {
            arrivalTime += Integer.parseInt(args[i]);
            arrivals[i + 1] = arrivalTime;
          }
          res = arrivals;
        } else if (rowIdx > row)
          break;
        rowIdx++;
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Catch exception: " + e);
      e.printStackTrace();
    }
    return res;
  }

  public static Queue<BaseDag> getJobs(Queue<BaseDag> jobs, double minComplTime,
      int numOfTasks, boolean isSmall) {
    // TODO: fix this.
    Queue<BaseDag> interactiveJobs = new LinkedList<BaseDag>();
    for (BaseDag job : jobs) {
      // System.out.println(((StageDag) job).viewDag());
      double temp = job.minCompletionTime();
      // System.out.println(job.dagId + " minComplTime: "+temp + "\n");
      if (temp < minComplTime && job.allTasks().size() < numOfTasks && isSmall)
        interactiveJobs.add(job);
      else if (temp > minComplTime && job.allTasks().size() > numOfTasks
          && !isSmall)
        interactiveJobs.add(job);
    }
    return interactiveJobs;
  }

  public static void writeTaskDurationStatistics(String inputFile,
      String outputFile) {
    Queue<BaseDag> jobs = readWorkloadTrace(inputFile);
    FileWriter file = null;
    try {
      file = new FileWriter(outputFile);
      for (BaseDag job : jobs) {
        for (Map.Entry<String, Stage> entry : job.stages.entrySet()) {
          Stage stage = entry.getValue();
          double duration = stage.vDuration;
          String toWrite = "" + duration + "," + stage.vids.length() + "\n";
          file.write(toWrite);
        }
      }
      file.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void genInput(int numInteractiveQueues,
      int numInteractiveJobsPerQueue, int numInteractiveTask,
      int numBatchQueues, int numBatchJobsPerQueue) {
    genQueueInput(numInteractiveQueues, numBatchQueues);
    genJobInput(numInteractiveQueues, numInteractiveJobsPerQueue,
        numInteractiveTask, numBatchQueues, numBatchJobsPerQueue);
  }

  public static Queue<BaseDag> readWorkloadTrace(String workloadFile) {
    Queue<BaseDag> jobs = new LinkedList<BaseDag>();
    jobs = StageDag.readDags(workloadFile); // change the parameters.
    return jobs;
  }
}
