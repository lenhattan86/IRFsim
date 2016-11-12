package cluster.speedfair;

import java.util.ArrayList;

import cluster.datastructures.Resources;
import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;

public class ServiceRate {
	private final boolean DEBUG = true;
	public ArrayList<Resources> slopes = new ArrayList<Resources>();
	private ArrayList<Double> curveDurations = new ArrayList<Double>();

	public enum Type {
		max, sum
	};

	private Type type = Type.max;

	public ServiceRate() {
		type = Type.max;
	}

	public ServiceRate(ArrayList<Resources> slopes, ArrayList<Double> curveDurations) {
		this.slopes = slopes;
		this.curveDurations = curveDurations;

	}

	public void addSlope(Resources slope, double duration) {
		this.curveDurations.add(duration);
		this.slopes.add(slope);
	}

	public int getNumOfSlopes() {
		return slopes.size();
	}

	public Resources getGuaranteedRate(double curTime, double startTime) {
		if (startTime < 0)
			return null;
		double mCurrTime = curTime + Globals.STEP_TIME - startTime;
		double xDuration = 0;
		Resources rate = new Resources(0.0);
		for (int iSlope = 0; iSlope < this.getNumOfSlopes(); iSlope++) {
			double prevDuration = xDuration;
			xDuration += this.curveDurations.get(iSlope);
			if (mCurrTime <= xDuration && mCurrTime > prevDuration) {
				rate = this.slopes.get(iSlope);
				break;
			}
		}
		return rate;
	}

	public boolean isBeyondGuaranteedDuration(double curTime, double startTime) {
		double mCurrTime = curTime + Globals.STEP_TIME - startTime;
		double xDuration = 0;
		for (int iSlope = 0; iSlope < this.getNumOfSlopes(); iSlope++) {
			xDuration += this.curveDurations.get(iSlope);
		}
		return mCurrTime > xDuration;
	}
	
	public Resources guaranteedResources(double curTime, double startTime) {
    Resources rate = getGuaranteedRate(curTime, startTime);
    Resources res = new Resources(0);
    if (rate.distinct(Resources.ZEROS))
      res = new Resources(rate);
    else if(this.slopes.size()>0){
      res = new Resources();
      for (int i=0; i< Globals.NUM_DIMENSIONS; i++) {
        double C = Simulator.cluster.getClusterMaxResAlloc().resource(i);
        double ans = (C*Globals.PERIODIC_INTERVAL/(Simulator.QUEUE_LIST.getRunningQueues().size()));
        ans = ans - this.slopes.get(0).resource(i)*this.curveDurations.get(0);
        ans = ans/(Globals.PERIODIC_INTERVAL-this.curveDurations.get(0));
        ans = Math.min(Simulator.cluster.getClusterMaxResAlloc().resource(i), ans);
        ans = Math.max(0, ans);
        res.resources[i]=ans;
      }
    }
    return res;
  }
	
	public Resources getAlpha(){
	  Resources res = new Resources(0);
    if (this.slopes.size()>0)
      res = new Resources(this.slopes.get(0));
    return res;
	}

	
	public ArrayList<Double> getCurveDurations(){
	  return this.curveDurations;
	}

}
