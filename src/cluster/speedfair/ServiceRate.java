package cluster.speedfair;

import java.util.ArrayList;

import cluster.datastructures.Resources;
import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;

public class ServiceRate {
	private final boolean DEBUG = true;
	private ArrayList<Double> slopes = new ArrayList<Double>();
	private ArrayList<Double> curveDurations = new ArrayList<Double>();

	public enum Type {
		max, sum
	};

	private Type type = Type.max;

	public ServiceRate() {
		type = Type.max;
	}

	public ServiceRate(ArrayList<Double> slopes, ArrayList<Double> curveDurations) {
		this.slopes = slopes;
		this.curveDurations = curveDurations;

	}

	public void addSlope(double slope, double duration) {
		this.curveDurations.add(duration);
		this.slopes.add(slope);
	}

	public int getNumOfSlopes() {
		return slopes.size();
	}

	public double getGuaranteedRate(double curTime, double startTime) {
		if (startTime < 0)
			return 0.0;
		double mCurrTime = curTime + Globals.STEP_TIME - startTime;
		double xDuration = 0;
		double rate = 0.0;
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

	public boolean isBeyongGuaranteedDuration(double curTime, double startTime) {
		double mCurrTime = curTime + Globals.STEP_TIME - startTime;
		double xDuration = 0;
		for (int iSlope = 0; iSlope < this.getNumOfSlopes(); iSlope++) {
			xDuration += this.curveDurations.get(iSlope);
		}
		return mCurrTime > xDuration;
	}

	public Resources guaranteedResources(Resources demand, double curTime, double startTime) {
		Resources res = new Resources();
		if (this.type == Type.max) {
			int idxOfMax = demand.idxOfMax();
			double maxVal = demand.resource(idxOfMax);
			Resources normalizedDemand = Resources.divide(demand, maxVal);
			double rate = getGuaranteedRate(curTime, startTime);
			res = Resources.multiply(normalizedDemand, rate);
		} else if(this.type == Type.sum) {
			double sumVal = demand.sum();
			Resources normalizedDemand = Resources.divide(demand, sumVal);
			double rate = getGuaranteedRate(curTime, startTime);
			res = Resources.multiply(normalizedDemand, rate);
		} else{
			System.err.println("[ServiceRate] type (max or sum) is not legal.");
		}

		return res;
	}

}
