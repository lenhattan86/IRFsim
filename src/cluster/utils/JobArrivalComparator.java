package cluster.utils;

import java.util.Comparator;

import cluster.datastructures.BaseJob;

public class JobArrivalComparator implements Comparator<BaseJob>{

	@Override
	public int compare(BaseJob job1, BaseJob job2) {
		int res = 0; 
		if (job1.arrivalTime > job2.arrivalTime){
			res = 1;
		}else if (job1.arrivalTime < job2.arrivalTime){
			res = -1;
		}
		return res;
	}
}
