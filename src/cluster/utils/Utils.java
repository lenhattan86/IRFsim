package cluster.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import cluster.datastructures.BaseJob;
import cluster.datastructures.SubGraph;
import cluster.datastructures.MLJob;
import cluster.datastructures.Resource;
import cluster.simulator.Main.Globals;

public class Utils {

  private static final boolean DEBUG = true;

  // generate a trace
  // read a bunch of dags
  // take a time distribution
  // generate a trace with N number of jobs
  // following the arrival times given
  // either trace or distribution
  // if no time distribution, just N number of jobs
  // otherwise, add the time as well
  public static void generateTrace() {

    int numJobs = 5000;
    Map<Integer, BaseJob> inputJobsMap = new HashMap<Integer, BaseJob>();
    Queue<BaseJob> inputJobs = MLJob.readDags(Globals.PathToInputFile, false, false, false);
    for (BaseJob dag : inputJobs) {
      inputJobsMap.put(dag.dagId, dag);
    }
    int numInputDags = inputJobs.size();
    int nextDagId = 0;

    Randomness r = new Randomness();
    // read the time distribution
    List<Integer> timeline = timeDistribution();
    if (timeline != null) {
      for (Integer time : timeline) {
        // for every time step
        int nextDagIdx = r.pickRandomInt(numInputDags);
        // take a random DAG and write it to file with next dag ID
        BaseJob nextDag = inputJobsMap.get(nextDagIdx);
        nextDag.dagId = nextDagId;
        nextDag.arrivalTime = time;
        nextDagId++;
        Utils.writeDagToFile((MLJob) nextDag, true);
      }
    } else {
      // generate random numJobs jobs in a trace
      while (numJobs > 0) {
        // for every time step
        int nextDagIdx = r.pickRandomInt(numInputDags);
        // take a random DAG and write it to file with next dag ID
        BaseJob nextDag = inputJobsMap.get(nextDagIdx);
        nextDag.dagId = nextDagId;
        nextDagId++;
        Utils.writeDagToFile((MLJob) nextDag, false);
        numJobs--;
      }
    }
  }

  public static List<Integer> timeDistribution() {

    String fileTimeDistribution = Globals.DataFolder + "/FBdistribution.txt";
    File file = new File(fileTimeDistribution);
    if (!file.exists()) return null;

    List<Integer> timeline = new ArrayList<Integer>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        timeline.add(Integer.parseInt(line));
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Catch exception: " + e);
    }

    return timeline;
  }

  public static void writeDagToFile(MLJob dag, boolean considerTimeDistr) {
    File file = new File(Globals.DataFolder + "/" + Globals.FileOutput);
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
      bw.write("# " + dag.dagName + "\n");
      if (considerTimeDistr) {
        bw.write(dag.stages.size() + " " + dag.dagId + " " + dag.arrivalTime + "\n");
      } else {
        bw.write(dag.stages.size() + " " + dag.dagId + " " + "\n");
      }
      for (SubGraph stage : dag.stages.values()) {
        bw.write(stage.name + " " + stage.vDuration + " ");
        double[] resArray = stage.vDemands.convertToResourceArray();
        for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
          bw.write(resArray[i] + " ");
        }
        bw.write(stage.vids.length() + "\n");
      }
      int numEdges = 0;
      for (SubGraph stage : dag.stages.values()) {
        numEdges += stage.children.size();
      }
      bw.write(numEdges + "\n");
      for (SubGraph stage : dag.stages.values()) {
        for (String child : stage.children.keySet()) {
          bw.write(stage.name + " " + child + " " + "ata" + "\n");
        }
      }
      bw.close();
    } catch (Exception e) {
      System.err.println("Catch exception: " + e);
    }
  }

  public static double roundBase(double value, int places) {
    double factor = Math.pow(10.0, places);
    double roundedVal = value;
    roundedVal = roundedVal * factor;
    roundedVal = Math.round(roundedVal);
    roundedVal /= factor;
    return roundedVal;
  }

  public static double roundDefault(double value) {
    return roundBase(value, 6);
  }

  public static int getMinValIdx(double[] nonNegArray) {
    double minVal = Double.MAX_VALUE - 1;
    int idx = -1;
    for (int i = 0; i < nonNegArray.length; i++) {
      if (minVal > nonNegArray[i]) {
        minVal = nonNegArray[i];
        idx = i;
      }
    }
    return idx;
  }

  public static int getMinValIdx(Resource[] nonNegArray, int bottleneckId) {
    double minVal = Double.MAX_VALUE - 1;
    int idx = -1;
    for (int i = 0; i < nonNegArray.length; i++) {
      if (minVal > nonNegArray[i].resource(bottleneckId)) {
        minVal = nonNegArray[i].resource(bottleneckId);
        idx = i;
      }
    }
    return idx;
  }

  public static void createUserDir(final String dirName) {
    final File dir = new File(dirName);
    if (!dir.exists() && !dir.mkdirs()) {
      System.err.println("cannot mkdir " + dirName);
    }
  }
  
  public static double min(double... arrays){
    double minVal = Double.MAX_VALUE;
    for (double arg : arrays)
      if (minVal>arg)
        minVal = arg;
    return minVal;
  }
  
  public static int idxOfMax(double... arrays){
    double maxVal = Double.MIN_VALUE;
    int i=0, idx = 0;
    for (double arg : arrays){
      if (maxVal<arg){
      	maxVal = arg;
      	idx=i;
      }
      i++;
    }
    return idx;
  }
  
  public static double max(double... arrays){
    double maxVal = Double.MIN_VALUE;
    for (double arg : arrays){
      if (maxVal<arg)
      	maxVal = arg;
    }
    return maxVal;
  }
  
  public static double[] multifly(double[] array, double factor){
	  double[] result = array;
	  for (int i=0; i<array.length; i++)
		  result[i] = array[i]*factor;
	  return result;
  }
}
