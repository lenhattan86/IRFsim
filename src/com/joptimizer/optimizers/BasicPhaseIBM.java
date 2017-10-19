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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;

import com.joptimizer.exception.InfeasibleProblemException;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.functions.BarrierFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.util.ColtUtils;

/**
 * Basic Phase I Method (implemented as a Barried Method).
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 579"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class BasicPhaseIBM {
	
	private BarrierMethod originalProblem;
	private int originalDim =-1;
	private int dim =-1;
	private DenseDoubleAlgebra ALG = DenseDoubleAlgebra.DEFAULT;
	private DoubleFactory1D F1 = DoubleFactory1D.dense;
	private DoubleFactory2D F2 = DoubleFactory2D.dense;
	private static Log log = LogFactory.getLog(BasicPhaseIBM.class.getName());

	public BasicPhaseIBM(BarrierMethod originalProblem) {
		this.originalProblem = originalProblem;
		originalDim = originalProblem.getDim();
		this.dim = originalProblem.getDim()+1;//variable Y=(X, s)
	}
	
	public double[] findFeasibleInitialPoint() throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("findFeasibleInitialPoint");
		}
		
		OptimizationRequest or = new OptimizationRequest();
		
		//objective function: s
		DoubleMatrix1D C = F1.make(dim);
		C.set(dim-1, 1.);
		LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(C, 0);
		or.setF0(objectiveFunction);
		or.setToleranceFeas(originalProblem.getToleranceFeas());
		or.setTolerance(originalProblem.getTolerance());
		or.setCheckKKTSolutionAccuracy(originalProblem.isCheckKKTSolutionAccuracy());
		
//		// Inequality constraints: fi(X)-s
//		ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[originalProblem.getFi().length];
//		for(int i=0; i<inequalities.length; i++){
//			
//			final ConvexMultivariateRealFunction originalFi = originalProblem.getFi()[i];
//			
//			ConvexMultivariateRealFunction fi = new ConvexMultivariateRealFunction() {
//				
//				public double value(double[] Y) {
//					DoubleMatrix1D y = DoubleFactory1D.dense.make(Y);
//					DoubleMatrix1D X = y.viewPart(0, originalDim);
//					return originalFi.value(X.toArray()) - y.get(dim-1);
//				}
//				
//				public double[] gradient(double[] Y) {
//					DoubleMatrix1D y = DoubleFactory1D.dense.make(Y);
//					DoubleMatrix1D X = y.viewPart(0, originalDim);
//					DoubleMatrix1D origGrad = F1.make(originalFi.gradient(X.toArray()));
//					DoubleMatrix1D ret = F1.make(1, -1);
//					ret = F1.append(origGrad, ret);
//					return ret.toArray();
//				}
//				
//				public double[][] hessian(double[] Y) {
//					DoubleMatrix1D y = DoubleFactory1D.dense.make(Y);
//					DoubleMatrix1D X = y.viewPart(0, originalDim);
//					DoubleMatrix2D origHess = F2.make(originalFi.hessian(X.toArray()));
//					DoubleMatrix2D[][] parts = new DoubleMatrix2D[][]{{origHess, null},{null,F2.make(1, 1)}};
//					return F2.compose(parts).toArray();
//				}
//				
//				public int getDim() {
//					return dim;
//				}
//			};
//			inequalities[i] = fi;
//		}
		//or.setFi(inequalities);
		
	  // Equality constraints: add a final zeroes column
		DoubleMatrix2D AEorig = originalProblem.getA();
		DoubleMatrix1D BEorig = originalProblem.getB();
		if(AEorig!=null){
			DoubleMatrix2D zeroCols = F2.make(AEorig.rows(), 1); 
			DoubleMatrix2D[][] parts = new DoubleMatrix2D[][]{{AEorig, zeroCols}};
			DoubleMatrix2D AE = F2.compose(parts);
			DoubleMatrix1D BE = BEorig;
			or.setA(AE);
			or.setB(BE);
		}
		
		//initial point
		DoubleMatrix1D X0 = originalProblem.getNotFeasibleInitialPoint();
		if(X0==null){
			if(AEorig!=null){
				X0 = findOneRoot(AEorig, BEorig);
			}else{
				X0 = F1.make(originalProblem.getDim(), 1./originalProblem.getDim());
			}
		}
		
		//check primal norm
		if (AEorig!=null) {
			//DoubleMatrix1D originalRPriX0 = AEorig.zMult(X0, BEorig.copy(), 1., -1., false);
			DoubleMatrix1D originalRPriX0 = ColtUtils.zMult(AEorig, X0, BEorig, -1);
			if(ALG.norm2(originalRPriX0) > originalProblem.getToleranceFeas()){
				throw new JOptimizerException("The initial point for Basic Phase I Method must be equalities-feasible");
			}
		}
		
		//DoubleMatrix1D originalFiX0 = originalProblem.getFi(X0);
		double s = originalProblem.getBarrierFunction().calculatePhase1InitialFeasiblePoint(X0, originalProblem.getToleranceFeas());
		
		//lucky strike?
//		int maxIneqIndex = Utils.getMaxIndex(originalFiX0);
//		if(originalFiX0.get(maxIneqIndex) + originalProblem.getTolerance()<0){
//			//the given notFeasible starting point is in fact already feasible
//			return X0;
//		}
		if(s < 0){
		//the given notFeasible starting point is already feasible
			return X0.toArray();
		}
		
//		DoubleMatrix1D initialPoint = F1.make(1, -Double.MAX_VALUE); 
//		initialPoint = F1.append(X0, initialPoint);
//		for(int i=0; i<originalFiX0.size(); i++){
//			//initialPoint.set(dim-1, Math.max(initialPoint.get(dim-1), originalFiX0.get(i)+Math.sqrt(originalProblem.getToleranceFeas())));
//			initialPoint.set(dim-1, Math.max(initialPoint.get(dim-1), originalFiX0.get(i)*Math.pow(originalProblem.getToleranceFeas(),-2)));
//		}
		
		DoubleMatrix1D initialPoint = F1.make(1, s);
		initialPoint = F1.append(X0, initialPoint);
		or.setInitialPoint(initialPoint.toArray());
		
	    //optimization
		BarrierFunction bfPh1 = originalProblem.getBarrierFunction().createPhase1BarrierFunction();
		BarrierMethod opt = new PhaseIBarrierMethod(bfPh1);
		opt.setOptimizationRequest(or);
		opt.optimize();
		OptimizationResponse response = opt.getOptimizationResponse();
//		DoubleMatrix1D sol = F1.make(response.getSolution());
//		DoubleMatrix1D ret = sol.viewPart(0, originalDim);
//		DoubleMatrix1D ineq = originalProblem.getFi(ret);
//		maxIneqIndex = Utils.getMaxIndex(ineq);
//		if(log.isDebugEnabled()){
//			log.debug("ineq        : "+ArrayUtils.toString(ineq.toArray()));
//			log.debug("max ineq pos: "+maxIneqIndex);
//			log.debug("max ineq val: "+ineq.get(maxIneqIndex));
//		}
//		//if(sol[dim-1]>0){
//		if(ineq.get(maxIneqIndex)>=0){	
//			throw new Exception(JOptimizer.INFEASIBLE_PROBLEM);
//		}
		//double[] originalSolution = Arrays.copyOf(response.getSolution(), originalDim);
		double[] originalSolution = new double[originalDim];
		System.arraycopy(response.getSolution(), 0, originalSolution, 0, originalDim);
		if(Double.isNaN(originalProblem.getBarrierFunction().value(F1.make(originalSolution)))){
			throw new InfeasibleProblemException();
		}

		return originalSolution;
	}
	
	private class PhaseIBarrierMethod extends BarrierMethod{
		
		public PhaseIBarrierMethod(BarrierFunction barrierFunction) {
			super(barrierFunction);
		}

		@Override
		protected boolean checkCustomExitConditions(DoubleMatrix1D Y){
			DoubleMatrix1D X = Y.viewPart(0, getDim()-1);
			
			//equalities
			DoubleMatrix1D originalRPriX = F1.make(0);
			if(getA()!=null){
				//originalRPriX = originalProblem.getA().zMult(X, originalProblem.getB().copy(), 1., -1., false);
				originalRPriX = ColtUtils.zMult(originalProblem.getA(), X, originalProblem.getB(), -1.);
			}
			boolean b2 = ALG.norm2(originalRPriX) < originalProblem.getToleranceFeas();
			
			//inequalities
			boolean b1 = !Double.isNaN(originalProblem.getBarrierFunction().value(X)) || Y.getQuick((int) (Y.size()-1))<0;
			
			if(log.isDebugEnabled()){
				log.debug("checkCustomExitConditions: " + (b1 && b2));
			}
			return b1 && b2;
		}
	}
	
	/**
	 * Just looking for one out of all the possible solutions.
	 * @see "Convex Optimization, C.5 p. 681".
	 */
	private DoubleMatrix1D findOneRoot(DoubleMatrix2D A, DoubleMatrix1D b) throws JOptimizerException{
		return originalProblem.findEqFeasiblePoint(A, b);
	}
}
