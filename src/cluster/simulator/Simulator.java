package cluster.simulator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import cluster.cluster.Cluster;
import cluster.datastructures.BaseJob;
import cluster.datastructures.JobQueue;
import cluster.datastructures.JobQueueList;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.datastructures.MLJob;
import cluster.schedulers.QueueScheduler;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.JobsArrivalPolicy;
import cluster.utils.Output;
import cluster.utils.Randomness;
import cluster.utils.Utils;

// implement the timeline server
public class Simulator {

	public static boolean DEBUG = false;

	public static double CURRENT_TIME = 0;
	
	public static Resource Capacity = null;

	public static Queue<BaseJob> runnableJobs = null;
	public static Queue<BaseJob> runningJobs = null;
	public static Queue<BaseJob> runningBatchJobs = null;
	public static Queue<BaseJob> completedJobs = null;

	public static boolean STOP_CMD = false;

	public static JobQueueList QUEUE_LIST = null;
	public static Cluster cluster = null;

	public static Randomness r;
	private double nextTimeToLaunchJob = 0;

	int totalReplayedJobs = 0;
	int lastCompletedJobs = 0;

	public static QueueScheduler queueSched = null;

	// dag_id -> list of tasks
	public static Map<Integer, Set<Integer>> tasksToStartNow = null;

	public static boolean ONLINE = true;

	public static Queue<BaseJob> user2RunableJobs = null;
	public static Queue<BaseJob> user1RunableJobs = null;

	private boolean IS_STOP = false;

	public static int completedJobCnt = 0;

	public static void duplicateJob(Queue<BaseJob> dags, int dagId) {
		MLJob dag = null;
		for (BaseJob d : dags) {
			if (d.dagId == dagId) {
				dag = (MLJob) d;
				break;
			}
		}

		if (dag == null)
			return;

		int newDagId = dags.size();
		try {
			MLJob newDag = (MLJob) dag.clone();
			newDag.dagId = newDagId;
			newDag.dagName = "" + newDagId;
			dags.add(newDag);
			// Simulator.QUEUE_LIST.addRunnableJob2Queue(newDag,
			// newDag.getQueueName());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	public Simulator() {
		CURRENT_TIME = 0;
		STOP_CMD = false;
		
		

		QUEUE_LIST = new JobQueueList();

		QUEUE_LIST.readQueue(Globals.PathToQueueInputFile);

		QUEUE_LIST.sortJobQueues();

		QUEUE_LIST.printQueueInfo();

		System.out.println("start reading Dags from " + Globals.PathToInputFile);

		runnableJobs = MLJob.readDags(Globals.PathToInputFile);

		System.out.println("done reading Dags from " + Globals.PathToInputFile);

		runningBatchJobs = new LinkedList<BaseJob>();
		double capacity[] = { Globals.MACHINE_MAX_CPU, Globals.MACHINE_MAX_GPU, Globals.MACHINE_MAX_MEM };
		cluster = new Cluster(true, new Resource(capacity));
		Capacity = cluster.getClusterMaxResAlloc(); 

		/*
		 * if (Globals.COMPUTE_STATISTICS) { double[] area_makespan = new
		 * double[Globals.NUM_DIMENSIONS]; Output.debugln(DEBUG,
		 * "#dag_id maxCP area"); double total_area = 0.0; for (BaseJob dag :
		 * runnableJobs) { MLJob ddag = (MLJob) dag; double[] bottlenecks = new
		 * double[Globals.NUM_DIMENSIONS]; for (SubGraph stage :
		 * ddag.stages.values()) { bottlenecks[stage.vDemands.resBottleneck()] += 1;
		 * } Output.debugln(DEBUG, "dagName:" + ddag.dagName + " numOfStages:" +
		 * ddag.stages.values().size()); for (int i = 0; i < Globals.NUM_DIMENSIONS;
		 * i++) { Output.debug(DEBUG, " " + bottlenecks[i] /
		 * ddag.stages.values().size()); } Output.debug(DEBUG, "\n"); } //
		 * System.exit(-1); //TODO: why exit in the middle for (BaseJob dag :
		 * runnableJobs) { for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
		 * area_makespan[i] += dag.area().get(i); } double areaJob = (double)
		 * Collections.max(dag.area().values()) / Globals.MACHINE_MAX_RESOURCE;
		 * double maxCPJob = dag.getMaxCP(); Output.debugln(DEBUG, dag.dagId + " " +
		 * maxCPJob + " " + areaJob); total_area += areaJob; } double
		 * max_area_makespan = Double.MIN_VALUE; for (int i = 0; i <
		 * Globals.NUM_DIMENSIONS; i++) { max_area_makespan =
		 * Math.max(max_area_makespan, area_makespan[i] /
		 * Globals.MACHINE_MAX_RESOURCE); } Output.debugln(DEBUG, "makespan_lb: " +
		 * total_area + " " + max_area_makespan); // System.exit(-1); // TODO: why
		 * exit in the middle }
		 */

		totalReplayedJobs = runnableJobs.size();
		runningJobs = new LinkedList<BaseJob>();
		completedJobs = new LinkedList<BaseJob>();

		queueSched = new QueueScheduler();

		tasksToStartNow = new TreeMap<Integer, Set<Integer>>();

		r = new Randomness();

		// Initialize output & log files
		Output.write("", false, Globals.PathToResourceLog);
	}

	public void simulateMultiQueues() {
		Simulator.CURRENT_TIME = 0;
		while (true) {
			double progress = Simulator.CURRENT_TIME / Globals.SIM_END_TIME * 100;
			if (progress % 10 == 0)
				System.out.println("progress: " + progress + "%");

			// for (Simulator.CURRENT_TIME = 0; Simulator.CURRENT_TIME <=
			// Globals.SIM_END_TIME; Simulator.CURRENT_TIME += Globals.STEP_TIME) {

			Output.debugln(DEBUG, "\n==== STEP_TIME:" + Utils.roundDefault(Simulator.CURRENT_TIME) + " ====\n");

			Simulator.CURRENT_TIME = Utils.roundDefault(Simulator.CURRENT_TIME);
			tasksToStartNow.clear();

			// terminate any task if it can finish and update cluster available
			// resources; converting waiting tasks to runnable tasks
			Map<Integer, List<Integer>> finishedTasks = cluster.finishTasks();

			// update jobs status with newly finished tasks
			boolean jobCompleted = updateJobsStatus(finishedTasks);

			// handle jobs completion and arrivals
			boolean newJobArrivals = handleNewJobArrival4MultiQueues();
			// STOP condition
			if (stop()) {
				printReport();
				writeReport();
				break;
			}

			QUEUE_LIST.updateRunningQueues();

			// if (!jobCompleted && !newJobArrivals && finishedTasks.isEmpty())
			if (false)
				Output.debugln(DEBUG, "----- Do nothing ----");
			else {
				Output.debugln(DEBUG,
						"[Simulator]: START work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());
				queueSched.schedule();
				Output.debugln(DEBUG,
						"[Simulator]: END work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());
			}

			for (BaseJob dag : Simulator.runningJobs) {
				dag.receivedService.addUsage(dag.getRsrcInUse());
				Resource usage = new Resource(dag.getRsrcInUse());
				dag.usedReses.add(usage);
				// System.out.println(usage);
			}

			Simulator.printUsedResources();
			Simulator.writeResourceUsage();
			Simulator.CURRENT_TIME += Globals.STEP_TIME;
		}
		System.out.println("\n==== END STEP_TIME:" + Simulator.CURRENT_TIME + " ====\n");
	}

	private boolean isStop() {
		if (Simulator.CURRENT_TIME >= Globals.SIM_END_TIME)
			return true;
		return IS_STOP;
	}

	private void printReport() {
		System.out.println("\n==== Final Report: Completed Jobs ====");
		TreeMap<Integer, Double> results = new TreeMap<Integer, Double>();
		double makespan = Double.MIN_VALUE;
		double average = 0.0;
		ArrayList<Double> avgCompletionTimePerQueue = new ArrayList<Double>();
		for (BaseJob dag : completedJobs) {
			makespan = Math.max(makespan, dag.jobEndTime);
			average = average + dag.getCompletionTime();
		}
		average /= completedJobs.size();
		System.out.println("---------------------");
		System.out.println("Avg. job compl. time:" + average);
		System.out.println("Jobs completed: " + completedJobs.size());
		System.out.println("Makespan:" + makespan);
		
		for (JobQueue queue : QUEUE_LIST.getJobQueues()) {
			double avg = 0.0;
			int count = 0;
			// Collections.sort((List<BaseJob>) queue.completedJobs, new
			// JobIdComparator());
			for (BaseJob dag : queue.completedJobs) {
				avg += dag.getCompletionTime();
				count++;
			}
			avg = avg / count;
			System.out.println(queue.getQueueName() + " Jobs completed: " + queue.completedJobs.size() + " avg. cmplt of "
					+ count + " jobs : " + avg);
		}
		System.out.println("---------------------");
		System.out.println("NUM_OPT:" + Globals.NUM_OPT + " NUM_PES:" + Globals.NUM_PES);
	}

	private void writeReport() {
		Output.writeln("JobId, startTime, endTime, duration, queueName", false);
		System.out.println("===== Final Report: Completed Jobs =====");
		TreeMap<Integer, Double> results = new TreeMap<Integer, Double>();
		double makespan = Double.MIN_VALUE;
		for (BaseJob dag : completedJobs) {
			double dagDuration = dag.getCompletionTime();
			makespan = Math.max(makespan, dagDuration);
			results.put(dag.dagId, dagDuration);
			Output.writeln(dag.dagId + "," + dag.jobStartTime + "," + dag.jobEndTime + "," + dagDuration + ","
					+ dag.getQueueName());
		}
	}

	private void writeResUsage() {
		System.out.println("==== Resource Usage in details ====");
		System.out.print("jobs = ");
		for (BaseJob dag : completedJobs) {
			System.out.print(dag.dagId + ",");
		}
		System.out.println("");

		System.out.print("normalizeRes = {");
		for (BaseJob dag : completedJobs) {
			Resource maxRes = new Resource();
			for (Resource res : dag.usedReses)
				maxRes = Resources.piecewiseMax(maxRes, res);
			System.out.print("" + maxRes.toStringList() + ",");
		}
		System.out.println("};");

		System.out.print("durs = {");
		for (BaseJob dag : completedJobs) {
			double dagDuration = (dag.jobEndTime - dag.jobStartRunningTime);
			System.out.print(dagDuration + ",");
		}
		System.out.println("};");

	}

	public static void printUsedResources() {
		for (BaseJob dag : runningJobs) {
			Output.debugln(DEBUG,
					"Dag Id " + dag.dagId + " in " + dag.getQueueName() + " -- dag.rsrcInUse: " + dag.getRsrcInUse());
			// Output.writeln(dag.dagId + ", " + dag.rsrcInUse, true,
			// Globals.PathToResourceLog);
			// Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- Resource Share: " +
			// dag.rsrcQuota);
			// Resources minReq =
			// dag.serviceCurve.getMinReqService(Simulator.CURRENT_TIME -
			// dag.jobStartTime);
			// boolean isSatisfied = dag.serviceCurve.isSatisfied(dag.receivedService,
			// Simulator.CURRENT_TIME);
			// if (!isSatisfied)
			// Output.debugln(DEBUG,
			// "Dag Id " + dag.dagId + " " + " is NOT satified" + " -- Received : " +
			// dag.receivedService);
			// Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- Received: " +
			// dag.receivedService + " -- S.Curve: " + minReq);
		}
	}

	public static void writeResourceUsage() {
		for (JobQueue q : QUEUE_LIST.getJobQueues()) {
			String toWrite = q.getResourceUsageStr() + "," + q.L;
			Output.writeln(toWrite, true, Globals.PathToResourceLog);
			Output.debugln(DEBUG, q.getResourceUsageStr());
		}
	}

	boolean stop() {
		if (Simulator.CURRENT_TIME >= Globals.SIM_END_TIME) {
			System.out.println("Simulator finished as the time is up.");
			return true;
		}

		if ((Globals.DEBUG_ALL || Globals.DEBUG_LOCAL) && Simulator.CURRENT_TIME >= Globals.SIM_END_TIME)
			return true;

		// if (Globals.numQueues != 0) return runningBatchJobs.isEmpty();

		return STOP_CMD;
	}

	boolean updateJobsStatus(Map<Integer, List<Integer>> finishedTasks) {
		boolean someDagFinished = false;
		if (!finishedTasks.isEmpty()) {
			Iterator<BaseJob> iter = runningJobs.iterator();
			while (iter.hasNext()) {
				BaseJob crdag = iter.next();
				if (finishedTasks.get(crdag.dagId) == null) {
					continue;
				}

				Output.debugln(DEBUG, "DAG:" + crdag.dagId + ": " + finishedTasks.get(crdag.dagId).size()
						+ " tasks finished at time:" + Simulator.CURRENT_TIME);
				boolean thisDagFinished = ((MLJob) crdag).finishTasks(finishedTasks.get(crdag.dagId), false);

				if (thisDagFinished) {
					Output.debugln(DEBUG, "DAG:" + crdag.dagId + " finished at time:" + Simulator.CURRENT_TIME);
					runningBatchJobs.remove(crdag);
					completedJobs.add(crdag);
					QUEUE_LIST.addCompletionJob2Queue(crdag, crdag.getQueueName());
					crdag.onFinish();
					iter.remove();
					someDagFinished = true;
				}
			}
		}
		return someDagFinished; // return true if one of the running jobs are
														// finished.
	}

	boolean handleNewJobArrival4MultiQueues() {
		// flag which specifies if jobs have inter-arrival times or starts at t=0
		Output.debugln(DEBUG,
				"handleNewJobArrival; currentTime:" + Simulator.CURRENT_TIME + " nextTime:" + nextTimeToLaunchJob);

		boolean existNewJob = false;

		// for batch jobs.
		// start all batch jobs at time = 0
		if (Globals.JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.All) {
			Set<BaseJob> newlyStartedJobs = new HashSet<BaseJob>();
			for (BaseJob newJob : runnableJobs) {
				if (!newJob.getQueue().isLQ) {
					newlyStartedJobs.add(newJob);
					Simulator.QUEUE_LIST.addRunningJob2Queue(newJob, newJob.getQueueName());
					newJob.jobStartTime = Simulator.CURRENT_TIME;
					/*
					 * Output.debugln(DEBUG, "Started job:" + newJob.dagId + " at time:" +
					 * Simulator.CURRENT_TIME);
					 */
					existNewJob = true;
				}
			}
			runnableJobs.removeAll(newlyStartedJobs);
			runningJobs.addAll(newlyStartedJobs);
			runningBatchJobs.addAll(newlyStartedJobs);
		} else if (Globals.JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.Trace) {
			Set<BaseJob> newlyStartedJobs = new HashSet<BaseJob>();
			for (BaseJob dag : runnableJobs) {
				if (!dag.getQueue().isLQ) {
					if (dag.arrivalTime == Simulator.CURRENT_TIME) {
						dag.jobStartTime = Simulator.CURRENT_TIME;
						// dag.jobStartRunningTime = dag.jobStartTime;
						newlyStartedJobs.add(dag);
						Simulator.QUEUE_LIST.addRunningJob2Queue(dag, dag.getQueueName());
						Output.debugln(DEBUG, "Started job:" + dag.dagId + " at time:" + Simulator.CURRENT_TIME);
						existNewJob = true;
					}
				}
			}
			// clear the datastructures
			runnableJobs.removeAll(newlyStartedJobs);
			runningJobs.addAll(newlyStartedJobs);
			runningBatchJobs.addAll(newlyStartedJobs);
		} else {
			System.err.println("JOBS_ARRIVAL_POLICY: " + Globals.JOBS_ARRIVAL_POLICY);
		}

		return existNewJob;
	}

	public static MLJob getDag(int dagId) {
		for (BaseJob dag : Simulator.runningJobs) {
			if (dag.dagId == dagId) {
				return (MLJob) dag;
			}
		}
		System.err.println("\nThere is no job " + dagId);
		return null;
	}

	public static void writeCompltJobs(String csvfile, ArrayList<String> data) {
		System.out.println(csvfile);
		FileWriter writer;
		try {
			writer = new FileWriter(csvfile);
			String collect = data.stream().collect(Collectors.joining("\n"));
			writer.write(collect);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void updateQueueWeight() {

	}

}