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

public class PricingScheduler implements Scheduler {
	private static boolean DEBUG = true;
	private String schedulePolicy;
	// Map<String, Resources> resDemandsQueues = null;

	static Resource clusterTotCapacity = null;

	// implementation idea:
	// 1. for every queue, compute it's total resource demand vector

	public PricingScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "PricingScheduler";
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
		pricing(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
	}

	public static void pricing(Resource resCapacity, List<JobQueue> runningQueues) {

		// sort queues based on beta
		int numberOfQueues = runningQueues.size();
		Collections.sort((List<JobQueue>) runningQueues, new BetaComparator());
		double[] betas = new double[runningQueues.size()];
		for (int i = 0; i < runningQueues.size(); i++) {
			betas[i] = runningQueues.get(i).getReportBeta();
		}

		// compute the ratios
		double ratios[] = new double[numberOfQueues];
		for (int i = 0; i < numberOfQueues; i++) {
			JobQueue q = runningQueues.get(i);
			ratios[i] = q.getDemand().getMemory() / q.getDemand().getGpuCpu();
		}

		// initialization
		double price[] = new double[3];
		JobQueue lastQueue = runningQueues.get(numberOfQueues - 1);
		price[0] = 1;
		price[1] = lastQueue.getReportBeta();
		price[2] = 1 + lastQueue.getReportBeta();
		Resource useralloc[] = userallocGPU(betas, ratios, price);

		Resource currLoad = Resources.sum(useralloc);

		int gpumin = numberOfQueues - 1;
		boolean flag = true;

		Resource finalAlloc[] = Resources.NONEs(numberOfQueues);
		if (currLoad.resources[1] > currLoad.resources[0]) {
			finalAlloc = useralloc;
			finalAlloc[numberOfQueues - 1].resources[0] = 0;
			finalAlloc[numberOfQueues - 1].resources[1] = 0;
			currLoad = Resources.sum(finalAlloc); // x+beta y = z/ri; cur_load(1)+x =
																						// cur_load(2)+ y;
			finalAlloc[numberOfQueues - 1].resources[1] = (currLoad.resource(0) - currLoad.resource(1)
					+ (finalAlloc[numberOfQueues - 1].resources[2] / ratios[numberOfQueues - 1])) / (1 + lastQueue.getBeta());
			finalAlloc[numberOfQueues - 1].resources[0] = (currLoad.resource(1) - currLoad.resource(2)
					+ finalAlloc[numberOfQueues - 1].resources[2] / ratios[numberOfQueues - 1]) / (1 + lastQueue.getBeta());
			currLoad = Resources.sum(finalAlloc);
		}

		double error = Math.pow(10, -4);

		if (numberOfQueues == 0) {
			System.err.println("numberOfQueues is too small");
			return;
		} else if (numberOfQueues == 1) {
			flag = false;
			finalAlloc[0] = new Resource(resCapacity);
		}

		while (Math.abs(currLoad.resources[0] - currLoad.resources[1]) > error && flag) {
			gpumin = gpumin - 1;
			if (gpumin < 0)
				System.err.println("gpumin is negative");

			price[0] = 1;
			price[1] = betas[gpumin];
			price[2] = 1 + betas[gpumin];
			useralloc = useralloc(betas, ratios, price);
			currLoad = Resources.sum(useralloc);
			if (currLoad.resources[0] > currLoad.resource(1)) {
				Resource userAllocG[] = userallocGPU(betas, ratios, price);
				Resource currLoadG = Resources.sum(userAllocG);
				if (currLoadG.resources[0] > currLoadG.resource(1)) {
					continue;
				} else {
					finalAlloc = useralloc;
					finalAlloc[gpumin].resources[0] = 0;
					finalAlloc[gpumin].resources[1] = 0;
					currLoadG = Resources.sum(finalAlloc);
					finalAlloc[gpumin].resources[1] = (currLoadG.resources[0] - currLoadG.resources[1]
							+ (finalAlloc[gpumin].resource(2) / ratios[gpumin])) / (1 + betas[gpumin]);
					finalAlloc[gpumin].resources[0] = currLoadG.resource(1) + finalAlloc[gpumin].resource(1)
							- currLoadG.resource(0);
					break;
				}
			} else {
				for (double k = betas[gpumin + 1]; k >= betas[gpumin]; k = k - 0.0001) {
					price[0] = 1;
					price[1] = k;
					price[2] = k + 1;
					useralloc = userallocGPU(betas, ratios, price);
					currLoad = Resources.sum(useralloc);
					if (Math.abs(currLoad.resource(0) - currLoad.resource(1)) < error) {
						finalAlloc = useralloc;
						flag = false;
						break;
					}
				}
			}

		}

		if (numberOfQueues > 1) {
			double budget = Resources.sum(finalAlloc).bottleneckRes();
			price = Utils.multifly(price, budget);
			for (int j = 0; j < numberOfQueues; j++) {
				finalAlloc[j] = Resources.divideNoRound(finalAlloc[j], budget);
			}
		}

		// step 3: allocate the resources.
		for (int i = 0; i < numberOfQueues; i++) {
			JobQueue q = runningQueues.get(i);
			double shares[] = { finalAlloc[i].resource(0), finalAlloc[i].resource(1), finalAlloc[i].resource(2) };
			shares = Utils.multifly(shares, Globals.MACHINE_MAX_RESOURCE);
			QueueScheduler.allocateResToQueue(q, shares);
		}
	}

	private static Resource[] userallocGPU(double[] betas, double ratio[], double currentPrices[]) {
		Resource userAlloc[] = Resources.NONEs(betas.length);

		for (int j = 0; j < betas.length; j++) {
			double beta = betas[j];
			userAlloc[j].resources[2] = Math.min(1 / currentPrices[2],
					Math.max(ratio[j], beta * ratio[j] / currentPrices[1]));
			if (beta < currentPrices[1]) {
				userAlloc[j].resources[0] = userAlloc[j].resource(2) / ratio[j];
				userAlloc[j].resources[1] = 0;
			} else { // if beta = price, put it in GPU.
				userAlloc[j].resources[0] = 0;
				userAlloc[j].resources[1] = userAlloc[j].resource(2) / (ratio[j] * beta);
			}
		}

		return userAlloc;
	}

	private static Resource[] useralloc(double betas[], double ratios[], double currentPrices[]) {

		Resource userAlloc[] = Resources.NONEs(betas.length);
		for (int j = 0; j < betas.length; j++) {
			userAlloc[j].resources[2] = Math.min(1 / currentPrices[2],
					Math.max(ratios[j], betas[j] * ratios[j] / currentPrices[1]));
			if (betas[j] <= currentPrices[1]) {
				userAlloc[j].resources[0] = userAlloc[j].resource(2) / ratios[j];
				userAlloc[j].resources[1] = 0;
			} else {
				userAlloc[j].resources[0] = 0;
				userAlloc[j].resources[1] = userAlloc[j].resources[2] / (ratios[j] * betas[j]);
			}
		}
		return userAlloc;
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
