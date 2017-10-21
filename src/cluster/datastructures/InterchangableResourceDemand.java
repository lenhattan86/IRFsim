package cluster.datastructures;

import cluster.simulator.Main.Globals;

public class InterchangableResourceDemand {
  private double gpuCpu = 0.0;
  private double memory = 0.0;
  private double beta = 1.0;
  
  public InterchangableResourceDemand(double gc, double m, double b){
    this.gpuCpu = gc;
    this.memory = m;
    this.beta = b;
  }
  
  public double convertToCPU(){
    return gpuCpu;
  }
  
  public double getGpuCpu() {
    return gpuCpu;
  }

  public void setGpuCpu(double gpuCpu) {
    this.gpuCpu = gpuCpu;
  }

  public double getMemory() {
    return memory;
  }

  public void setMemory(double memory) {
    this.memory = memory;
  }

  public double getBeta() {
    return beta;
  }

  public void setBeta(double beta) {
    this.beta = beta;
  }

  public double convertToGPU(){
    return gpuCpu/beta;
  }
  
  public Resource convertToGPUDemand(){
    double[] res = {0, this.convertToGPU(), this.memory};
    return new Resource(res);
  }
  
  public Resource convertToCPUDemand(){
    double[] res = {this.convertToCPU(), 0, this.memory};
    return new Resource(res);
  }
  
  public double[] convertToResourceArray(){
    double[] res = { this.convertToCPU(),  this.memory/Globals.MEMORY_SCALE_DOWN, this.beta};
    return res;
  }
  
  public void adddWith(InterchangableResourceDemand demand){
    this.gpuCpu += demand.gpuCpu;
    this.memory += demand.memory;
  }
  
  @Override
  public String toString() {    
    return "gc="+ this.gpuCpu + "|mem=" + this.memory + "|beta=" +this.beta;
  }
}