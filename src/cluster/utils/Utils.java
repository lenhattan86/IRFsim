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

//import com.joptimizer.exception.JOptimizerException;
//import com.joptimizer.optimizers.BIPLokbaTableMethod;
//import com.joptimizer.optimizers.BIPOptimizationRequest;
//import com.joptimizer.optimizers.LPOptimizationRequest;
//import com.joptimizer.optimizers.LPPrimalDualMethod;
//import com.mathworks.engine.EngineException;
//import com.mathworks.engine.MatlabEngine;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cluster.datastructures.BaseJob;
import cluster.datastructures.SubGraph;
import cluster.datastructures.MLJob;
import cluster.datastructures.Resource;
import cluster.simulator.Main;
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
    Queue<BaseJob> inputJobs = MLJob.readDags(Globals.PathToInputFile);
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
  
//  public static double[] linprog(double[] f, double[][] A, double[] b, double[][] Aeq, double[] beq, double[] lb, double[] ub){
//		LPOptimizationRequest or = new LPOptimizationRequest(); 
//		or.setC(f);
//		if(A!=null){
//			or.setG(A);
//			or.setH(b);
//		}
//		if(Aeq!=null){
//			or.setA(Aeq);
//			or.setB(beq);
//		}
//		if(lb!=null)
//			or.setLb(lb);
//		if(ub!=null)
//			or.setUb(ub);
//		
//		or.setDumpProblem(true);
//		LPPrimalDualMethod opt = new LPPrimalDualMethod();
//		opt.setLPOptimizationRequest(or);
//		try {
//			opt.optimize();
//		} catch (JOptimizerException e) {
//			e.printStackTrace();
//		}
//		return opt.getOptimizationResponse().getSolution();
//	}
//  
//	
//	public static int[] biprog_joptimizer(double[] c, double[][] A, double[] b, double[][] Aeq, double[] beq){
//		DoubleMatrix1D C = DoubleFactory1D.dense.make(c);
//		DoubleMatrix2D Amat = DoubleFactory2D.dense.make(A);
//		
//		DoubleMatrix1D B = DoubleFactory1D.dense.make(b);
//		
//	
//		DoubleMatrix2D AeqMat = DoubleFactory2D.dense.make(Aeq);
//		DoubleMatrix1D Beq = DoubleFactory1D.dense.make(beq);
//		
//		BIPOptimizationRequest or = new BIPOptimizationRequest();
//		or.setC(C);
//		
//		or.setG(Amat);
//		or.setH(B);
//		
//		or.setA(AeqMat);
//		or.setB(Beq);
//		
//		or.setDumpProblem(true);
//		int[] sol = null;
//		//optimization
//		BIPLokbaTableMethod opt = new BIPLokbaTableMethod();
//		opt.setBIPOptimizationRequest(or);
//		try {
//				opt.optimize();
//				sol = opt.getBIPOptimizationResponse().getSolution();
//		} catch (JOptimizerException e) {
//			e.printStackTrace();
//		}
//		return sol;
//	}
	
	
	
//	public static int[] biprog_matlab(double[] c, double[][] A, double[] b, double[][] Aeq, double[] beq){
//		double[] intcon = new double[c.length];
//		double[] lb = new double[c.length];
//		double[] ub = new double[c.length];
//		for (int i = 0; i< intcon.length; i++){
//			intcon[i] = i+1;
//			ub[i] = 1;
//			lb[i] = 0;
//		}
//		
//
//		int[] sols = new int[c.length];
//		double[] temp;
//		Object results;
//		try {
//			results = Globals.MATLAB.feval("intlinprog", MatlabEngine.NULL_WRITER,  MatlabEngine.NULL_WRITER, c, intcon, A, b, Aeq, beq, lb, ub);
////			results = Globals.MATLAB.feval("intlinprog",  c, intcon, A, b, Aeq, beq, lb, ub);
//			temp = (double[]) results;
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//		
//		if (temp==null)
//			return null;
//		
//		for (int i = 0; i< sols.length; i++)
//			sols[i] = (int) Math.round(temp[i]);
//			
//		return sols;
//	}
  public static double sum(double[] values){
  	int n = values.length;
  	double res = 0;
  	for (int i=0; i<n; i++){
  		res += values[i];
  	}
  	return res;
  }
  
  public static double[] sum(double[] a, double[] b){
  	int n = a.length;
  	double[] res = new double[n];
  	for (int i=0; i<n; i++){
  		res[i] = a[i] + b[i];
  	}
  	return res;
  }
  
  public static double[] substract(double[] a, double[] b){
  	int n = a.length;
  	double[] res = new double[n];
  	for (int i=0; i<n; i++){
  		res[i] = a[i] - b[i];
  	}
  	return res;
  }
  
  public static double[] sum(double[][] values){
  	int n = values.length;
  	int m = values[0].length;
  	double[] res = new double[n];
  	for (int i=0; i<n; i++){
  		res[i] = Utils.sum(values[i]);
  	}
  	return res;
  }
  
  public static double maxArray(double[] values){
  	int n = values.length;
  	double res = Double.MIN_VALUE;
  	for (int i=0; i<n; i++){
  		if(res < values[i])
  			res = values[i];
  	}
  	return res;
  }
  
  public static int maxArray(int[] values){
  	int n = values.length;
  	int res = Integer.MIN_VALUE;
  	for (int i=0; i<n; i++){
  		if(res < values[i])
  			res = values[i];
  	}
  	return res;
  }
  
//  public static void main(String[] args) {
//		double[] c = new double[] { 1, 4, 0, 7, 0, 0, 8, 6, 0, 4 }; 
//		double[][] A = new double[][] { 
//				{ -3, -1, -4, -4, -1, -5, -4, -4, -1, -1 },
//				{  0,  0, -3, -1, -5, -5, -5, -1,  0, 0 }, 
//				{ -4, -1, -5, -2, -4, -3, -2, -4, -4, 0 },
//				{ -3, -4, -3, -5, -3, -1, -4, -5, -1, -4 } };
//				double[] b = new double[] { 0, -2, -2, -8 };
//		
//				double[][] Aeq = new double[][] { 
//			{ 0, 1, 1, 0, 0, 0, 0, 0, 0, 0 },
//			{  0,  0, 0, 1, 1, 0, 0, 0, 0, 0 }};
//			double[] beq = new double[] { 0, 1};
//  	int[] sol = biprog_joptimizer(c, A, b, Aeq, beq);
//  	double[] Arow = A[0];
//		System.out.println(sol[1] + " " + sol[2]+ " " + sol[3]+ " " + sol[4]);
//	}
  
}
