package cluster.datastructures;

import cluster.simulator.Simulator;

public class ProcessingTime {
	public double p = 0;
	public boolean isCpu = false;
	public BaseJob job;
	
	public ProcessingTime(boolean isCpu, BaseJob job){
		this.job = job;
		this.isCpu = isCpu;
		if(isCpu)
			p = job.getReportDemand().cpuCompl;
		else
			p = job.getReportDemand().gpuCompl;
	}
}
