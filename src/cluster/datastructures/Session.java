package cluster.datastructures;

public class Session {
  public static double MAX_SESSION_TIME = 10000.0; // in seconds. 
  private int numOfJobs = 0;
  private Resource alpha;
  private double alphaDuration = 0.0;
  private double startTime = 0.0;
  private double period = 0.0;
  
  private boolean isPeriodic = true;
  
  private boolean isReject = false;
  
  public Session (int numOfJobs, Resource alpha, double alphaDuration, double startTime, double period, boolean isPeriodic){
    this.numOfJobs = numOfJobs;
    this.alpha = alpha;
    this.startTime = startTime;
    this.period = period;
    this.alphaDuration = alphaDuration;
    this.isPeriodic = isPeriodic;
  }
  
  public int getNumOfJobs() {
    return numOfJobs;
  }

  public Resource getAlpha() {
    return alpha;
  }

  public double getStartTime() {
    return startTime;
  }

  public double getPeriod(int numAdmittedQueues) {
    if (isPeriodic)
      return period;
    else
      return alphaDuration*(numAdmittedQueues+1);
  }
  
  public double getPeriod(){
    return period;
  }
  
  public double getAlphaDuration(){
    return this.alphaDuration;
  }
  
  public boolean isPeriodic(){
    return this.isPeriodic;
  }
  
  public void reject(){
    this.isReject = true;
  }
  
  public boolean isReject(){
    return this.isReject;
  }
}
