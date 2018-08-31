package cluster.datastructures;

import java.util.Comparator;

public class JobLengthComparator implements Comparator<BaseJob> {
	private int mode = 0;// 0 -> both CPU & GPU, 1 -> CPU, 2 -> GPU.
	
	public JobLengthComparator(int mode) {
		this.mode = mode;
	}

	@Override
	public int compare(BaseJob job1, BaseJob job2) {
		double p1 = 0;
		double p2 = 0;
		if (this.mode==0){
			p1 = job1.getMinProcessingTime();
			p2 = job2.getMinProcessingTime();
		} else if(this.mode==1){
			p1 = job1.getReportDemand().cpuCompl;
			p2 = job2.getReportDemand().cpuCompl;
		}
		else if(this.mode==2){
			p1 = job1.getReportDemand().gpuCompl;
			p2 = job2.getReportDemand().gpuCompl;
		}
		
		
		int res = 0;
		if (p1 > p2) {
			res = 1;
		} else if (p1 < p2) {
			res = -1;
		}
		return res;
	}
}
