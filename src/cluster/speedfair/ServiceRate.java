package cluster.speedfair;

import java.util.ArrayList;

import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;

public class ServiceRate {
	private final boolean DEBUG = true;
	private ArrayList<Double> slopes = new ArrayList<Double>();
	private ArrayList<Double> curveDurations = new ArrayList<Double>();

	public ServiceRate() {
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
		double mCurrTime = curTime + Globals.STEP_TIME;
		double xDuration = 0;
		for (int iSlope = 0; iSlope < this.getNumOfSlopes(); iSlope++) {
			xDuration += this.curveDurations.get(iSlope);
		}
		return mCurrTime > xDuration;
	}
}
