package cluster.datastructures;

import java.util.Comparator;

public class ProcessingTimesComparator implements Comparator<ProcessingTime>{
	@Override
	public int compare(ProcessingTime p1, ProcessingTime p2) {
		int res = 0; 
		if (p1.p > p2.p){
			res = 1;
		}else if (p1.p < p2.p){
			res = -1;
		}
		return res;
	}
}
