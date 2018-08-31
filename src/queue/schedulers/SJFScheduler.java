package queue.schedulers;

import java.util.Collections;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.JobArrivalComparator;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.simulator.Simulator;

public class SJFScheduler implements Scheduler {
	private String schedulePolicy;

	static Resource clusterTotCapacity = null;
	static Resource clusterAvailRes = null;
	static double[] L;

	public SJFScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SJF";
	}

	@Override
	public void computeResShare() {
		int numQueuesRuning = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs().size();
		if (numQueuesRuning == 0) {
			return;
		}

		for (JobQueue q : Simulator.QUEUE_LIST.getQueuesWithQueuedJobs()) {
			Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobArrivalComparator());
		}

		clusterAvailRes = Simulator.cluster.getClusterResAvail();
		List<JobQueue> activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		
		while(clusterAvailRes.resource(1)>0 && activeQueues.size() > 0){
			AlloXScheduler.online_allox(clusterTotCapacity, activeQueues, 1);
			clusterAvailRes = Simulator.cluster.getClusterResAvail();
			activeQueues = Simulator.QUEUE_LIST.getQueuesWithQueuedJobs();
		}
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
