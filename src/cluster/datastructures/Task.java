package cluster.datastructures;

public class Task implements Comparable<Task> {

	private static final boolean DEBUG = true;
  public int taskId;
  public int dagId;
  public double taskDuration;
  public Resource resDemands;

  public Task(int dagId, int taskId) {
    this.dagId = dagId;
    this.taskId = taskId;
  }

  public Task(int dagId, int taskId, double taskDuration, Resource resDemands) {
    this.dagId = dagId;
    this.taskId = taskId;
    this.taskDuration = taskDuration;
    this.resDemands = resDemands;
  }

  public Task(double taskDuration, Resource resDemands) {
    this.taskDuration = taskDuration;
    this.resDemands = resDemands;
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
}
