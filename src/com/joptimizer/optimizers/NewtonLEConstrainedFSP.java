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
package com.joptimizer.optimizers;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joptimizer.exception.IterationsLimitException;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.solvers.BasicKKTSolver;
import com.joptimizer.solvers.KKTSolver;
import com.joptimizer.util.ColtUtils;
import com.joptimizer.util.Utils;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * Linear equality constrained newton optimizer, with feasible starting point.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 521"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class NewtonLEConstrainedFSP extends OptimizationRequestHandler {

	private KKTSolver kktSolver;
	private static Log log = LogFactory.getLog(NewtonLEConstrainedFSP.class.getName());
	
	public NewtonLEConstrainedFSP(boolean activateChain){
		if(activateChain){
			this.successor = new NewtonLEConstrainedISP(true);
		}
	}
	
	public NewtonLEConstrainedFSP(){
		this(false);
	}

	@Override
	public void optimize() throws JOptimizerException {
		if(log.isDebugEnabled()){
			log.debug("optimize");
		}
		OptimizationResponse response = new OptimizationResponse();
		
	    //checking responsibility
		if (getFi() != null) {
			// forward to the chain
			forwardOptimizationRequest();
			return;
		}
		
		long tStart = System.currentTimeMillis();
		
		//initial point must be feasible (i.e., satisfy x in domF and Ax = b).
		DoubleMatrix1D X0 = getInitialPoint();
		double rPriX0Norm = (X0 != null)? ALG.norm2(rPri(X0)) : 0d;
		//if (X0 == null	|| (getA()!=null && Double.compare(ALG.norm2(getA().zMult(X0, getB().copy(), 1., -1., false)), 0d) != 0)) {
		//if (X0 == null	|| rPriX0Norm > Utils.getDoubleMachineEpsilon()) {	
		if (X0 == null	|| rPriX0Norm > getTolerance()) {	
			// infeasible starting point, forward to the chain
			forwardOptimizationRequest();
			return;
		}
		
		if(log.isDebugEnabled()){
			log.debug("X0:  " + ArrayUtils.toString(X0.toArray()));
		}
		DoubleMatrix1D X = X0;
		double F0X;
		//double previousF0X = Double.NaN;
		double previousLambda = Double.NaN;
		int iteration = 0;
		while (true) {
			iteration++;
			F0X = getF0(X);
			if(log.isDebugEnabled()){
				log.debug("iteration " + iteration);
				log.debug("X=" + ArrayUtils.toString(X.toArray()));
				log.debug("f(X)=" + F0X);
			}
			
//			if(!Double.isNaN(previousF0X)){
//				if (previousF0X < F0X) {
//					throw new Exception("critical minimization problem");
//				}
//			}
//			previousF0X = F0X;
			
			// custom exit condition
			if(checkCustomExitConditions(X)){
				break;
			}
			
			DoubleMatrix1D gradX = getGradF0(X);
			DoubleMatrix2D hessX = getHessF0(X);
			
			double gradXNorm = ALG.norm2(gradX);
			if(gradXNorm < Utils.getDoubleMachineEpsilon()){
				break;
			}

			// Newton step and decrement
			if(this.kktSolver==null){
				//this.kktSolver = new BasicKKTSolver();
				this.kktSolver = new BasicKKTSolver();
			}
			if(isCheckKKTSolutionAccuracy()){
				kktSolver.setCheckKKTSolutionAccuracy(isCheckKKTSolutionAccuracy());
				kktSolver.setToleranceKKT(getToleranceKKT());
			}
			kktSolver.setHMatrix(hessX);
			kktSolver.setGVector(gradX);
			if(getA()!=null){
				kktSolver.setAMatrix(getA());
			}
			DoubleMatrix1D[] sol = kktSolver.solve();
			DoubleMatrix1D step = sol[0];
			DoubleMatrix1D w = (sol[1]!=null)? sol[1] : F1.make(0);
			if(log.isDebugEnabled()){
				log.debug("stepX: " + ArrayUtils.toString(step.toArray()));
				log.debug("w    : " + ArrayUtils.toString(w.toArray()));
			}

			// exit condition: check the Newton decrement
			double lambda = Math.sqrt(ALG.mult(step, ALG.mult(hessX, step)));
			if(log.isDebugEnabled()){
				log.debug("lambda: " + lambda);
			}
			if (lambda / 2. <= getTolerance()) {
				break;
			}
			
			// iteration limit condition
			if (iteration == getMaxIteration()) {
				log.error(IterationsLimitException.MAX_ITERATIONS_EXCEEDED);
				throw new IterationsLimitException(IterationsLimitException.MAX_ITERATIONS_EXCEEDED);
			}
			
		    // progress conditions
			if(isCheckProgressConditions()){
				if (!Double.isNaN(previousLambda) && previousLambda <= lambda) {
					log.error("No progress achieved, exit iterations loop without desired accuracy");
					throw new JOptimizerException("No progress achieved, exit iterations loop without desired accuracy");
				}
			}
			previousLambda = lambda;

			// backtracking line search
			double s = 1d;
			DoubleMatrix1D X1 = null;
			int cnt = 0;
			while (cnt < 250) {
				cnt++;
				// @TODO: can we use simplification 9.7.1 ??
				
				// x + s*step: note that there are not inequalities to check
				//X1 = X.copy().assign(step.copy().assign(Mult.mult(s)), Functions.plus);
				X1 = ColtUtils.sum(X, step, s);
				//log.debug("X1: "+ArrayUtils.toString(X1.toArray()));
				
				if (isInDomainF0(X1)) {
					double condSX = getF0(X1);
					//NB: this will also check !Double.isNaN(getF0(X1))
					double condDX = F0X + getAlpha() * s * ALG.mult(gradX, step);
					//log.debug("condSX: "+condSX);
					//log.debug("condDX: "+condDX);
					if (condSX <= condDX) {
						break;
					}
				}
				s = getBeta() * s;
			}
			if(log.isDebugEnabled()){
				log.debug("s: " + s);
			}

			// update
			X = X1;
		}

		long tStop = System.currentTimeMillis();
		if(log.isDebugEnabled()){
			log.debug("time: " + (tStop - tStart));
		}
		response.setSolution(X.toArray());
		setOptimizationResponse(response);
	}
	
	public void setKKTSolver(KKTSolver kktSolver) {
		this.kktSolver = kktSolver;
	}
}
