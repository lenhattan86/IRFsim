/*
 * Copyright 2011-2017 joptimizer.com
 *
 * This work is licensed under the Creative Commons Attribution-NoDerivatives 4.0 
 * International License. To view a copy of this license, visit 
 *
 *        http://creativecommons.org/licenses/by-nd/4.0/ 
 *
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */
package com.joptimizer.functions;

import cern.colt.matrix.tdouble.DoubleMatrix1D;

/**
 * Interface for the barrier function used by a given barrier optimization method.
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.2"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public interface BarrierFunction extends TwiceDifferentiableMultivariateRealFunction{

	/**
	* Calculates the duality gap for a barrier method build with this barrier function. 
	*/
	public double getDualityGap(double t);
	
	/**
	 * Create the barrier function for the basic Phase I method.
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.4.1"
	 */
	public BarrierFunction createPhase1BarrierFunction();
	
	/**
	 * Calculates the initial value for the additional variable s in basic Phase I method.
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.4.1"
	 */
	public double calculatePhase1InitialFeasiblePoint(DoubleMatrix1D originalNotFeasiblePoint, double tolerance);
}
