package cluster.datastructures;

public class Task implements Comparable<Task> {

	private static final boolean DEBUG = true;
  public int taskId;
  public int dagId;
  public double taskDuration;
  public InterchangableResourceDemand demand;
  public Resource usage;
  
  private int numSubtask = 0;

  public Task(int dagId, int taskId) {
    this.dagId = dagId;
    this.taskId = taskId;
  }

  public Task(int dagId, int taskId, double taskDuration, InterchangableResourceDemand demand) {
    this.dagId = dagId;
    this.taskId = taskId;
    this.taskDuration = taskDuration;
    this.demand = demand;
  }

  public Task(double taskDuration, InterchangableResourceDemand demand) {
    this.taskDuration = taskDuration;
    this.demand = demand;
  }

  @Override
  public String toString() {
    String output = "<" + this.dagId + " " + " " + this.taskId + ">";
    return output;
  }

  @Override
  public int compareTo(Task arg0) {
    return 0;
  }
  
  public void increaseSubtask(){
    this.numSubtask++;
  }
  
  public int getNumSubtask(){
    return this.numSubtask;
  }
  
}