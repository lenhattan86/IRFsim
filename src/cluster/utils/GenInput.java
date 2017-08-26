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
import cluster.datastructures.BaseJob;
import cluster.datastructures.Dependency;
import cluster.datastructures.Resource;
import cluster.datastructures.Session;
import cluster.datastructures.Sessions;
import cluster.datastructures.SubGraph;
import cluster.datastructures.MLJob;
import cluster.simulator.Main.Globals;

public class GenInput {
  private static int stageIter = 0;
  public static double[] NaN_1 = {};
  public static double[][] NaN_2 = {};
  public static double weight = 1.0;
  public static String queueFile = "input_gen/queue_input";
  public static String jobFile = "input_gen/jobs_input";

  public static Randomness rand = new Randomness();

  /*
   * // test geninput public static void main(String[] args) {
   * 
   * writeTaskDurationStatistics("workload/queries_bb_FB_distr.txt", "pdf/" +
   * "queries_bb_FB_distr.csv");
   * writeTaskDurationStatistics("workload/queries_tpcds_FB_distr_new.txt",
   * "pdf/" + "queries_tpcds_FB_distr_new.csv");
   * writeTaskDurationStatistics("workload/queries_tpch_FB_distr.txt", "pdf/" +
   * "queries_tpch_FB_distr.csv");
   * 
   * int numInteractiveQueues = 1, numInteractiveJobsPerQueue = 1; int
   * numInteractiveTask = 200, numBatchQueues = 3, numBatchJobsPerQueue = 1; //
   * genInput(numInteractiveQueues, numInteractiveJobsPerQueue, //
   * numInteractiveTask, numBatchQueues, numBatchJobsPerQueue);
   * 
   * Globals.NUM_DIMENSIONS = 2; Queue<BaseDag> jobs = readWorkloadTrace(
   * "input_gen/" + "jobs_input_1_4.txt"); Queue<BaseDag> subJobs = new
   * LinkedList<BaseDag>(); subJobs.add(jobs.poll());
   * genInputFromWorkload(numInteractiveQueues, numInteractiveJobsPerQueue,
   * numInteractiveTask, numBatchQueues, numBatchJobsPerQueue, subJobs);
   * 
   * }
   */

  public static void genQueueInput(int numInteractiveQueues,
      int numBatchQueues) {
    String file = Globals.PathToQueueInputFile;
    Output.write("", false, file);

    for (int i = 0; i < numInteractiveQueues; i++) {
      int queueId = i;
      String toWrite = GenInput.genSingleQueueInfo(queueId, "bursty" + queueId,
          weight, true, Globals.SESSION_DATA.sessionsArray[i]);
      Output.writeln(toWrite, true, file);
    }

    for (int i = 0; i < numBatchQueues; i++) {
      int queueId = i;
      String toWrite = GenInput.genSingleQueueInfo(
          queueId + numInteractiveQueues, "batch" + queueId, weight, false,
          null);
      Output.writeln(toWrite, true, file);
    }
  }
  
  public static void genQueueInput(int numBatchQueues) {
    String file = Globals.PathToQueueInputFile;
    Output.write("", false, file);

    for (int i = 0; i < numBatchQueues; i++) {
      int queueId = i;
      String toWrite = GenInput.genSingleQueueInfo(
          queueId, "queue" + queueId, weight, false,
          null);
      Output.writeln(toWrite, true, file);
    }
  }

  public static void genJobInput(int numBatchQueues, int numBatchJobsPerQueue) {

    String file = GenInput.jobFile + "_"
        + numBatchQueues + ".txt";
    Output.write("", false, file);

    double[] resources2 = { 0.1, 0.1, 0.0, 0.0, 0.0, 0.0 };

    int batchStartId = Globals.JOB_START_ID;
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
      MLJob job, int arrivalTime, double taskNumScale, double durScale,
      boolean isUncertain) {
    String str = "";
    str += "# " + jobId + "\n";
    // TODO: customize the job arrival time
    str += "" + job.numStages + " " + jobId + " " + job.NUM_ITERATIONS + " "+ arrivalTime + " "
        + queueName + "\n";
    for (Map.Entry<String, SubGraph> entry : job.stages.entrySet()) {
      SubGraph stage = entry.getValue();

      double uncertainDur = 0.0;
      if (isUncertain) {
        int len = SessionData.DUR_ERROR_10.length;
        double error = SessionData.DUR_ERROR_10[stageIter % len]
            * Globals.ESTIMASION_ERRORS / 0.1;
        error = Math.min(Math.max(error, -1), 1);
        uncertainDur = stage.vDuration * error;
        uncertainDur = Utils.round(uncertainDur, 2);
      }

      double duration = (stage.vDuration + uncertainDur) * durScale
          / Globals.STEP_TIME;
      duration = Utils.round(duration, 0) * Globals.STEP_TIME;
      duration = Utils.round(duration, 2);
      duration = Math.max(duration, Globals.STEP_TIME);
      if (durScale <= 0)
        duration = Globals.STEP_TIME;
      // TODO: hardcode
      // duration = 5.0;
      str += stage.name + " " + duration;

      for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
          double uncertainRes = 0.0;
          if (isUncertain) {
            int len = SessionData.RES_ERROR_10.length;
            // System.out.println("val:"+SessionData.RES_ERROR_10[stageIter%len][i]);
            double error = SessionData.RES_ERROR_10[stageIter % len][i]
                * Globals.ESTIMASION_ERRORS / 0.1;
            error = Math.min(Math.max(error, -1), 1);
            uncertainRes = stage.vDemands.resource(i) * error;
            uncertainRes = Utils.round(uncertainRes, 2);
          }
          str += " "
              + Utils.round(stage.vDemands.resource(i) + uncertainRes, 2);
      }
      str += " " + stage.getBeta();
      int taskNum = (int) (stage.vids.length() * taskNumScale);
      if (taskNum == 0)
        taskNum = 1;
      str += " " + taskNum + "\n";
      stageIter++;
    }
    str += job.numEdgesBtwStages;
    for (Map.Entry<String, SubGraph> entry : job.stages.entrySet()) {
      SubGraph stage = entry.getValue();
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
      double weight, boolean isLQ, Session s) {
    String str = "";
    str += "# " + queueId + "\n";
    if (!isLQ) {
      str += "" + queueName + " 0 0.0 \n";
      str += "" + weight;
    } else {
      str += "" + queueName + " 1 " + s.getStartTime() + " \n";
      int numOfJobs = s.getNumOfJobs();
      str += "" + numOfJobs + "\n";

      for (int i = 0; i < numOfJobs; i++) {
        str += "" + s.getAlphaDurations()[i];
        str += " " + s.getPeriods()[i];
        for (int k = 0; k < Globals.NUM_DIMENSIONS; k++)
          str += " " + s.getAlphas()[i].resource(k);
        /*
         * if (i<=2) System.out.println(s.getAlphas()[i]);
         */ if (i < numOfJobs - 1)
          str += "\n";
      }
    }
    return str;
  }

  public static void genInputFromWorkload(int numBatchQueues, int numBatchJobs, Queue<BaseJob> jobs) {

    genQueueInput(numBatchQueues);

    customizeJobs(numBatchQueues, numBatchJobs, jobs);
  }

  private static void customizeJobs(int numBatchQueues, int numBatchJobs, Queue<BaseJob> jobs) {
    stageIter = 0;
    String file = Globals.PathToInputFile;
    Output.write("", false, file);

    Iterator<BaseJob> jobIter2 = jobs.iterator();
    int batchStartId = Globals.JOB_START_ID;
    int[] arrivalTimes = readRandomProcess(Globals.DIST_FILE);
    int arrivalIdx = 0;
    if (jobs.size() == 0 && numBatchQueues > 0) {
      System.err.println("jobs are empty.");
      return;
    }
    if (numBatchQueues == 0) {
      return;
    }
    for (int i = 0; i < numBatchJobs; i++) {
      int batchQueueIdx = i % numBatchQueues;
      int jobIdx = i + batchStartId;
      if (jobIter2.hasNext()) {
        MLJob job = (MLJob) jobIter2.next();
        String toWrite = "";
        if (!Globals.GEN_JOB_ARRIVAL)
          toWrite = genSingleJobInfo(jobIdx, "queue" + (batchQueueIdx), job,
              job.arrivalTime, 1, Globals.SCALE_BATCH_DURATION, false);
        else {
          if (arrivalIdx >= arrivalTimes.length)
            arrivalIdx = 0;
          toWrite = genSingleJobInfo(jobIdx, "queue" + (batchQueueIdx), job,
              arrivalTimes[arrivalIdx++], Globals.SCALE_UP_BATCH_JOB,
              Globals.SCALE_BATCH_DURATION, false);
        }
        Output.writeln(toWrite, true, file);
      } else {
        jobIter2 = jobs.iterator();
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

  public static Queue<BaseJob> getJobs(Queue<BaseJob> jobs, double minComplTime,
      int numOfTasks, boolean isSmall) {
    // TODO: fix this.
    Queue<BaseJob> interactiveJobs = new LinkedList<BaseJob>();
    for (BaseJob job : jobs) {
      // System.out.println(((StageDag) job).viewDag());
      double temp = job.minCompletionTime();
      double longestTaskDuration = job.getLongestTaskDuration();

      if (Globals.LONG_DURATION_TASK_TOBE_REMOVED > 0
          && !isSmall && longestTaskDuration > Globals.LONG_DURATION_TASK_TOBE_REMOVED  )
        continue; // skip this job because the task is too long.

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
    Queue<BaseJob> jobs = readWorkloadTrace(inputFile);
    FileWriter file = null;
    try {
      file = new FileWriter(outputFile);
      for (BaseJob job : jobs) {
        for (Map.Entry<String, SubGraph> entry : job.stages.entrySet()) {
          SubGraph stage = entry.getValue();
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

  public static double avgTaskDuration(Queue<BaseJob> jobs) {
    double avgDuration = 0.0;
    double numOfTasks = 0.0;
    for (BaseJob job : jobs) {
      for (Map.Entry<String, SubGraph> entry : job.stages.entrySet()) {
        SubGraph stage = entry.getValue();
        double duration = stage.vDuration;
        avgDuration += duration * stage.vids.length();
        numOfTasks += stage.vids.length();
      }
    }
    avgDuration /= numOfTasks;
    return avgDuration;
  }

  public static void genInput(int numBatchQueues, int numBatchJobsPerQueue) {
    genQueueInput(numBatchQueues);
    genJobInput(numBatchQueues, numBatchJobsPerQueue);
  }

  public static Queue<BaseJob> readWorkloadTrace(String workloadFile) {
    Queue<BaseJob> jobs = new LinkedList<BaseJob>();
    jobs = MLJob.readDags(workloadFile); // change the parameters.
    return jobs;
  }
}
