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
import cluster.simulator.Main.Globals.QueueSchedulerPolicy;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;
import cluster.utils.Utils;

public class SpeedUpScheduler implements Scheduler {
	private static boolean DEBUG = false;

	private String schedulePolicy;
	// Map<String, Resources> resDemandsQueues = null;

	static Resource clusterTotCapacity = null;

	// implementation idea:
	// 1. for every queue, compute it's total resource demand vector

	public SpeedUpScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SpeedUp";
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
		speedUp(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
	}
	
	public static void speedUp(Resource resCapacity, List<JobQueue> runningQueues) {
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
	    
	    double ri[] = new double[n];
	    double bi[] = new double[n];
	    for (int i = 0; i < n; i++) {
	      double beta = activeQueues.get(i).getReportBeta();
	      ri[i] = activeQueues.get(i).getReportMemToCpuRatio();
	      bi[i] = Double.min((1+beta)/n, 1/(ri[i]*n));
	    }
	    
	 // equalities constraints.
      double[][] A = new double[1 * n][3 * n + 1];

      for (int i = 0; i < n; i++) {
        A[i][3 * i + 0] = 1; // xi
        A[i][3 * i + 1] = activeQueues.get(i).getReportBeta(); // yi
        A[i][3 * i + 2] = -bi[i]; // ki
        
        A[i][3 * n] = 0; // k*
      }
      double[] b = new double[1 * n];

	    // Inequalities constraints
	    double[][] G = new double[2 + 2*n][3 * n + 1];
	    double[] h = new double[2 + 2*n];
	    h[0]=1;
	    h[1]=1;
	    for (int i = 0; i < n; i++) {
	      G[0][3 * i] = G[1][3 * i + 1] = 1.0; // sum xi, sum yi
	      
	      for (int j=0; j<n; j++)
	        G[2 + i][3 * j + 2] = bi[j]*ri[j];
	      h[2 + i] = 1;
	      
	      G[2 + n+ i][3 * i + 2] = -1;
	      G[2 + n+ i][3 * n] = 1;
	    }
	    
	    // Bounds on variables
	    double[] lb = new double[3 * n + 1];
	    double[] ub = new double[3 * n + 1];
	    // optimization problem
	    LPOptimizationRequest or = new LPOptimizationRequest();
	    or.setC(f);
	    or.setG(G);
	    or.setH(h);
	    or.setLb(lb);
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
	          sol[3 * i + 0]*clusterTotCapacity.resource(0),
	          sol[3 * i + 1]*clusterTotCapacity.resource(1),
	          sol[3 * i + 2]*bi[i]*ri[i]*clusterTotCapacity.resource(2) };
	      
	      QueueScheduler.allocateResToQueue(q, shares);
	    }
	  }
	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
