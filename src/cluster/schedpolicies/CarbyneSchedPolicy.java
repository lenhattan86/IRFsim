package cluster.schedpolicies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cluster.carbyne.DagExecution;
import cluster.cluster.Cluster;
import cluster.datastructures.Resources;
import cluster.datastructures.StageDag;
import cluster.datastructures.Task;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.Interval;
import cluster.utils.Output;
import cluster.utils.Utils;

/**
 * Carbyne scheduling class
 * */
public class CarbyneSchedPolicy extends SchedPolicy {
	
	private static final boolean DEBUG = true;

  public CarbyneSchedPolicy(Cluster cluster) {
    super(cluster);
  }

  // look at the best execution timeline and pick the tasks
  // which need to be scheduled as of CURRENT_TIME
  @Override
  public void schedule(StageDag dag) {
    Set<Integer> tasksAsOfNowDag = Simulator.tasksToStartNow.get(dag.dagId);
    if (tasksAsOfNowDag == null || tasksAsOfNowDag.isEmpty()) {
      return;
    }
    for (int task : tasksAsOfNowDag) {
      boolean assigned = cluster.assignTask(dag.dagId, task,
          dag.duration(task), dag.rsrcDemands(task));
      if (assigned) {
        // Output.debugln(DEBUG,"assigned task: " + task + "; at time:"
        // + Simulator.CURRENT_TIME + " for dag:" + dag.dagId);

        // remove the task from the runnable and put it in running
        dag.runningTasks.add(task);
        dag.runnableTasks.remove((Integer) task);
      } else {
        Output.debugln(DEBUG,"Task:" + task + " from dag: " + dag.dagId
            + " cannot be assigned; it should!!!");
        break;
      }
    }
  }

  @Override
  public double planSchedule(StageDag dag, Resources leftOverResources) {
    if (dag.runnableTasks.isEmpty()) {
      return -1;
    }

    DagExecution dagExec = new DagExecution(dag, leftOverResources);
    double pessimism = Simulator.r.pickRandomDouble(0.0, 1.0);
    if (pessimism > Globals.LEVEL_OF_OPTIMISM) {
      Globals.NUM_OPT++;
      dagExec.schedule(false);
    }
    else {
      Globals.NUM_PES++;
      dagExec.schedule(true);
    }
    if (dagExec.complTime > 0) {

      Set<Integer> tasksToStartNow = dagExec.timelineExec
          .get(Simulator.CURRENT_TIME);
      // iff all dependencies for a task in tasksToStartNow are satisfied, then schedule
//// new code
      Set<Integer> tasksDepSatisfied = null;new HashSet<Integer>();
      for (int candTask : tasksToStartNow) {
        boolean candTaskReadyToSched = true;
        List<Interval> depCandTasks = dag.getParents(candTask);
        for (Interval ival : depCandTasks) {
          if (!dag.finishedTasks.containsAll(ival.toList())) {
            candTaskReadyToSched = false;
            break;
          }
        }
        if (candTaskReadyToSched) {
          if (tasksDepSatisfied == null) {
            tasksDepSatisfied = new HashSet<Integer>();
          }
          tasksDepSatisfied.add(candTask);
        }
      }
//// end new code

      if (tasksDepSatisfied != null) {
        Simulator.tasksToStartNow.put(dag.dagId, tasksDepSatisfied);

        // check to see if fungible resources can be decreased more
        if (Globals.ADJUST_FUNGIBLE) {
          adjustTasksFungibleRsrcs2(dag, dagExec.complTime, tasksDepSatisfied);
        }
      }
    }
    return dagExec.complTime;
  }

  void adjustTasksFungibleRsrcs2(StageDag dag, double expComplTime,
      Set<Integer> tasksToStartNow) {
    //Output.debugln(DEBUG,"start: adjustTasksFungibleRsrcs2 at time:"+Simulator.CURRENT_TIME +" expComplTime:"+expComplTime);

    StageDag adag = StageDag.clone(dag);
    for (int taskId : tasksToStartNow) {
      Resources taskRsrcDem = Resources.clone(dag.rsrcDemands(taskId));
      double taskDuration = dag.duration(taskId);
      //Output.debugln(DEBUG,"\ntaskId:"+taskId+" taskDuration:"+taskDuration+" taskRsrcDem:"+taskRsrcDem);
      for (int i = 2; i < Globals.NUM_DIMENSIONS; i++) {
        //Output.debugln(DEBUG,"\nResource current:"+i);
        int numTries = 0;
        double up = taskRsrcDem.resource(i);
        if (up == 0.0)
          continue;
        double bottom = 0.0;
        double bestResVal = Double.MAX_VALUE;
        double bestDurVal = Double.MIN_VALUE;

        // decrease every resource of this particular task
        double newTaskDuration = 0.0;
        double decr = 0.0;
        while (true) {
          //Output.debugln(DEBUG,"UP:"+up+" BOTTOM:"+bottom+" numTries:"+numTries);
          if ((up == bottom) || (numTries == 20) || (up == 0)) {
            //Output.debugln(DEBUG,"up and bottom are 0 || numTries == 5;");
            break;
          }

          decr = Utils.round(((up-bottom) / 2)+bottom, 2);
          if (decr == up) {
            //Output.debugln(DEBUG,"decr:"+decr+" up:"+up);
            break;
          }
          numTries++;
          //Output.debugln(DEBUG,"DECR:"+decr);
          //decr += 0.01;
          double newTaskResDemI = Utils.round(Math.max(decr, 0.001), 2);
          newTaskDuration = Utils.round(
              (taskRsrcDem.resource(i) * taskDuration) / newTaskResDemI , 2);
          if (newTaskDuration == 0) {
            break;
          }

          Resources adjustedTaskDemand = Resources.clone(taskRsrcDem);
          adjustedTaskDemand.resources[i] = newTaskResDemI;
          if (adag.adjustedTaskDemands == null) {
            adag.adjustedTaskDemands = new HashMap<Integer, Task>();
          }
          //Output.debugln(DEBUG,"added task adjusted; taskId:"+taskId+" taskDur:"+newTaskDuration+" taskDemands:"+adjustedTaskDemand);
          adag.adjustedTaskDemands.put(taskId, new Task(newTaskDuration,
              adjustedTaskDemand));

          // schedule the new dag
          DagExecution dagExec = new DagExecution(adag, null);
          dagExec.schedule(true);
          //Output.debugln(DEBUG,"dagExec.complTime:"+dagExec.complTime+" expComplTime:"+expComplTime);
          if (dagExec.complTime < 0 || dagExec.complTime > ( /*(1+0.15) * */expComplTime )) {
            // go up
            bottom = decr;
            //break;
          } else {
            up = decr;
            if (bestResVal > decr) {
              //Output.debugln(DEBUG,"-- dagExec.complTime:"+dagExec.complTime+" expComplTime:"+expComplTime);
              bestResVal = decr;
              bestDurVal = newTaskDuration;
              //Output.debugln(DEBUG,"-- dim:"+i+" bestResVal:"+bestResVal+" bestDurVal:"+bestDurVal);
            }
          }
        }
        //Output.debugln(DEBUG,"Dim:"+i+" best vals are; resBest:"+bestResVal+" newTaskDur:"+bestDurVal);
        if (bestResVal < Double.MAX_VALUE) {
          taskRsrcDem.resources[i] = bestResVal;
          taskDuration = bestDurVal;
        }
      }
      if (dag.rsrcDemands(taskId).distinctNew(taskRsrcDem)) {
        if (dag.adjustedTaskDemands == null)
          dag.adjustedTaskDemands = new HashMap<Integer, Task>();
        dag.adjustedTaskDemands.put(taskId, new Task(taskDuration,
            taskRsrcDem));
      }
    }
    //Output.debugln(DEBUG,"end: adjustTasksFungibleRsrcs2; took:"+(System.currentTimeMillis()-start)/1000);
  }

  void adjustTasksFungibleRsrcsNew(StageDag dag, double expComplTime,
      Set<Integer> tasksToStartNow) {
    Output.debugln(DEBUG,"== adjustTasksFungibleRsrcsNew ==");
    StageDag adag = StageDag.clone(dag);
    for (int taskId : tasksToStartNow) {
      Resources taskRsrcDem = dag.rsrcDemands(taskId);
      double taskDuration = dag.duration(taskId);
      Output.debugln(DEBUG,"Compute for task:"+taskId+" with taskRsrcDem:"+taskRsrcDem+" and dur:"+taskDuration);
      for (int i = 2; i < Globals.NUM_DIMENSIONS; i++) {
        Output.debugln(DEBUG,"For dimension:"+i);
        double smallestBadVal = Double.MIN_VALUE;
        double bestResVal = Double.MIN_VALUE;
        double bestDurVal = Double.MIN_VALUE;
        double decr = 0.0;
        double exp = 0;
        double currDecr = 0.0;
        double factor = 100;
        while (true) {
          Output.debugln(DEBUG,"smallestBadVal:"+smallestBadVal+" bestResVal:"+bestResVal+" bestDurVal:"+bestDurVal+" decr:"+decr+" exp:"+exp+" currDec:"+currDecr);
          currDecr = Math.pow(2, exp) / factor;
          decr += currDecr;
          double newTaskResDem = Utils.round(Math.max(taskRsrcDem.resource(i) - decr, .001), 2);
          if (newTaskResDem == 0) {
            Output.debugln(DEBUG,"newTaskResDem is 0");
            break;
          }
          double newTaskDuration = Utils.round((taskRsrcDem.resource(i) * taskDuration) / newTaskResDem, 2);
          Output.debugln(DEBUG,"new decr:"+currDecr+" decr:"+decr+" task new resDemand in dim."+i+" is:"+newTaskResDem+" taskDur old:"+taskDuration+" taskDur new:"+newTaskDuration);
          if (newTaskDuration == 0) {
            break;
          }
          Resources adjustedTaskDemand = Resources.clone(taskRsrcDem);
          adjustedTaskDemand.resources[i] = newTaskResDem;
          if (adag.adjustedTaskDemands == null) {
            adag.adjustedTaskDemands = new HashMap<Integer, Task>();
          }
          adag.adjustedTaskDemands.put(taskId, new Task(newTaskDuration,
              adjustedTaskDemand));
          // schedule the new dag
          DagExecution dagExec = new DagExecution(adag, null);
          dagExec.schedule(true);
          if (dagExec.complTime < 0 || dagExec.complTime > expComplTime) {
            Output.debugln(DEBUG,"A. dagExec.complTime:"+dagExec.complTime+" expComplTime:"+expComplTime);
            Output.debugln(DEBUG,"decr bef:"+decr+" currDecr:"+currDecr);
            decr -= currDecr;
            Output.debugln(DEBUG,"decr after:"+decr+" currDecr:"+currDecr+" exp:"+exp);
            exp = 0;
            Output.debugln(DEBUG,"smallestBadVal:"+smallestBadVal+" decr:"+decr);
            if (smallestBadVal == Double.MIN_VALUE) {
              smallestBadVal = decr; 
            }
            else {
              if (smallestBadVal == decr) {
                // went back to top-> nothing better found
                Output.debugln(DEBUG,"FOUND OPTIMAL -> nothing else to do");
                break;
              }
            }
            //factor *= 10;
            //break;
          }
          else {
            Output.debugln(DEBUG,"B. dagExec.complTime:"+dagExec.complTime+" expComplTime:"+expComplTime);
            Output.debugln(DEBUG,"bestResVal bef:"+bestResVal+" bestDurVal bef:"+bestDurVal);
            bestResVal = newTaskResDem;
            bestDurVal = newTaskDuration;
            Output.debugln(DEBUG,"bestResVal after:"+bestResVal+" bestDurVal after:"+bestDurVal);
            //taskRsrcDem.resources[i] = newTaskResDem;
            //taskDuration = newTaskDuration;
            exp++;
            Output.debugln(DEBUG,"exp val:"+exp);
          }
          
        }
        if (bestResVal != Double.MIN_VALUE) {
          taskRsrcDem.resources[i] = bestResVal;
          taskDuration = bestDurVal;
          Output.debugln(DEBUG,"Best val rsrc dem:"+bestResVal+" best val dur:"+taskDuration);
        }
      }
    }
  }

  // for the tasks which should start running as of now
  // try to decrease every fungible resource dimension
  // and see if the job completion time increases
  void adjustTasksFungibleRsrcs(StageDag dag, double expComplTime,
      Set<Integer> tasksToStartNow) {

    StageDag adag = StageDag.clone(dag);
    for (int taskId : tasksToStartNow) {
      Resources taskRsrcDem = dag.rsrcDemands(taskId);
      double taskDuration = dag.duration(taskId);

      for (int i = 2; i < Globals.NUM_DIMENSIONS; i++) {

        double decr = 0.0;
        // decrease every resource of this particular task
        while (true) {
          decr += 0.01;
          double newTaskResDemI = Utils.round(
              Math.max(taskRsrcDem.resource(i) - decr, 0.001), 2);
          double newTaskDuration = Utils.round(
              (taskRsrcDem.resource(i) * taskDuration) / newTaskResDemI, 2);
          if (newTaskDuration == 0) {
            break;
          }

          Resources adjustedTaskDemand = Resources.clone(taskRsrcDem);
          adjustedTaskDemand.resources[i] = newTaskResDemI;
          if (adag.adjustedTaskDemands == null) {
            adag.adjustedTaskDemands = new HashMap<Integer, Task>();
          }
          adag.adjustedTaskDemands.put(taskId, new Task(newTaskDuration,
              adjustedTaskDemand));

          // schedule the new dag
          DagExecution dagExec = new DagExecution(adag, null);
          dagExec.schedule(true);
          if (dagExec.complTime < 0 || dagExec.complTime > expComplTime) {
            break;
          } else {
            taskRsrcDem.resources[i] = newTaskResDemI;
            taskDuration = newTaskDuration;
            decr = 0.0;
          }
        }
      }
      if (dag.rsrcDemands(taskId).distinctNew(taskRsrcDem)) {
        if (dag.adjustedTaskDemands == null)
          dag.adjustedTaskDemands = new HashMap<Integer, Task>();
        dag.adjustedTaskDemands.put(taskId, new Task(taskDuration,
            taskRsrcDem));
      }
    }
  }
}
