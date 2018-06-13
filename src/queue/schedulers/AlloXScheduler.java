package queue.schedulers;

import java.util.Collections;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;
import cluster.utils.BetaComparator;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Utils;

public class AlloXScheduler implements Scheduler {
	private String schedulePolicy;

	static Resource clusterTotCapacity = null;
	private static boolean isComputed = false;
	private static Resource computedShares[] = null;

	// implementation idea:
	// 1. for every queue, compute it's total resource demand vector

	public AlloXScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "AlloX";
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

		// allox_heuristic(clusterTotCapacity,
		// Simulator.QUEUE_LIST.getRunningQueues());
		allox(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
	}

	public static void allox(Resource resCapacity, List<JobQueue> runningQueues) {
		// sort queues based on beta
		int numberOfQueues = runningQueues.size();

		double SHARES[][] = { { 3520, 0, 880 }, { 320, 28.57, 217.14 }, { 0, 31.43, 62.86 } };
		for (int i = 0; i < numberOfQueues; i++) {
			JobQueue q = runningQueues.get(i);
			double shares[] = SHARES[i];
			if (!isComputed) {
				System.out.println("i=" + i + "(" + shares[0] + "," + shares[1] + "," + shares[2] + ")");
			}
			QueueScheduler.allocateResToQueue(q, shares);
		}
		isComputed = true;
	}

	public static void allox_heuristic(Resource resCapacity, List<JobQueue> runningQueues) {
		// sort queues based on beta
		int numberOfQueues = runningQueues.size();
		Collections.sort((List<JobQueue>) runningQueues, new BetaComparator());
		double[] betas = new double[runningQueues.size()];
		for (int i = 0; i < runningQueues.size(); i++) {
			betas[i] = runningQueues.get(i).getReportBeta();
		}

		// initialization
		double price[] = new double[2];
		JobQueue lastQueue = runningQueues.get(numberOfQueues - 1);
		price[0] = 1;
		price[1] = lastQueue.getReportBeta();
		Resource useralloc[] = userAlloc(betas, price);
		Resource currLoad = Resources.sum(useralloc);
		currLoad = Resources.divideVector(currLoad, resCapacity);

		int gpumin = numberOfQueues - 1;
		boolean flag = true;

		if (numberOfQueues == 0) {
			System.err.println("numberOfQueues is too small");
			return;
		} else if (numberOfQueues == 1) {
			flag = false;
		}

		while (flag) {
			if (currLoad.resources[0] <= currLoad.resource(1)) {
				double Y = (currLoad.resource(1) - currLoad.resource(0))
						/ (betas[gpumin] / resCapacity.resource(0) + 1.0 / resCapacity.resource(1));
				Y = Math.min(1 / betas[gpumin], Y);
				useralloc[gpumin].resources[1] = useralloc[gpumin].resources[1] - Y;
				useralloc[gpumin].resources[0] = betas[gpumin] * Y;
				currLoad = Resources.sum(useralloc);
				currLoad = Resources.divideVector(currLoad, resCapacity);
				break;
			}

			gpumin = gpumin - 1;
			if (gpumin < 0)
				System.err.println("gpumin is negative");

			price[0] = 1;
			price[1] = betas[gpumin];

			useralloc = userAlloc(betas, price);
			currLoad = Resources.sum(useralloc);
			currLoad = Resources.divideVector(currLoad, resCapacity);
		}

		Resource sumAlloc = Resources.sum(useralloc);
		// step 3: allocate the resources.
		for (int i = 0; i < numberOfQueues; i++) {
			JobQueue q = runningQueues.get(i);
			double shares[] = { 0, 0, 0 };
			shares[0] = Utils.roundBase(useralloc[i].resource(0) * resCapacity.resource(0) / sumAlloc.resource(0), 2);
			shares[1] = Utils.roundBase(useralloc[i].resource(1) * resCapacity.resource(1) / sumAlloc.resource(1), 2);
			// shares[0] = useralloc[i].resource(0) * resCapacity.resource(0) /
			// sumAlloc.resource(0);
			// shares[1] = useralloc[i].resource(1) * resCapacity.resource(1) /
			// sumAlloc.resource(1);
			shares[2] = shares[0] / q.getDemand().cpu * q.getDemand().mem
					+ shares[1] / q.getDemand().gpu * q.getDemand().gpuMem;
			if (!isComputed) {
				System.out.println("i=" + i + "(" + shares[0] + "," + shares[1] + "," + shares[2] + ")");
			}
			QueueScheduler.allocateResToQueue(q, shares);
		}
		isComputed = true;
	}

	private static Resource[] userAlloc(double[] betas, double currentPrices[]) {
		Resource userAlloc[] = Resources.NONEs(betas.length);

		for (int j = 0; j < betas.length; j++) {
			double beta = betas[j];
			if (beta < currentPrices[1]) {
				userAlloc[j].resources[0] = 1;
				userAlloc[j].resources[1] = 0;
			} else { // if beta = price, put it in GPU.
				userAlloc[j].resources[0] = 0;
				userAlloc[j].resources[1] = 1 / currentPrices[1];
			}
		}
		return userAlloc;
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
