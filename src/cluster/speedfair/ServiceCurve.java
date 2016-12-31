package cluster.speedfair;

import java.util.ArrayList;

import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Main.Globals;

public class ServiceCurve {
	private final boolean DEBUG = true;
	private ArrayList<Double> slopes= new ArrayList<Double>();
	private ArrayList<Double> curveDurations= new ArrayList<Double>();
	
	public ServiceCurve(){
	}
	
	public ServiceCurve(ArrayList<Double> slopes, ArrayList<Double> curveDurations){
		this.slopes = slopes;
		this.curveDurations = curveDurations;
		
	}
	
	public void addSlope(double slope, double duration){
		this.curveDurations.add(duration);
		this.slopes.add(slope);
	}
	
	public int getNumOfSlopes(){
		return slopes.size();
	}
	
	public Resource getMinReqService(double runTime){
		runTime = runTime + Globals.STEP_TIME;
		double xDuration = 0;
		double minVal = 0;
		int slopeIdx = 0;
		for (int iSlope=0; iSlope < this.getNumOfSlopes(); iSlope++) {
			double prevDuration = xDuration;
			xDuration += this.curveDurations.get(iSlope);
			if(runTime <= xDuration && runTime >prevDuration){
				slopeIdx = iSlope; 
				break;
			}
		}
		
		xDuration = 0;
		for (int iSlope=0; iSlope < slopeIdx; iSlope++) {
			xDuration += this.curveDurations.get(iSlope);
			minVal += this.slopes.get(iSlope)*xDuration;
		}
		minVal += (runTime-xDuration)*this.slopes.get(slopeIdx);
		
//		Output.debugln(DEBUG, " Service Curve at " +runTime+ " is " + minVal);
		// scale up the requirement.
		Resource minService = Resources.initResources(false, minVal);
		return minService;
	}
	
	public ArrayList<Double>getSlopes(){
		return this.slopes;
	}
	
	public ArrayList<Double>getCurveDurations(){
		return this.curveDurations;
	}
	
	// only need to satisfy the max resource.
	public boolean isSatisfied(Resource receivedResources, double runTime){
		int maxIdx = receivedResources.idOfMaxResource();
		Resource minReq = getMinReqService(runTime);
		return minReq.resource(maxIdx)>=minReq.resource(maxIdx);
	}
}
