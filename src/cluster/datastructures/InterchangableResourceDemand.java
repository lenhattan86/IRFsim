package cluster.datastructures;

import cluster.simulator.Main.Globals;

public class InterchangableResourceDemand {
  public double beta = 0.0;
  
  public boolean isCPU = true;
  
  public double cpu = 0.0;
  public double mem = 0.0;
  public double cpuCompl = 0.0;
  
  public double gpu = 0.0;
  public double gpuMem = 0.0;
  public double gpuCompl = 0.0;  
  
  public InterchangableResourceDemand(double c, double m, double g, double gm, double cCompl, double gComplt){
  	this.cpu = c;
    this.mem = m;
    this.cpuCompl = cCompl;
    
    this.gpu  = g;
    this.gpuMem = gm;
    this.gpuCompl = gComplt;
    
    this.beta = this.cpuCompl/this.gpuCompl * this.cpu/this.gpu;
  }
  
  public boolean isCpuJob(){
  	return beta < 0.0001;
  }
  
  public InterchangableResourceDemand(InterchangableResourceDemand d){
  	this.cpu = d.cpu;
    this.mem = d.mem;
    this.cpuCompl = d.cpuCompl;
    
    this.gpu  = d.gpu;
    this.gpuMem = d.gpuMem;
    this.gpuCompl = d.gpuCompl;
    this.beta = d.beta;
  }
  
  public Resource getCpuDemand(){
  	double res[]  ={cpu, 0, mem};
  	return new Resource(res);
  }

  public Resource getGpuDemand(){
  	double res[]  = {0, gpu, gpuMem};
  	return new Resource(res);
  }  
  
  public Resource getUsage(){
  	
  	if (isCPU){
  		double[] res = {this.cpu, 0.0, this.mem};
  		return new Resource(res);
  	} else {
  		double[] res = {0.0, this.gpu, this.gpuMem};
  		return new Resource(res);
  	}
  }
  
  public double[] convertToResourceArray(){
  	//TODO: do this.
  	double arr[] = {this.cpu, this.mem, this.cpuCompl, this.gpu, this.gpuMem, this.gpuCompl};
  	return arr;
  }
}