package cluster.datastructures;

import cluster.simulator.Simulator;

public class Session {
  public static double MAX_SESSION_TIME = 10000.0; // in seconds. 
  private int numOfJobs = 0;
  private Resource[] alphas;
  private double[] alphaDurations = null;
  private double startTime = 0;
  private double[] periods = null;
  
  private boolean isReject = false;
  
  public Session (int numOfJobs, Resource[] alphas, double[] alphaDurations, double startTime, double[] periods){
    this.numOfJobs = numOfJobs;
    this.alphas = new Resource[numOfJobs];
    this.periods = new double[numOfJobs]; 
    this.alphaDurations =new double[numOfJobs]; 
    
    for (int i=0; i<numOfJobs; i++){
      int idx = i % alphaDurations.length;
      this.alphas[i] = new Resource(alphas[idx]);
      this.periods[i] = periods[idx];
      this.alphaDurations[i] = alphaDurations[idx];
    }
    this.startTime = startTime;
  }
  
  public Session (int numOfJobs, double[] alphaVals, double[] alphaDurations, double startTime, double[] periods){
    this.numOfJobs = numOfJobs;
    this.alphas = new Resource[numOfJobs];
    this.periods = new double[numOfJobs]; 
    this.alphaDurations =new double[numOfJobs]; 
    for (int i=0; i<numOfJobs; i++){
      int idx = i % alphaVals.length;
      this.alphas[i] = new Resource(alphaVals[idx]);
      this.periods[i] = periods[idx];
      this.alphaDurations[i] = alphaDurations[idx];
    }
    this.startTime = startTime;
  }
  
  public Session (int numOfJobs, double[][] alphaVals, double[] alphaDurations, double startTime, double[] periods){
    this.numOfJobs = numOfJobs;
    this.alphas = new Resource[numOfJobs];
    this.periods = new double[numOfJobs]; 
    this.alphaDurations =new double[numOfJobs]; 
    for (int i=0; i<numOfJobs; i++){
      int idx = i % alphaVals.length;
      this.alphas[i] = new Resource(alphaVals[idx]);
      this.periods[i] = periods[idx];
      this.alphaDurations[i] = alphaDurations[idx];
    }
    this.startTime = startTime;
  }
  
  public Session (int numOfJobs, double[][] alphaVals, double[] alphaDurations, double startTime, double period){
    this.numOfJobs = numOfJobs;
    this.alphas = new Resource[numOfJobs];
    this.periods = new double[numOfJobs]; 
    this.alphaDurations =new double[numOfJobs]; 
    for (int i=0; i<numOfJobs; i++){
      int idx = i % alphaVals.length;
      this.alphas[i] = new Resource(alphaVals[idx]);
      this.periods[i] = period;
      this.alphaDurations[i] = alphaDurations[idx];
    }
    this.startTime = startTime;
  }
  
  public int getNumOfJobs() {
    return numOfJobs;
  }

  private int getPeriodIdx(double currTime){
    int idx = -1;
    double startPeriodTime = this.startTime;
    for (int i=0; i<this.numOfJobs; i++){
      double endPeriodTime = startPeriodTime + this.periods[i];
      if(currTime>=startPeriodTime && currTime<endPeriodTime)
        return i;
      startPeriodTime = endPeriodTime;
    }
    System.err.println("[Sesssion] Increase the number of jobs, "+this.numOfJobs + " at " + Simulator.CURRENT_TIME);
    return idx;
  }
  
  public Resource getAlpha(double currTime){
    int idx = this.getPeriodIdx(currTime);
    if(idx<=0)
      return new Resource(0.0);
    return this.alphas[this.getPeriodIdx(currTime)];
  }
  
  public Resource[] getAlphas(){
    return this.alphas;
  }
  
  public double getStartTime() {
    return startTime;
  }
  
  /*public double getStartPeriodTime(double currTime){
    double periodIdx = (currTime - startTime)/period;
    int pIdx = (int) periodIdx;
    return pIdx*period + this.startTime;
  }*/
  public double getStartPeriodTime(double currTime){
    double startPeriodTime = this.startTime;
    for (int i=0; i<this.numOfJobs; i++){
      double endPeriodTime = startPeriodTime + this.periods[i];
      if(currTime>=startPeriodTime && currTime<endPeriodTime)
        return startPeriodTime;
      startPeriodTime = endPeriodTime;
    }
    return -1.0;
  }
  
  public double getEndTime(){
    double endPeriodTime = this.startTime;
    for (int i=0; i<numOfJobs; i++)
      endPeriodTime = endPeriodTime + this.periods[i];
    return endPeriodTime;
  }
  
  public double getStartPeriodTime(int jobId){
    double startPeriodTime = this.startTime;
    for (int i=0; i<jobId; i++)
      startPeriodTime = startPeriodTime + this.periods[i];
    return startPeriodTime;
  }

  public double getPeriod(double currTime) {
    return this.periods[this.getPeriodIdx(currTime)];
  }

  
  public double getAlphaDuration(double currTime){
    return this.alphaDurations[this.getPeriodIdx(currTime)];
  }
  
  public double[] getAlphaDurations(){
    return this.alphaDurations;
  }
  
  public double[] getPeriods(){
    return this.periods;
  }
  
  public void reject(){
    this.isReject = true;
  }
  
  public boolean isReject(){
    return this.isReject;
  }
}
