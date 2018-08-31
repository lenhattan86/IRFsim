package cluster.datastructures;

import java.util.Comparator;

public class JobProcessingTimeComparator implements Comparator<BaseJob>{

	@Override
	public int compare(BaseJob job1, BaseJob job2) {
		int res = 0; 
		double v1 = job1.getPForAlloX();
		double v2 = job2.getPForAlloX();
		if (v1 < v2){
			res = 1;
		}else if (v1 > v2){
			res = -1;
		}
		return res;
	}
}
