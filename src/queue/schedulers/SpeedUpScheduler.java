package queue.schedulers;

import java.util.Collections;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.simulator.Main;
import cluster.simulator.Main.Globals;
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
	
	public static void maxMinMem(Resource resCapacity, List<JobQueue> runningQueues) {
	  if(Simulator.CURRENT_TIME==0.0)
	    DEBUG=true;
	  
	  if(runningQueues.isEmpty()) return;
	  Resource allocRes = Simulator.cluster.getClusterAllocatedRes();
	  Resource remainResource = Resources.subtract(resCapacity,allocRes);
	  int numOfQueues = runningQueues.size();
	  double[] memShares = new double[numOfQueues];
	  int i=0;
	  for (JobQueue queue : runningQueues) {
      memShares[i] = queue.getResourceUsage().resource(Globals.NUM_DIMENSIONS-1);
      i++;
    }
	  
    boolean resAvail = true;    
    int numAllocQueues = 0;
    while(resAvail && numAllocQueues < numOfQueues){
      // step 1: pick user with min memory share
      double minMem = Double.MAX_VALUE;
      i=0; int minIdx =0;
      JobQueue q=runningQueues.get(0);
      for (; i<numOfQueues; i++) {
        if(memShares[i]<=minMem){
          q = runningQueues.get(i);
          minIdx=i;
          minMem = memShares[i];
        }
      }
      
      // pick the task with min beta
      BaseJob unallocJob = q.getUnallocRunningJob();
      if(unallocJob==null) {
        memShares[minIdx] = Double.MAX_VALUE;
        numAllocQueues++;
        continue;
      }
      
      int taskId = unallocJob.getCommingTaskId();
      InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
      Resource gDemand = demand.convertToGPUDemand();
      Resource cDemand = demand.convertToCPUDemand();
      Resource taskDemand = null;
      
      boolean isCPU = false;
      if(!gDemand.fitsIn(remainResource)) {
        if(!cDemand.fitsIn(remainResource)){
          memShares[minIdx] = Double.MAX_VALUE;
          numAllocQueues++;
          break;
        }else{
          taskDemand = cDemand;
          isCPU=true;
        }
      } else{
        taskDemand = gDemand;
      }
      
      boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
          unallocJob.duration(taskId), taskDemand);
      
      if(assigned){
        remainResource = Resources.subtract(remainResource, taskDemand);
        memShares[minIdx]+= taskDemand.resource(Globals.NUM_DIMENSIONS-1);
        if(isCPU){
          unallocJob.isCPUUsages.put(taskId, true);
        }
          
        if (unallocJob.jobStartRunningTime<0){
          unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
        }
      } 
    }
  }

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
