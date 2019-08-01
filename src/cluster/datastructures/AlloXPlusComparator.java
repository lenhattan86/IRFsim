package cluster.datastructures;

import java.util.Comparator;

import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;

public class AlloXPlusComparator implements Comparator<ProcessingTime>{
	@Override
	public int compare(ProcessingTime p1, ProcessingTime p2) {
		int p1Wait = (int) (Simulator.CURRENT_TIME - p1.job.arrivalTime);
		int p2Wait = (int) (Simulator.CURRENT_TIME - p2.job.arrivalTime);
		
		if(p1Wait >= Globals.ALLOX_TIME_OUT && p2Wait < Globals.ALLOX_TIME_OUT) {
			return -1;
		} 
//		
		if(p1Wait < Globals.ALLOX_TIME_OUT && p2Wait >= Globals.ALLOX_TIME_OUT) {
			return 1;
		}
//		
		if(p1Wait >= Globals.ALLOX_TIME_OUT && p2Wait >= Globals.ALLOX_TIME_OUT && (p2Wait!=p1Wait)) {
//			return Integer.signum(p2Wait-p1Wait);
			return Integer.signum((int) (p1.p - p2.p));
		}
//		return Integer.signum(p2Wait-p1Wait);
		return Integer.signum((int) (p1.p - p2.p));
			
//		if (p1.p > p2.p){
//			res = 1;
//		}else if (p1.p < p2.p){
//			res = -1;
//		}
//		return res;
	}
}
