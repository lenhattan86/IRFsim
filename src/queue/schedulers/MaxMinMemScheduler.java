package queue.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Simulator;
import cluster.simulator.Main;
import cluster.simulator.Main.Globals;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;
import cluster.utils.Utils;

public class MaxMinMemScheduler implements Scheduler {
  private static boolean DEBUG = false;

  private String schedulePolicy;
  // Map<String, Resources> resDemandsQueues = null;

  static Resource clusterTotCapacity = null;

  // implementation idea:
  // 1. for every queue, compute it's total resource demand vector

  public MaxMinMemScheduler() {
    clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
    this.schedulePolicy = "MaxMinMemScheduler";
  }

  @Override
  public void computeResShare() {

    int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
    if (numQueuesRuning == 0) {
      return;
    }

    for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
      Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobArrivalComparator());
    }

    // equal share all resources
    maxMinMem(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
  }

  public static void maxMinMem_bk(Resource resCapacity, List<JobQueue> runningQueues) {
    if (Simulator.CURRENT_TIME >= 15.0) DEBUG = true;

    List<JobQueue> activeQueues = new ArrayList<JobQueue>();
    for (JobQueue queue : runningQueues) {
      if (queue.hasRunningJobs()) {
        activeQueues.add(queue);
      }
    }
    if (activeQueues.isEmpty()) return;

    int n = activeQueues.size();

    // step 1: computes the shares
    // Objective function
    double[] f = new double[3 * n + 1];
    f[3 * n] = -1;

    // Inequalities constraints
    double[][] G = new double[3 + n][3 * n + 1];
    // for(int i=0; i<n; i++)
    // G[0][3*i] = G[1][3*i+1] = G[2][3*i+2] = 1.0;
    for (int i = 0; i < n; i++) {
      G[0][3 * i] = G[1][3 * i + 1] = G[2][3 * i + 2] = 1.0;

      G[3 + i][3 * i + 2] = -1;
      G[3 + i][3 * n] = 1;
    }

    double[] h = new double[3 + n];
    for (int i = 0; i < 3; i++)
      h[i] = resCapacity.resource(i);

    // Bounds on variables
    double[] lb = new double[3 * n + 1];
    double[] ub = new double[3 * n + 1];
    // equalities constraints.
    double[][] A = new double[1 * n][3 * n + 1];

    for (int i = 0; i < n; i++) {
      double zi_xi = activeQueues.get(i).getMemToCpuRatio();
      A[i][3 * i + 0] = zi_xi;

      A[i][3 * i + 1] = activeQueues.get(i).getReportBeta() * zi_xi;
      A[i][3 * i + 2] = -1;

      ub[3 * i + 0] = Double.MAX_VALUE; // activeQueues.get(i).getDemand().convertToCPU();
      ub[3 * i + 1] = Double.MAX_VALUE; // activeQueues.get(i).getDemand().convertToGPU();
      ub[3 * i + 2] = activeQueues.get(i).getDemand().getMemory();
    }
    ub[3 * n] = Double.MAX_VALUE;
    double[] b = new double[2 * n];

    // optimization problem
    LPOptimizationRequest or = new LPOptimizationRequest();
    or.setC(f);
    or.setG(G);
    or.setH(h);
    or.setLb(lb);
    // or.setUb(ub);
    or.setA(A);
    or.setB(b);
    or.setDumpProblem(false);
    // optimization
    LPPrimalDualMethod opt = new LPPrimalDualMethod();
    opt.setLPOptimizationRequest(or);
    try {
      opt.optimize();
    } catch (JOptimizerException e) {
      e.printStackTrace();
    }
    double[] sol = opt.getOptimizationResponse().getSolution();
    
    // step 2: allocate the resources.
    double[] memShares = new double[n];
    double[] memSharesSol = new double[n];
    for (int i = 0; i < n; i++) {
      JobQueue q = runningQueues.get(i);
      memShares[i] = q.getResourceUsage().resource(2);
      memSharesSol[i] = sol[3*i+2];
    }

    // Resource computedShares = new Resource(shares);
    Resource allocRes = Simulator.cluster.getClusterAllocatedRes();
    Resource remainingRes = Resources.subtract(clusterTotCapacity, allocRes);
    while (true) {
      // pick up the queue with least mem;
      double minShare = Double.MAX_VALUE;
      int idxMin = -1;
      for (int i = 0; i < n; i++) {
        double memGap = (memShares[i]-memSharesSol[i]);
        if(memGap<minShare && memShares[i]<Double.MAX_VALUE){
          idxMin = i;
          minShare = memGap;
        }
      }
      if(idxMin <0)
        break;
      
      JobQueue q = runningQueues.get(idxMin);
      BaseJob unallocJob = q.getUnallocRunningJob();
      if (unallocJob == null) {
        memShares[idxMin] = Double.MAX_VALUE;
        continue;
      }

      // allocate resource to each task.
      int taskId = unallocJob.getCommingTaskId();
      if (taskId < 0){
        continue;
      }
      
      InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
      Resource gDemand = demand.convertToGPUDemand();
      Resource cDemand = demand.convertToCPUDemand();
      Resource taskDemand = null;
      boolean isCPU = false;
      remainingRes = Resources.subtract(clusterTotCapacity, allocRes);
      if (!gDemand.fitsIn(remainingRes)) {
        if (!cDemand.fitsIn(remainingRes)) {
          memShares[idxMin] = Double.MAX_VALUE;
          continue;
        } else {
          taskDemand = cDemand;
          isCPU = true;
        }
      } else {
        taskDemand = gDemand;
      }

      boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
          unallocJob.duration(taskId), taskDemand);

      if (assigned) {
        allocRes = Resources.sum(allocRes, taskDemand);
        if (isCPU) {
          unallocJob.isCPUUsages.put(taskId, true);
        }

        if (unallocJob.jobStartRunningTime < 0) {
          unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
        }
        memShares[idxMin] += taskDemand.resource(2);
      } else {
        memShares[idxMin] = Double.MAX_VALUE;
      }
    }
  }
  
  public static void maxMinMem(Resource resCapacity, List<JobQueue> runningQueues) {

	    List<JobQueue> activeQueues = new ArrayList<JobQueue>();
	    for (JobQueue queue : runningQueues) {
	      if (queue.hasRunningJobs()) {
	        activeQueues.add(queue);
	      }
	    }
	    if (activeQueues.isEmpty()) return;

	    int n = activeQueues.size();

	    // step 1: computes the shares
	    // Objective function
	    double[] f = new double[3 * n + 1]; // x + y + z + zMin?
	    f[3 * n] = -1;

	    // Inequalities constraints
	    double[][] G = new double[3 + n][3 * n + 1];
	    // for(int i=0; i<n; i++)
	    // G[0][3*i] = G[1][3*i+1] = G[2][3*i+2] = 1.0;
	    
	    
	    double reportMems[] = new double[n];
	    for (int i = 0; i < n; i++) {
	      InterchangableResourceDemand demand = activeQueues.get(i).getReportDemand();
	      reportMems[i] = demand.getMemory()/Math.max(demand.getGpuCpu(), demand.getMemory()); 
	      G[0][3 * i] = G[1][3 * i + 1] = G[2][3 * i + 2] = 1.0; // sum(x), or sum(y), or sum(z) = 1

	      //G[3 + i][3 * i + 2] = -1; G[3 + i][3 * n] = 1; // -z_i + zMin <=0 
	      G[3 + i][3 * i + 2] = -1; G[3 + i][3 * n] = reportMems[i]; // -z_i + t m_i' <= 0;
	    }

	    double[] h = new double[3 + n];
	    for (int i = 0; i < 3; i++)
	      h[i] = resCapacity.resource(i);

	    // Bounds on variables
	    double[] lb = new double[3 * n + 1];
	    double[] ub = new double[3 * n + 1];
	    // equalities constraints.
	    double[][] A = new double[1 * n][3 * n + 1];

	    for (int i = 0; i < n; i++) {
	      double zi_xi = activeQueues.get(i).getReportMemToCpuRatio();
	      A[i][3 * i + 0] = zi_xi;

	      A[i][3 * i + 1] = activeQueues.get(i).getReportBeta() * zi_xi;
	      A[i][3 * i + 2] = -1;

	      ub[3 * i + 0] = Double.MAX_VALUE; // activeQueues.get(i).getDemand().convertToCPU();
	      ub[3 * i + 1] = Double.MAX_VALUE; // activeQueues.get(i).getDemand().convertToGPU();
	      ub[3 * i + 2] = reportMems[i];
	    }
	    ub[3 * n] = Double.MAX_VALUE;
	    double[] b = new double[2 * n];

	    // optimization problem
	    LPOptimizationRequest or = new LPOptimizationRequest();
	    or.setC(f);
	    or.setG(G);
	    or.setH(h);
	    or.setLb(lb);
	    // or.setUb(ub);
	    or.setA(A);
	    or.setB(b);
	    or.setDumpProblem(false);
	    // optimization
	    LPPrimalDualMethod opt = new LPPrimalDualMethod();
	    opt.setLPOptimizationRequest(or);
	    try {
	      opt.optimize();
	    } catch (JOptimizerException e) {
	      e.printStackTrace();
	    }
	    double[] sol = opt.getOptimizationResponse().getSolution();
	    // step 2: allocate the resources.
	    for (int i = 0; i < n; i++) {
	      JobQueue q = activeQueues.get(i);

	      double shares[] = {
	          sol[3 * i + 0],
	          sol[3 * i + 1],
	          sol[3 * i + 2] };
	      
	      QueueScheduler.allocateResToQueue(q, shares);
	    }
	  }

  public static void maxMinMemBK(Resource resCapacity, List<JobQueue> runningQueues) {

    List<JobQueue> activeQueues = new ArrayList<JobQueue>();
    for (JobQueue queue : runningQueues) {
      if (queue.hasRunningJobs()) {
        activeQueues.add(queue);
      }
    }
    if (activeQueues.isEmpty()) return;

    int n = activeQueues.size();

    // step 1: computes the shares
    // Objective function
    double[] f = new double[3 * n + 1];
    f[3 * n] = -1;

    // Inequalities constraints
    double[][] G = new double[3 + n][3 * n + 1];
    // for(int i=0; i<n; i++)
    // G[0][3*i] = G[1][3*i+1] = G[2][3*i+2] = 1.0;
    for (int i = 0; i < n; i++) {
      G[0][3 * i] = G[1][3 * i + 1] = G[2][3 * i + 2] = 1.0;

      G[3 + i][3 * i + 2] = -1;
      G[3 + i][3 * n] = 1;
    }

    double[] h = new double[3 + n];
    for (int i = 0; i < 3; i++)
      h[i] = resCapacity.resource(i);

    // Bounds on variables
    double[] lb = new double[3 * n + 1];
    double[] ub = new double[3 * n + 1];
    // equalities constraints.
    double[][] A = new double[1 * n][3 * n + 1];

    for (int i = 0; i < n; i++) {
      double zi_xi = activeQueues.get(i).getReportMemToCpuRatio();
      A[i][3 * i + 0] = zi_xi;

      A[i][3 * i + 1] = activeQueues.get(i).getReportBeta() * zi_xi;
      A[i][3 * i + 2] = -1;

      ub[3 * i + 0] = Double.MAX_VALUE; // activeQueues.get(i).getDemand().convertToCPU();
      ub[3 * i + 1] = Double.MAX_VALUE; // activeQueues.get(i).getDemand().convertToGPU();
      double reportMem = activeQueues.get(i).getReportDemand().getMemory(); 
      ub[3 * i + 2] = reportMem;
    }
    ub[3 * n] = Double.MAX_VALUE;
    double[] b = new double[2 * n];

    // optimization problem
    LPOptimizationRequest or = new LPOptimizationRequest();
    or.setC(f);
    or.setG(G);
    or.setH(h);
    or.setLb(lb);
    // or.setUb(ub);
    or.setA(A);
    or.setB(b);
    or.setDumpProblem(false);
    // optimization
    LPPrimalDualMethod opt = new LPPrimalDualMethod();
    opt.setLPOptimizationRequest(or);
    try {
      opt.optimize();
    } catch (JOptimizerException e) {
      e.printStackTrace();
    }
    double[] sol = opt.getOptimizationResponse().getSolution();
    // step 2: allocate the resources.
    for (int i = 0; i < n; i++) {
      JobQueue q = activeQueues.get(i);

      double shares[] = {
          sol[3 * i + 0],
          sol[3 * i + 1],
          sol[3 * i + 2] };
      
      QueueScheduler.allocateResToQueue(q, shares);
    }
  }


  @Override
  public String getSchedulePolicy() {
    return this.schedulePolicy;
  }
}
