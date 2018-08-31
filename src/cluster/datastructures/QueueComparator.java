package cluster.datastructures;

import java.util.Comparator;

public class QueueComparator implements Comparator<JobQueue>{

	@Override
	public int compare(JobQueue q1, JobQueue q2) {
		int res = 0; 
		if (q1.L > q2.L){
			res = 1;
		}else if (q1.L < q2.L){
			res = -1;
		}
		return res;
	}
}
