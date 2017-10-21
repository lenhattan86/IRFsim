package cluster.datastructures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import cluster.data.JobData;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.Interval;
import cluster.utils.Output;

public class MLJob extends BaseJob implements Cloneable {

	private static final boolean DEBUG = false;
	public String dagName;

	public Map<String, String> nextHopOnCriticalPath;

	// keep track of ancestors and descendants of tasks per task
	public Map<Integer, Set<Integer>> ancestorsT = new HashMap<Integer, Set<Integer>>();
	public Map<Integer, Set<Integer>> descendantsT = new HashMap<Integer, Set<Integer>>();
	public Map<Integer, Set<Integer>> unorderedNeighborsT = new HashMap<Integer, Set<Integer>>();
	public Map<String, Set<String>> ancestorsS = new HashMap<String, Set<String>>();
	public Map<String, Set<String>> descendantsS = new HashMap<String, Set<String>>();
	public Map<String, Set<String>> unorderedNeighborsS = new HashMap<String, Set<String>>();

	public Set<String> chokePointsS;
	public Set<Integer> chokePointsT;

	// keep track of adjusted profiles for certain tasks;
	public Map<Integer, Task> adjustedTaskDemands = null;

	public MLJob(int id, int... arrival) {
		super(id, arrival);
		stages = new LinkedHashMap<String, SubGraph>();
		chokePointsS = new HashSet<String>();
		chokePointsT = null;
	}

	public static MLJob clone(MLJob dag) {
		MLJob clonedDag = new MLJob(dag.dagId);
		clonedDag.dagName = dag.dagName;
		clonedDag.numStages = dag.numStages;
		clonedDag.numEdgesBtwStages = dag.numEdgesBtwStages;

		clonedDag.rsrcQuota = Resources.clone(dag.rsrcQuota);
		clonedDag.jobExpDur = dag.jobExpDur;

		if (dag.adjustedTaskDemands != null)
			clonedDag.adjustedTaskDemands = new HashMap<Integer, Task>(dag.adjustedTaskDemands);

		clonedDag.runnableTasks = new LinkedHashSet<Integer>(dag.runnableTasks);
		clonedDag.runningTasks = new LinkedHashSet<Integer>(dag.runningTasks);
		clonedDag.finishedTasks = new LinkedHashSet<Integer>(dag.finishedTasks);
		clonedDag.chokePointsS.addAll(dag.chokePointsS);

		if (dag.CPlength != null) {
			clonedDag.CPlength = new HashMap<Integer, Double>(dag.CPlength);
			clonedDag.BFSOrder = new HashMap<Integer, Double>(dag.BFSOrder);
		}

		for (Map.Entry<String, SubGraph> entry : dag.stages.entrySet()) {
			String stageName = entry.getKey();
			SubGraph stage = entry.getValue();
			clonedDag.stages.put(stageName, SubGraph.clone(stage));
		}

		clonedDag.vertexToStage = new HashMap<Integer, String>(dag.vertexToStage);
		clonedDag.ancestorsS = new HashMap<String, Set<String>>(dag.ancestorsS);
		clonedDag.descendantsS = new HashMap<String, Set<String>>(dag.descendantsS);
		clonedDag.unorderedNeighborsS = new HashMap<String, Set<String>>(dag.unorderedNeighborsS);

		clonedDag.ancestorsT = new HashMap<Integer, Set<Integer>>(dag.ancestorsT);
		clonedDag.descendantsT = new HashMap<Integer, Set<Integer>>(dag.descendantsT);

		clonedDag.setQueueName(dag.getQueueName());

		return clonedDag;
	}

	// scale large DAGs to be handled by the simulator
	public void scaleDag() {

		int numTasksDag = allTasks().size();
		if (numTasksDag <= Globals.MAX_NUM_TASKS_DAG) {
			return;
		}

		double scaleFactor = 1.0;
		while (true) {
			scaleFactor *= 1.2;
			if ((int) Math.ceil((double) numTasksDag / scaleFactor) <= Globals.MAX_NUM_TASKS_DAG)
				break;
		}

		Map<Integer, String> vStartIdToStage = new TreeMap<Integer, String>();
		for (SubGraph stage : stages.values()) {
			vStartIdToStage.put(stage.vids.begin, stage.name);
		}

		Map<String, Integer> numTasksBefore = new HashMap<String, Integer>();
		int vertexIdxStart = 0, vertexIdxEnd = 0;
		for (int vIdStart : vStartIdToStage.keySet()) {
			String stageName = vStartIdToStage.get(vIdStart);

			int numVertices = stages.get(stageName).vids.length();
			numTasksBefore.put(stageName, numVertices);
			numVertices = (int) Math.max(Math.ceil((double) numVertices / scaleFactor), 1);
			vertexIdxEnd += numVertices;

			stages.get(stageName).vids.begin = vertexIdxStart;
			stages.get(stageName).vids.end = vertexIdxEnd - 1;

			vertexIdxStart = vertexIdxEnd;
		}

		// reinitialize the mapping from vertices to stages
		vertexToStage.clear();
		for (SubGraph stage : stages.values()) {
			for (int i = stage.vids.begin; i <= stage.vids.end; i++) {
				vertexToStage.put(i, stage.name);
			}
		}

		// update vids for dependencies between stages
		List<Dependency> edges = new ArrayList<Dependency>();
		for (String stageSrc : stages.keySet()) {
			for (String stageDst : stages.get(stageSrc).children.keySet()) {
				edges.add(new Dependency(stageSrc, stageDst, stages.get(stageSrc).children.get(stageDst).type));
			}
		}

		// update new edge structure
		for (String stage : stages.keySet()) {
			stages.get(stage).children.clear();
			stages.get(stage).parents.clear();
		}

		for (Dependency dependency : edges) {
			this.populateParentsAndChildrenStructure(dependency.parent, dependency.child, dependency.type);
		}
	}

	// only for tasks that are not running or finished
	public void reverseDag() {
		// remove tasks which are finished or running
		if (runningTasks != null) {
			for (int taskId : runningTasks) {
				this.vertexToStage.remove(taskId);
			}
		}
		if (finishedTasks != null) {
			for (int taskId : finishedTasks) {
				this.vertexToStage.remove(taskId);
			}
		}
		runningTasks.clear();
		runnableTasks.clear();

		// if any predecessor is still running -> can't consider it
		for (int taskId : this.vertexToStage.keySet()) {
			if (this.descendantsT.get(taskId)
					.isEmpty()/* && (ancestors.isEmpty()) */) {
				runnableTasks.add(taskId);
			}
		}
		finishedTasks.clear();
	}

	public Set<Integer> chokePointsT() {
		if (chokePointsT == null) {
			chokePointsT = new HashSet<Integer>();

			for (int task : this.vertexToStage.keySet()) {
				if (isChokePoint(task)) {
					chokePointsT.add(task);
				}
			}
		}
		return chokePointsT;
	}

	public boolean isChokePoint(int taskId) {
		String stageTaskId = this.vertexToStage.get(taskId);
		return (this.chokePointsS.contains(stageTaskId));
	}

	// view dag methods //
	public String viewDag() {
		String str = "";
		str += "\n == DAG: " + this.dagId + " == arrives at " + this.arrivalTime + " in Queue: " + this.queueName
				+ "\n";

		for (SubGraph stage : stages.values()) {
			str += "Stage: " + stage.id + " " + stage.name + " lasts " + stage.vDuration + " " + " [";
			str += stage.toString() + "]\n";

			str += " Task No: " + (stage.vids.end - stage.vids.begin + 1);
			// for (int i = stage.vids.begin; i <= stage.vids.end; i++)
			// str += i + " ";
			str += "\n";

			str += "  Parents: ";
			for (String parent : stage.parents.keySet())
				str += parent + ", ";
			str += "  Children: ";
			for (String child : stage.children.keySet())
				str += child + ", ";
			str += "\n";
		}

		// str += "== CP ==";
		// str += CPlength.toString();
		return str;
	}

	// end print dag //

	// read dags from file //
	public static Queue<BaseJob> readDags(String filePathString, boolean readNumIter, boolean readBeta, boolean genIteration) {
		Output.debugln(DEBUG, "[readDags] read " + filePathString);

		Queue<BaseJob> dags = new LinkedList<BaseJob>();
		File file = new File(filePathString);
		assert (file.exists() && !file.isDirectory());
		int jobNum = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			int dagsReadSoFar = 0;
			int vIdxStart, vIdxEnd;
			String dag_name = "";

			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#")) {
					dag_name = line.split("#")[1];
					dag_name = dag_name.trim();
					jobNum++;
					continue;
				}

				int numStages = 0, ddagId = -1, arrival = 0;
				int numOfIterations = 0;
				vIdxStart = 0;
				vIdxEnd = 0;

				String[] args = line.split(" ");
				assert (args.length <= 3) : "Incorrect node entry";

				dagsReadSoFar += 1;
				String queueName = "default";
				int sId = -1;
				if (args.length >= 2) {
					int j = 0;
					numStages = Integer.parseInt(args[j++]);
					ddagId = Integer.parseInt(args[j++]);
					if (args.length > j && readNumIter) {
						numOfIterations = Integer.parseInt(args[j++]);
					}
					if (args.length > j) {
						arrival = Integer.parseInt(args[j++]);
					}
					if (args.length > j) {
						queueName = args[j++].trim();
					}
					if (args.length > j) {
						sId = Integer.parseInt(args[j++].trim());
					}
					assert (numStages > 0);
					assert (ddagId >= 0);
					assert (numOfIterations > 0);
				} else if (args.length == 1) {
					numStages = Integer.parseInt(line);
					ddagId = dagsReadSoFar;
					assert (numStages > 0);
					assert (ddagId >= 0);
				}

				MLJob dag = new MLJob(ddagId, arrival);
				dag.numStages = numStages;
				if (readNumIter)
					dag.NUM_ITERATIONS = numOfIterations;
				dag.dagName = dag_name;
				dag.setQueueName(queueName);
				dag.sessionId = sId;
				if (Simulator.QUEUE_LIST == null)
					Simulator.QUEUE_LIST = new JobQueueList();

				Simulator.QUEUE_LIST.addJobQueue(queueName);
				int qIdx = Simulator.QUEUE_LIST.queueIdx(queueName);

				for (int i = 0; i < numStages; ++i) {
					String lline = br.readLine();
					args = lline.split(" ");
					assert (args.length == 7);

					int numVertices;
					String stageName;
					double durV;
					int j = 0;
					stageName = args[j++];
					assert (stageName.length() > 0);

					durV = Double.parseDouble(args[j++]);
					assert (durV >= 0);

					double gpuCpuDemand = Double.parseDouble(args[j++]);
					double memory= Double.parseDouble(args[j++]);
					double beta = 1.0;
					if (readBeta)
						beta = Double.parseDouble(args[j++]);
					InterchangableResourceDemand demand = new InterchangableResourceDemand(gpuCpuDemand, memory, beta);

					numVertices = Integer.parseInt(args[args.length - 1]);
					assert (numVertices >= 0);
					vIdxEnd += numVertices;
					SubGraph stage = new SubGraph(stageName, i, new Interval(vIdxStart, vIdxEnd - 1), durV, numVertices,
							demand);

					stage.reportDemands = new InterchangableResourceDemand(gpuCpuDemand + JobData.cheatedCpu[qIdx],
							memory + JobData.cheatedMemory[qIdx], beta + JobData.cheatedBeta[qIdx]);

					String arrivalStr = args[args.length - 2];
					if (arrivalStr.startsWith("@"))
						stage.arrivalTime = Integer.parseInt(arrivalStr.substring(1));

					dag.stages.put(stageName, stage);
					vIdxStart = vIdxEnd;
				}

				dag.vertexToStage = new HashMap<Integer, String>();
				for (SubGraph stage : dag.stages.values())
					for (int i = stage.vids.begin; i <= stage.vids.end; i++) {
						dag.vertexToStage.put(i, stage.name);
					}

				int numEdgesBtwStages;
				line = br.readLine();
				numEdgesBtwStages = Integer.parseInt(line);
				assert (numEdgesBtwStages >= 0);
				dag.numEdgesBtwStages = numEdgesBtwStages;

				for (int i = 0; i < numEdgesBtwStages; ++i) {
					args = br.readLine().split(" ");
					assert (args.length == 3) : "Incorrect entry for edge description; [stage_src stage_dst comm_type]";

					String stage_src = args[0], stage_dst = args[1], comm_pattern = args[2];
					assert (stage_src.length() > 0);
					assert (stage_dst.length() > 0);
					assert (comm_pattern.length() > 0);

					dag.populateParentsAndChildrenStructure(stage_src, stage_dst, comm_pattern);
				}

				// //TODO: read service curve
				// int numOfSlopes;
				// line = br.readLine();
				// numOfSlopes = Integer.parseInt(line.trim());
				// assert (numOfSlopes >= 0);
				// for (int i = 0; i < numOfSlopes; ++i) {
				// args = br.readLine().split(" ");
				// assert (args.length < 2) :
				// "Incorrect entry for service curves";
				//
				// double duration = Double.parseDouble(args[0]), slope =
				// Double.parseDouble(args[1]);
				// if(i==numOfSlopes-1) duration = Double.MAX_VALUE;
				// dag.serviceCurve.addSlope(slope, duration);
				// }

				// dag.scaleDag();
				// dag.setCriticalPaths();
				// dag.setBFSOrder();

				for (int taskId : dag.allTasks()) {
					if (dag.getParents(taskId).isEmpty()) {
						dag.runnableTasks.add(taskId);
					}
				}
				
				if(genIteration)
					dag.generateIterations(JobData.jobIterations[(jobNum-1)%JobData.jobIterations.length]);

				dags.add(dag);
				// Simulator.QUEUE_LIST.addRunnableJob2Queue(dag,
				// dag.getQueueName());
			}
			br.close();
		} catch (Exception e) {
			System.err.println("Catch exception: " + e);
			e.printStackTrace();
		}
		return dags;
	}

	public void generateIterations(int numIter) {
		if(numIter<=1) return;
		
		SubGraph originStage = stages.values().iterator().next();
//		originStage.name = Globals.strStage;
		String prevStage = originStage.name;
		for (int k = 1; k < numIter; k++) {
			// create new stage
			SubGraph newStage = SubGraph.clone(originStage, false);
			String newStageName = Globals.strStage + k;
			// change stage name
			newStage.name = newStageName;
			this.stages.put(newStageName, newStage);
			// add vertex to stage
			for (int i = newStage.vids.begin; i <= newStage.vids.end; i++) {
				this.vertexToStage.put(i, newStage.name);
			}
			
			this.numEdgesBtwStages++;
			this.populateParentsAndChildrenStructure(prevStage, newStageName, "ata");
			prevStage = newStageName;
		}
		this.numStages = numIter;
	}

	public void populateParentsAndChildrenStructure(String stage_src, String stage_dst, String comm_pattern) {

		if (!stages.containsKey(stage_src) || !stages.containsKey(stage_dst)) {
			Output.debugln(DEBUG,
					"A stage entry for " + stage_src + " or " + stage_dst + " should be already inserted !!!");
			return;
		}
		if (stages.get(stage_src).children.containsKey(stage_dst)) {
			Output.debugln(DEBUG, "An edge b/w " + stage_src + " and " + stage_dst + " is already present.");
			return;
		}
		Dependency d = new Dependency(stage_src, stage_dst, comm_pattern, stages.get(stage_src).vids,
				stages.get(stage_dst).vids);

		stages.get(stage_src).children.put(stage_dst, d);
		stages.get(stage_dst).parents.put(stage_src, d);
	}

	// end read dags from file //

	// DAG traversals //
	@Override
	public void setCriticalPaths() {
		if (CPlength == null) {
			CPlength = new HashMap<Integer, Double>();
		}
		for (int vertexId : vertexToStage.keySet()) {
			longestCriticalPath(vertexId);
		}
	}

	@Override
	public double getMaxCP() {
		return Collections.max(CPlength.values());
	}

	@Override
	public double longestCriticalPath(int taskId) {
		if (CPlength != null && CPlength.containsKey(taskId)) {
			return CPlength.get(taskId);
		}

		if (CPlength == null) {
			CPlength = new HashMap<Integer, Double>();
		}

		if (nextHopOnCriticalPath == null) {
			nextHopOnCriticalPath = new HashMap<String, String>();
		}

		double maxChildCP = Double.MIN_VALUE;
		String stageName = this.vertexToStage.get(taskId);

		List<Interval> children = this.getChildren(taskId);
		// Output.debugln(DEBUG,"Children: "+children);
		if (children.size() == 0) {
			maxChildCP = 0;
		} else {
			for (Interval i : children) {
				for (int child = i.begin; child <= i.end; child++) {
					double childCP = longestCriticalPath(child);
					if (maxChildCP < childCP) {
						maxChildCP = childCP;
						nextHopOnCriticalPath.put(stageName, this.vertexToStage.get(child));
					}
				}
			}
		}

		double cp = maxChildCP + stages.get(stageName).duration(taskId);
		if (!CPlength.containsKey(taskId)) {
			CPlength.put(taskId, cp);
		}

		return CPlength.get(taskId);
	}

	@Override
	public void setBFSOrder() {
		if (BFSOrder == null) {
			BFSOrder = new HashMap<Integer, Double>();
		}
		if (BFSOrder.size() == vertexToStage.size()) {
			return;
		}

		Set<String> visitedStages = new HashSet<String>();
		Map<String, Integer> numParents = new HashMap<String, Integer>();
		List<String> freeStages = new ArrayList<String>();

		for (SubGraph s : stages.values()) {
			if (s.parents.size() == 0) {
				freeStages.add(s.name);
			} else {
				numParents.put(s.name, s.parents.size());
			}
		}

		int currentLevel = 0;
		while (freeStages.size() > 0) {
			List<String> nextFreeStages = new ArrayList<String>();
			for (String s : freeStages) {
				assert (!visitedStages.contains(s));

				int sb = stages.get(s).vids.begin;
				int se = stages.get(s).vids.end;
				for (int i = sb; i <= se; i++) {
					BFSOrder.put(i, (double) currentLevel);
				}

				visitedStages.add(s);
				for (String c : stages.get(s).children.keySet()) {
					int updatedVal = numParents.get(c);
					updatedVal -= 1;
					assert (updatedVal >= 0);
					numParents.put(c, updatedVal);

					if (numParents.get(c) == 0) {
						nextFreeStages.add(c);
					}
				}
			}
			freeStages = nextFreeStages;
			currentLevel++;
		}

		for (int tId : vertexToStage.keySet()) {
			double updatedVal = BFSOrder.get(tId);
			BFSOrder.put(tId, currentLevel - updatedVal);
		}
	}

	// end DAG traversals //

	@Override
	public InterchangableResourceDemand rsrcDemands(int taskId) {
		if (adjustedTaskDemands != null && adjustedTaskDemands.get(taskId) != null) {
			// TODO: compute the real demands for this.
			return adjustedTaskDemands.get(taskId).demand;
		}
		return stages.get(vertexToStage.get(taskId)).rsrcDemands(taskId);
	}

	@Override
	public InterchangableResourceDemand reportDemands(int taskId) {
		return stages.get(vertexToStage.get(taskId)).reportDemands(taskId);
	}

	@Override
	public Resource rsrcUsage(int taskId) {
		if (adjustedTaskDemands != null && adjustedTaskDemands.get(taskId) != null) {
			// TODO: compute the real demands for this.
			return adjustedTaskDemands.get(taskId).usage;
		}

		if (this.isCPUUsages.get(taskId) != null)
			return this.stages.get(vertexToStage.get(taskId)).rsrcDemands(taskId).convertToCPUDemand();
		else
			return this.stages.get(vertexToStage.get(taskId)).rsrcDemands(taskId).convertToGPUDemand();
	}

	@Override
	public double duration(int taskId) {
		if (adjustedTaskDemands != null && adjustedTaskDemands.get(taskId) != null) {
			return adjustedTaskDemands.get(taskId).taskDuration;
		}
		return stages.get(vertexToStage.get(taskId)).duration(taskId);
	}

	@Override
	public List<Interval> getChildren(int taskId) {

		List<Interval> childrenTask = new ArrayList<Interval>();
		for (Dependency dep : stages.get(vertexToStage.get(taskId)).children.values()) {
			Interval i = dep.getChildren(taskId);
			childrenTask.add(i);
		}
		return childrenTask;
	}

	@Override
	public List<Interval> getParents(int taskId) {

		List<Interval> parentsTask = new ArrayList<Interval>();
		for (Dependency dep : stages.get(vertexToStage.get(taskId)).parents.values()) {
			Interval i = dep.getParents(taskId);
			parentsTask.add(i);
		}
		return parentsTask;
	}

	public Set<Integer> getParentsTasks(int taskId) {
		Set<Integer> allParentTasks = new HashSet<Integer>();

		for (Interval ival : getParents(taskId)) {
			for (int i = ival.begin; i <= ival.end; i++) {
				allParentTasks.add(i);
			}
		}
		return allParentTasks;
	}

	public Set<Integer> getChildrenTasks(int taskId) {
		Set<Integer> allChildrenTasks = new HashSet<Integer>();

		for (Interval ival : getChildren(taskId)) {
			for (int i = ival.begin; i <= ival.end; i++) {
				allChildrenTasks.add(i);
			}
		}
		return allChildrenTasks;
	}

	@Override
	public Set<Integer> allTasks() {
		return vertexToStage.keySet();
	}

	public Set<Integer> remTasksToSchedule() {
		Set<Integer> allTasks = new HashSet<Integer>(vertexToStage.keySet());
		Set<Integer> consideredTasks = new HashSet<Integer>(this.finishedTasks);
		consideredTasks.addAll(this.runningTasks);
		allTasks.removeAll(consideredTasks);
		return allTasks;
	}

	public Resource totalResourceDemand() {
		Resource totalResDemand = new Resource(0.0);
		for (SubGraph stage : stages.values()) {
			totalResDemand.addWith(stage.totalWork());
		}
		return totalResDemand;
	}

	public Resource totalWorkInclDur() {
		Resource totalResDemand = new Resource(0.0);
		for (SubGraph stage : stages.values()) {
			totalResDemand.addWith(stage.totalWork());
		}
		return totalResDemand;
	}

	@Override
	public Map area() {
		Map<Integer, Double> area_dims = new TreeMap<Integer, Double>();
		for (SubGraph stage : stages.values()) {
			// TODO: double check this.
			Resource res = stage.vDemands.convertToGPUDemand();
			for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
				double bef = area_dims.get(i) != null ? area_dims.get(i) : 0;
				bef += stage.vids.length() * stage.vDuration * res.resources[i];
				area_dims.put(i, bef);
			}
		}
		return area_dims;
	}

	// return true or false -> based on if this job has finished or not
	public boolean finishTasks_old(List<Integer> completedTasks, boolean reverse) {

		if (completedTasks.isEmpty())
			return false;

		// move finishedTasks from runningTasks into finishedTasks
		assert (runningTasks.containsAll(completedTasks));
		runningTasks.removeAll(completedTasks);
		finishedTasks.addAll(completedTasks);
		// for (int i = 0; i < completedTasks.size(); i++) {
		// Integer taskToRemove = completedTasks.get(i);
		// }
		/*
		 * for (int fTask : completedTasks) { assert
		 * (runningTasks.contains(fTask)); runningTasks.remove((Integer) fTask);
		 * finishedTasks.add(fTask); }
		 */
		// all tasks have finished -> job has completed
		if (finishedTasks.size() == allTasks().size()) {
			jobEndTime = Simulator.CURRENT_TIME;
			return true;
		}

		List<Integer> tasksRemToBeSched = new ArrayList<Integer>(allTasks());
		tasksRemToBeSched.removeAll(runnableTasks);
		tasksRemToBeSched.removeAll(runningTasks);
		tasksRemToBeSched.removeAll(finishedTasks);
		for (int candTask : tasksRemToBeSched) {
			boolean candTaskReadyToSched = true;
			List<Interval> depCandTasks = (!reverse) ? getParents(candTask) : getChildren(candTask);
			for (Interval ival : depCandTasks) {
				if (!finishedTasks.containsAll(ival.toList())) {
					candTaskReadyToSched = false;
					break;
				}
			}
			if (candTaskReadyToSched) {
				runnableTasks.add(candTask);
			}

		}

		return false;
	}

	public boolean finishTasks(List<Integer> completedTasks, boolean reverse) {

		if (completedTasks.isEmpty())
			return false;

		// move finishedTasks from runningTasks into finishedTasks
		assert (runningTasks.containsAll(completedTasks));
		runningTasks.removeAll(completedTasks);
		finishedTasks.addAll(completedTasks);

		if (finishedTasks.size() == allTasks().size()) {
			jobEndTime = Simulator.CURRENT_TIME;
			return true;
		}

		/*
		 * ArrayList<Integer> tasksToRun1 = new ArrayList<Integer>();
		 * List<Integer> tasksRemToBeSched = new ArrayList<Integer>(allTasks());
		 * tasksRemToBeSched.removeAll(runnableTasks);
		 * tasksRemToBeSched.removeAll(runningTasks);
		 * tasksRemToBeSched.removeAll(finishedTasks); for (int candTask :
		 * tasksRemToBeSched) { boolean candTaskReadyToSched = true;
		 * List<Interval> depCandTasks = (!reverse) ? getParents(candTask) :
		 * getChildren(candTask); for (Interval ival : depCandTasks) { if
		 * (!finishedTasks.containsAll(ival.toList())) { candTaskReadyToSched =
		 * false; break; } } if (candTaskReadyToSched) {
		 * tasksToRun1.add(candTask); } } Output.debugln(DEBUG,
		 * tasksToRun1.toString());
		 */

		ArrayList<Integer> tasksToRun = new ArrayList<Integer>();
		for (SubGraph stageToBeSched : stages.values()) {
			boolean stageReadyToSched = true;

			ArrayList<Integer> candTasks = stageToBeSched.getTasks();
			for (String depStageString : stageToBeSched.parents.keySet()) {
				if (!finishedTasks.containsAll(this.stages.get(depStageString).getTasks())) {
					stageReadyToSched = false;
					break;
				}
			}

			if (stageReadyToSched) {
				tasksToRun.addAll(candTasks);
			}
		}
		tasksToRun.removeAll(runnableTasks);
		tasksToRun.removeAll(runningTasks);
		tasksToRun.removeAll(finishedTasks);
		runnableTasks.addAll(tasksToRun);

		Output.debugln(DEBUG, tasksToRun.toString());

		return false;
	}

	public SubGraph[] allStages() {
		return (SubGraph[]) stages.values().toArray();
	}

	// should decrease only the resources allocated in the current time quanta
	/*
	 * public Resource currResShareAvailable() { Resource totalShareAllocated =
	 * Resources.clone(this.rsrcQuota);
	 * 
	 * for (int task : launchedTasksNow) { Resource rsrcDemandsTask =
	 * rsrcDemands(task); totalShareAllocated.subtract(rsrcDemandsTask); }
	 * totalShareAllocated.normalize(); return totalShareAllocated; }
	 */

	public void seedUnorderedNeighbors() {

		int numTasks = this.allTasks().size();

		if (this.unorderedNeighborsT == null) {
			this.unorderedNeighborsT = new HashMap<Integer, Set<Integer>>();
		}
		if (this.ancestorsT == null) {
			this.ancestorsT = new HashMap<Integer, Set<Integer>>();
		}
		if (this.descendantsT == null) {
			this.descendantsT = new HashMap<Integer, Set<Integer>>();
		}
		if (this.unorderedNeighborsT.size() == numTasks) {
			return;
		}

		for (int i = 0; i < numTasks; i++) {
			seedAncestors(i, this.ancestorsT);
		}
		for (int i = 0; i < numTasks; i++) {
			seedDescendants(i, this.descendantsT);
		}

		List<Integer> allTasks = new ArrayList<Integer>();
		for (int i = 0; i < numTasks; i++) {
			allTasks.add(i);
		}
		for (int i = 0; i < numTasks; i++) {

			Set<Integer> union_i = new HashSet<Integer>(ancestorsT.get(i));
			union_i.addAll(descendantsT.get(i));
			Interval i_stage_ival = this.stages.get(this.vertexToStage.get(i)).vids;
			for (int j = i_stage_ival.begin; j <= i_stage_ival.end; j++) {
				union_i.add(j);
			}
			Set<Integer> unorderedNeighborsT_i = new HashSet<Integer>(allTasks);

			unorderedNeighborsT_i.removeAll(union_i);
			unorderedNeighborsT.put(i, unorderedNeighborsT_i);
		}

		this.ancestorsS = new HashMap<String, Set<String>>();
		this.descendantsS = new HashMap<String, Set<String>>();
		this.unorderedNeighborsS = new HashMap<String, Set<String>>();

		for (SubGraph s : stages.values()) {
			int vid = s.vids.begin;

			Set<String> ancestorsStS = new HashSet<String>();
			for (int task : this.ancestorsT.get(vid)) {
				String ancestorSt = this.vertexToStage.get(task);
				ancestorsStS.add(ancestorSt);
			}
			ancestorsStS.remove(s);
			this.ancestorsS.put(s.name, ancestorsStS);

			Set<String> descendantsStS = new HashSet<String>();
			for (int task : this.descendantsT.get(vid)) {
				String descendantSt = this.vertexToStage.get(task);
				descendantsStS.add(descendantSt);
			}
			descendantsStS.remove(s);
			this.descendantsS.put(s.name, descendantsStS);

			Set<String> unorderedNeighborsStS = new HashSet<String>();
			for (int task : this.unorderedNeighborsT.get(vid)) {
				String unorderedNeighborSt = this.vertexToStage.get(task);
				unorderedNeighborsStS.add(unorderedNeighborSt);
			}
			unorderedNeighborsStS.remove(s);
			this.unorderedNeighborsS.put(s.name, unorderedNeighborsStS);
		}

		// particular case:
		if (checkTwoStageDag())
			return;

		// compute the tasks chokepoints
		for (String s : this.unorderedNeighborsS.keySet()) {
			if (this.unorderedNeighborsS.get(s).isEmpty()) {
				this.chokePointsS.add(s);
			}
		}
		return;
	}

	// if only two stages and have a parent / child relationship then
	// the stage with no descendants is a chokepoint as default
	public boolean checkTwoStageDag() {
		if (stages.size() == 2) {
			// this is very bad
			List<String> lstages = new ArrayList<String>();
			for (String stage : stages.keySet()) {
				lstages.add(stage);
			}
			if (ancestorsS.get(lstages.get(0)).contains(lstages.get(1))) {
				this.chokePointsS.add(lstages.get(0));
				return true;
			}
			if (ancestorsS.get(lstages.get(1)).contains(lstages.get(0))) {
				this.chokePointsS.add(lstages.get(1));
				return true;
			}
		}
		return false;
	}

	public void seedAncestors(int i, Map<Integer, Set<Integer>> ancestors) {
		if (!ancestors.containsKey(i)) {
			Set<Integer> a = new HashSet<Integer>();

			for (int x : this.getParentsTasks(i)) {
				if (!ancestors.containsKey(x))
					seedAncestors(x, ancestors);

				a.add(x);
				for (Integer y : ancestors.get(x))
					a.add(y);
			}
			ancestors.put(i, a);
		}
	}

	public MLJob convertFromDAGToMLJob() {
		MLJob job = clone(this);
		// convert the number of tasks at each stage
		for (SubGraph stage : job.stages.values()) {
			// int scale = (int)(stage.vDuration/this.NUM_ITERATIONS);
			// stage.vDuration = scale*Globals.TIME_UNIT;
			stage.taskNum = Globals.TASK_BROKEN_DOWN * stage.taskNum;
			double gc = stage.vDemands.getGpuCpu() / Globals.TASK_BROKEN_DOWN;
			double m = stage.vDemands.getMemory() / Globals.TASK_BROKEN_DOWN;
			double beta = stage.vDemands.getBeta();
			stage.vDemands = new InterchangableResourceDemand(gc, m, beta);
		}
		
		// update vids for dependencies between stages
		List<Dependency> edges = new ArrayList<Dependency>();
		for (String stageSrc : this.stages.keySet()) {
			for (String stageDst : stages.get(stageSrc).children.keySet()) {
				edges.add(new Dependency(stageSrc, stageDst, stages.get(stageSrc).children.get(stageDst).type));
			}
		}
		
		for (Dependency dependency : edges) {
			job.populateParentsAndChildrenStructure(dependency.parent, dependency.child, dependency.type);
		}
				
		return job;
	}

	public void seedDescendants(int i, Map<Integer, Set<Integer>> descendants) {
		if (!descendants.containsKey(i)) {
			Set<Integer> d = new HashSet<Integer>();

			for (int x : this.getChildrenTasks(i)) {
				if (!descendants.containsKey(x))
					seedDescendants(x, descendants);

				d.add(x);
				for (Integer y : descendants.get(x))
					d.add(y);
			}
			descendants.put(i, d);
		}
	}

	public void changeResDemand(double[][] errors) {

	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public Resource naiveRsrcDemands(int taskId) {
		Resource res = new Resource();
		InterchangableResourceDemand demand = rsrcDemands(taskId);
		res.resources[0] = demand.convertToCPU();
		res.resources[1] = demand.convertToGPU();
		res.resources[2] = demand.getMemory();
		return res;
	}
}
