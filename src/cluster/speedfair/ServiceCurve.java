package cluster.speedfair;

import java.util.ArrayList;

import cluster.datastructures.Resources;

public class ServiceCurve {
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
	
	public Resources getMinReqService(double runTime){
		double xDuration = 0;
		double minVal = 0;
		for (int iSlope=0; iSlope < this.getNumOfSlopes(); iSlope++) {
			if(runTime >= xDuration){
				minVal +=  (runTime-xDuration)*this.slopes.get(iSlope);
				break;
			} else {
				minVal +=  xDuration*this.slopes.get(iSlope);
				xDuration += this.curveDurations.get(iSlope);
			}
		}
		Resources minService = new Resources(minVal);
		return minService;
	}
	
	public ArrayList<Double>getSlopes(){
		return this.slopes;
	}
	
	public ArrayList<Double>getCurveDurations(){
		return this.curveDurations;
	}
}
