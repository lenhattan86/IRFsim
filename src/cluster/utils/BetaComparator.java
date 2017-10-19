package cluster.utils;

import java.util.Comparator;

import cluster.datastructures.JobQueue;

public class BetaComparator implements Comparator<JobQueue>{

	@Override
	public int compare(JobQueue q1, JobQueue q2) {
		int res = 0; 
		double beta1 = q1.getReportBeta();
		double beta2 = q2.getReportBeta();
		if (beta1 > beta2){
			res = 1;
		}else if (beta1 < beta2){
			res = -1;
		}
		return res;
	}
}