package cluster.datastructures;

import cluster.simulator.Simulator;

public class ProcessingTime {
	public double p = 0;
	public boolean isCpu = false;
	public BaseJob job;
	
	public ProcessingTime(boolean isCpu, BaseJob job){
		this.job = job;
		this.isCpu = isCpu;
		if (job.jobStartRunningTime <0 )
			if(isCpu)
				p = job.getReportDemand().cpuCompl;
			else
				p = job.getReportDemand().gpuCompl;
		else {
			double pastTime = Simulator.CURRENT_TIME - job.jobStartRunningTime;
			boolean isRunningOnCpu = !job.isCpu;
			if(isCpu){
				if (isRunningOnCpu)					
					p = job.getReportDemand().cpuCompl - pastTime;
				else
					p = job.getReportDemand().cpuCompl - (pastTime/job.getReportDemand().gpuCompl*job.getReportDemand().cpuCompl);
			} else {
				if (isRunningOnCpu)					
					p = job.getReportDemand().gpuCompl -  (pastTime/job.getReportDemand().cpuCompl*job.getReportDemand().gpuCompl);
				else
					p = job.getReportDemand().gpuCompl - pastTime;
			}
		}
		this.job.wasScheduled = false;
	}
}
