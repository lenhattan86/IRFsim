package cluster.simulator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import cluster.cluster.Cluster;
import cluster.datastructures.BaseDag;
import cluster.datastructures.Resources;
import cluster.datastructures.Stage;
import cluster.datastructures.StageDag;
import cluster.resources.LeftOverResAllocator;
import cluster.schedulers.InterJobScheduler;
import cluster.schedulers.IntraJobScheduler;
import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.JobsArrivalPolicy;
import cluster.utils.Output;
import cluster.utils.Pair;
import cluster.utils.Randomness;
import cluster.utils.Triple;
import cluster.utils.Utils;

// implement the timeline server
public class Simulator {

	public final static boolean DEBUG = true;

	public static double CURRENT_TIME = 0;

	public static Queue<BaseDag> runnableJobs;
	public static Queue<BaseDag> runningJobs;
	public static Queue<BaseDag> completedJobs;

	public static Cluster cluster;

	public static Randomness r;
	double nextTimeToLaunchJob = 0;

	int totalReplayedJobs;
	int lastCompletedJobs;

	public static InterJobScheduler interJobSched;
	public static IntraJobScheduler intraJobSched;

	public static LeftOverResAllocator leftOverResAllocator;

	// dag_id -> list of tasks
	public static Map<Integer, Set<Integer>> tasksToStartNow;

	public Simulator() {
		runnableJobs = StageDag.readDags(Globals.PathToInputFile, Globals.DagIdStart,
				Globals.DagIdEnd - Globals.DagIdStart + 1);
		Output.debugln(DEBUG, "Print DAGs");
		for (BaseDag dag : runnableJobs) {
			((StageDag) dag).viewDag();
		}

		cluster = new Cluster(true, new Resources(Globals.MACHINE_MAX_RESOURCE));

		if (Globals.COMPUTE_STATISTICS) {

			double[] area_makespan = new double[Globals.NUM_DIMENSIONS];
			Output.debugln(DEBUG, "#dag_id maxCP area");
			double total_area = 0.0;
			for (BaseDag dag : runnableJobs) {
				StageDag ddag = (StageDag) dag;
				double[] bottlenecks = new double[Globals.NUM_DIMENSIONS];
				for (Stage stage : ddag.stages.values()) {
					bottlenecks[stage.vDemands.resBottleneck()] += 1;
				}
				Output.debugln(DEBUG, "dagName:" + ddag.dagName + " numOfStages:" + ddag.stages.values().size());
				for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
					Output.debug(DEBUG, " " + bottlenecks[i] / ddag.stages.values().size());
				}
				Output.debug(DEBUG, "\n");
			}
			// System.exit(-1); //TODO: why exit in the middle
			for (BaseDag dag : runnableJobs) {
				for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
					area_makespan[i] += dag.area().get(i);
				}
				double areaJob = (double) Collections.max(dag.area().values()) / Globals.MACHINE_MAX_RESOURCE;
				double maxCPJob = dag.getMaxCP();
				Output.debugln(DEBUG, dag.dagId + " " + maxCPJob + " " + areaJob);
				total_area += areaJob;
			}
			double max_area_makespan = Double.MIN_VALUE;
			for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
				max_area_makespan = Math.max(max_area_makespan, area_makespan[i] / Globals.MACHINE_MAX_RESOURCE);
			}
			Output.debugln(DEBUG, "makespan_lb: " + total_area + " " + max_area_makespan);
			// System.exit(-1); // TODO: why exit in the middle
		}

		totalReplayedJobs = runnableJobs.size();
		runningJobs = new LinkedList<BaseDag>();
		completedJobs = new LinkedList<BaseDag>();

		interJobSched = new InterJobScheduler();
		intraJobSched = new IntraJobScheduler();

		leftOverResAllocator = new LeftOverResAllocator();

		tasksToStartNow = new TreeMap<Integer, Set<Integer>>();

		r = new Randomness();
	}

	public void simulate() {

		for (Simulator.CURRENT_TIME = 0; Simulator.CURRENT_TIME < Globals.SIM_END_TIME; Simulator.CURRENT_TIME += Globals.STEP_TIME) {
			if (!Globals.DEBUG_ALL && !Globals.DEBUG_LOCAL)
				System.out.print(".");

			Output.debugln(DEBUG, "\n==== STEP_TIME:" + Simulator.CURRENT_TIME + " ====\n");

			Simulator.CURRENT_TIME = Utils.round(Simulator.CURRENT_TIME, 2);
			tasksToStartNow.clear();

			// terminate any task if it can finish and update cluster available
			// resources
			Map<Integer, List<Integer>> finishedTasks = cluster.finishTasks();

			// update jobs status with newly finished tasks
			boolean jobCompleted = updateJobsStatus(finishedTasks);

			// stop condition
			if (stop()) {
				Output.debugln(DEBUG, "==== Final Report: Completed Jobs ====");
				TreeMap<Integer, Double> results = new TreeMap<Integer, Double>();
				double makespan = Double.MIN_VALUE;
				double average = 0.0;
				for (BaseDag dag : completedJobs) {
					double jobDur = (dag.jobEndTime - dag.jobStartTime);
					// Output.debugln(DEBUG,"Dag:" + dag.dagId + " compl. time:"
					// + (dag.jobEndTime - dag.jobStartTime));
					double dagDuration = (dag.jobEndTime - dag.jobStartTime);
					makespan = Math.max(makespan, dagDuration);
					average += dagDuration;
					results.put(dag.dagId, (dag.jobEndTime - dag.jobStartTime));
				}
				average /= completedJobs.size();
				Output.debugln(DEBUG, "---------------------");
				Output.debugln(DEBUG, "Avg. job compl. time:" + average);
				Output.debugln(DEBUG, "Makespan:" + makespan);
				for (Integer dagId : results.keySet()) {
					Output.debugln(DEBUG, dagId + " " + results.get(dagId));
				}
				Output.debugln(DEBUG, "NUM_OPT:" + Globals.NUM_OPT + " NUM_PES:" + Globals.NUM_PES);
				break;
			}

			// handle jobs completion and arrivals
			boolean newJobArrivals = handleNewJobArrival();

			if (!jobCompleted && !newJobArrivals && finishedTasks.isEmpty()) {
				// if (finishedTasks.isEmpty()) {
				Output.debugln(DEBUG, "----- Do nothing ----");
			} else {
				if (Globals.TETRIS_UNIVERSAL) {
					interJobSched.resSharePolicy.packTasks();
				} else {
					Output.debugln(DEBUG, "[Simulator]: jobCompleted:" + jobCompleted + " newJobArrivals:" + newJobArrivals);
					if (jobCompleted || newJobArrivals) {
						interJobSched.schedule();
					}

					Output.debugln(DEBUG, "Running jobs size:" + runningJobs.size());

					// reallocate the share
					interJobSched.adjustShares();

					// do intra-job scheduling for every running job
					if (Globals.INTRA_JOB_POLICY == Globals.SchedulingPolicy.SpeedFair) {

						// if still available resources, go one job at a time and fill if
						// something. can be scheduled more
						Output.debugln(DEBUG,
								"[Simulator]: START work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());

						// while things can happen, give total resources to a job at a time,
						// the order is dictated by the inter job scheduler:
						// Shortest JobFirst - for SJF
						// RR - for Fair and DRF
						List<Integer> orderedJobs = interJobSched.orderedListOfJobsBasedOnPolicy();
						for (int jobId : orderedJobs) {
							for (BaseDag dag : runningJobs) {
								if (dag.dagId == jobId) {
									Resources totalResShare = Resources.clone(dag.rsrcQuota);
									// dag.rsrcQuota =
									// Resources.clone(cluster.getClusterResAvail());
									intraJobSched.schedule((StageDag) dag);
									dag.rsrcQuota = totalResShare;
									break;
								}
							}
						}

						Output.debugln(DEBUG,
								"[Simulator]: END work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());

					} else if (Globals.INTRA_JOB_POLICY != Globals.SchedulingPolicy.Carbyne) {

						// if still available resources, go one job at a time and fill if
						// something. can be scheduled more
						Output.debugln(DEBUG,
								"[Simulator]: START work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());

						// while things can happen, give total resources to a job at a time,
						// the order is dictated by the inter job scheduler:
						// Shortest JobFirst - for SJF
						// RR - for Fair and DRF
						List<Integer> orderedJobs = interJobSched.orderedListOfJobsBasedOnPolicy();
						for (int jobId : orderedJobs) {
							for (BaseDag dag : runningJobs) {
								if (dag.dagId == jobId) {
									Resources totalResShare = Resources.clone(dag.rsrcQuota);
									dag.rsrcQuota = Resources.clone(cluster.getClusterResAvail());
									intraJobSched.schedule((StageDag) dag);
									dag.rsrcQuota = totalResShare;
									break;
								}
							}
						}

						Output.debugln(DEBUG,
								"[Simulator]: END work conserving; clusterAvail:" + Simulator.cluster.getClusterResAvail());

					} else {
						// compute if any tasks should be scheduled based on reverse
						// schedule
						for (BaseDag dag : runningJobs) {
							dag.timeToComplete = intraJobSched.planSchedule((StageDag) dag, null);
						}
						// Output.debugln(DEBUG,"Tasks which should start as of now:"
						// + Simulator.tasksToStartNow);

						// schedule the DAGs -> looking at the list of tasksToStartNow
						for (BaseDag dag : runningJobs) {
							intraJobSched.schedule((StageDag) dag);
						}

						// Step2: redistribute the leftOverResources and ensuring is work
						// conserving
						leftOverResAllocator.allocLeftOverRsrcs();
					}
				}
			}

			for (BaseDag dag : Simulator.runningJobs) {
				dag.receivedService.addUsage(dag.rsrcInUse);
			}

			Simulator.printUsedResources();

			Output.debugln(DEBUG, "\n==== END STEP_TIME:" + Simulator.CURRENT_TIME + " ====\n");
		}
	}

	public static void printUsedResources() {
		for (BaseDag dag : runningJobs) {
			// Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- dag.rsrcInUse: " +
			// dag.rsrcInUse + " -- dag.currResDemand(): "
			// + dag.currResDemand());
			// Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- Resource Share: " +
			// dag.rsrcQuota);
			Resources minReq = dag.serviceCurve.getMinReqService(Simulator.CURRENT_TIME);
			boolean isSatisfied = dag.receivedService.greaterOrEqual(minReq);
			Output.debugln(DEBUG, "Dag Id " + dag.dagId + " " + (isSatisfied ? "is SATISFIED " : " is NOT satified")
					+ " -- Received Resource: " + dag.receivedService);
			Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- Received Resource: " + dag.receivedService);
			Output.debugln(DEBUG, "Dag Id " + dag.dagId + " -- Service Curve: " + minReq);
		}
	}

	boolean stop() {
		return (runnableJobs.isEmpty() && runningJobs.isEmpty() && (completedJobs.size() == totalReplayedJobs));
	}

	boolean updateJobsStatus(Map<Integer, List<Integer>> finishedTasks) {
		boolean someDagFinished = false;
		if (!finishedTasks.isEmpty()) {
			Iterator<BaseDag> iter = runningJobs.iterator();
			while (iter.hasNext()) {
				BaseDag crdag = iter.next();
				if (finishedTasks.get(crdag.dagId) == null) {
					continue;
				}

				Output.debugln(DEBUG, "DAG:" + crdag.dagId + ": " + finishedTasks.get(crdag.dagId).size()
						+ " tasks finished at time:" + Simulator.CURRENT_TIME);
				someDagFinished = ((StageDag) crdag).finishTasks(finishedTasks.get(crdag.dagId), false);

				if (someDagFinished) {
					Output.debugln(DEBUG, "DAG:" + crdag.dagId + " finished at time:" + Simulator.CURRENT_TIME);
					nextTimeToLaunchJob = Simulator.CURRENT_TIME;
					completedJobs.add(crdag);

					iter.remove();
				}
			}
		}
		return someDagFinished;
	}

	boolean handleNewJobArrival() {
		// flag which specifies if jobs have inter-arrival times or starts at t=0
		Output.debugln(DEBUG,
				"handleNewJobArrival; currentTime:" + Simulator.CURRENT_TIME + " nextTime:" + nextTimeToLaunchJob);
		if (runnableJobs.isEmpty() || ((nextTimeToLaunchJob != Simulator.CURRENT_TIME)
				&& (Globals.JOBS_ARRIVAL_POLICY != JobsArrivalPolicy.Trace))) {
			return false;
		}

		// start all jobs at time = 0
		if (Globals.JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.All) {
			while (!runnableJobs.isEmpty()) {
				BaseDag newJob = runnableJobs.poll();
				newJob.jobStartTime = Simulator.CURRENT_TIME;
				runningJobs.add(newJob);
				Output.debugln(DEBUG, "Started job:" + newJob.dagId + " at time:" + Simulator.CURRENT_TIME);
			}
		} else if (Globals.JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.One) {
			// if no job is running -> poll and add
			if (Simulator.runningJobs.isEmpty()) {
				BaseDag newJob = runnableJobs.poll();
				newJob.jobStartTime = Simulator.CURRENT_TIME;
				runningJobs.add(newJob);
				runnableJobs.remove(newJob);
				Output.debugln(DEBUG, "Started job:" + newJob.dagId + " at time:" + Simulator.CURRENT_TIME);
			}
		}
		// start one job at a time
		// compute the next time to launch a job using a distribution
		else if (Globals.JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.Distribution) {

			do {
				BaseDag newJob = runnableJobs.poll();
				assert newJob != null;
				newJob.jobStartTime = Simulator.CURRENT_TIME;

				runningJobs.add(newJob);
				nextTimeToLaunchJob = Utils.round(Math.max(Math.ceil(r.GetNormalSample(20, 5)), 2), 0);
				nextTimeToLaunchJob += Simulator.CURRENT_TIME;
				Output.debugln(DEBUG, "Started job:" + newJob.dagId + " at time:" + Simulator.CURRENT_TIME
						+ " next job arrives at time:" + nextTimeToLaunchJob);
			} while (!runnableJobs.isEmpty() && (nextTimeToLaunchJob == Simulator.CURRENT_TIME));
		} else if (Globals.JOBS_ARRIVAL_POLICY == JobsArrivalPolicy.Trace) {
			Set<BaseDag> newlyStartedJobs = new HashSet<BaseDag>();
			for (BaseDag dag : runnableJobs) {
				if (dag.timeArrival == Simulator.CURRENT_TIME) {
					dag.jobStartTime = Simulator.CURRENT_TIME;
					newlyStartedJobs.add(dag);
					Output.debugln(DEBUG, "Started job:" + dag.dagId + " at time:" + Simulator.CURRENT_TIME);
				}
			}
			// clear the datastructures
			runnableJobs.removeAll(newlyStartedJobs);
			runningJobs.addAll(newlyStartedJobs);
		}

		return true;
	}

	boolean handleNewJobCompleted() {
		int currCompletedJobs = completedJobs.size();
		if (lastCompletedJobs < currCompletedJobs) {
			lastCompletedJobs = currCompletedJobs;
			return true;
		}
		return false;
	}

	public static StageDag getDag(int dagId) {
		for (BaseDag dag : Simulator.runningJobs) {
			if (dag.dagId == dagId) {
				return (StageDag) dag;
			}
		}
		return null;
	}
}
