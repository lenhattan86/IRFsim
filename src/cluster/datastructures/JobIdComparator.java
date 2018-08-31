package cluster.datastructures;

import java.util.Comparator;

public class JobIdComparator implements Comparator<BaseJob>{
	@Override
	public int compare(BaseJob job1, BaseJob job2) {
		int res = 0; 
		if (job1.dagId > job2.dagId){
			res = 1;
		}else if (job1.dagId < job2.dagId){
			res = -1;
		}
		return res;
	}
}
