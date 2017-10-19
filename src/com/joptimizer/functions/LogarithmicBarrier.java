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

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;

import com.joptimizer.util.ColtUtils;

/**
 * Default barrier function for the barrier method algorithm.
 * <br>If f_i(x) are the inequalities of the problem, theh we have:
 * <br><i>&Phi;</i> = - Sum_i[log(-f_i(x))]
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.2.1"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class LogarithmicBarrier implements BarrierFunction {

	final DenseDoubleAlgebra ALG = DenseDoubleAlgebra.DEFAULT;
	final DoubleFactory1D F1 = DoubleFactory1D.dense;
	final DoubleFactory2D F2 = DoubleFactory2D.dense;
	private ConvexMultivariateRealFunction[] fi = null;
	private int dim = -1;

	/**
	 * Create the logarithmic barrier function.
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.2.1"
	 */
	public LogarithmicBarrier(ConvexMultivariateRealFunction[] fi, int dim) {
		this.fi = fi;
		this.dim = dim;
	}

	public double value(DoubleMatrix1D X) {
		double psi = 0;
		for (int j = 0; j < fi.length; j++) {
			double ineqValuejX = fi[j].value(X);
			if (ineqValuejX >= 0) {
				return Double.NaN;
			}
			psi -= Math.log(-ineqValuejX);
		}
		return psi;
	}

	public DoubleMatrix1D gradient(DoubleMatrix1D X) {
		DoubleMatrix1D gradFiSum = F1.make(getDim());
		for (int j = 0; j < fi.length; j++) {
			double ineqValuejX = fi[j].value(X);
			DoubleMatrix1D ineqGradjX = fi[j].gradient(X);
			//DoubleMatrix1D zz = ineqGradjX.assign(DoubleFunctions.mult(-1. / ineqValuejX));
			//gradFiSum.assign(zz, DoubleFunctions.plus);
			gradFiSum = ColtUtils.sum(gradFiSum, ineqGradjX, (-1. / ineqValuejX));
		}
		return gradFiSum;
	}
	
	public DoubleMatrix2D hessian(DoubleMatrix1D X) {
		DoubleMatrix2D HessSum = F2.make(new double[getDim()][getDim()]);
		DoubleMatrix2D GradSum = F2.make(new double[getDim()][getDim()]);
		for (int j = 0; j < fi.length; j++) {
			double ineqValuejX = fi[j].value(X);
			DoubleMatrix2D fijHessianX = fi[j].hessian(X);
			DoubleMatrix2D ineqHessjX = (fijHessianX!=FunctionsUtils.ZEROES_MATRIX_PLACEHOLDER)? fijHessianX : FunctionsUtils.ZEROES_MATRIX_PLACEHOLDER;
			DoubleMatrix1D ineqGradjX = fi[j].gradient(X);
			if(ineqHessjX!=FunctionsUtils.ZEROES_MATRIX_PLACEHOLDER){
				//HessSum.assign(ineqHessjX.assign(DoubleFunctions.mult(-1./ineqValuejX)), DoubleFunctions.plus);
				HessSum = ColtUtils.sum(HessSum, ineqHessjX, (-1./ineqValuejX));
			}
			DoubleMatrix2D aa = ALG.multOuter(ineqGradjX, ineqGradjX, null);
			//DoubleMatrix2D bb = aa.assign(DoubleFunctions.mult(1. / Math.pow(ineqValuejX, 2)));
			//GradSum.assign(bb, DoubleFunctions.plus);
			GradSum = ColtUtils.sum(GradSum, aa, (1. / Math.pow(ineqValuejX, 2)));
		}
		//return HessSum.assign(GradSum, DoubleFunctions.plus);
		return ColtUtils.sum(HessSum, GradSum);
	}
	
	public int getDim() {
		return this.dim;
	}
	
	public double getDualityGap(double t) {
		return ((double)fi.length) / t;
	}
	
	/**
	 * Create the barrier function for the Phase I.
	 * It is a LogarithmicBarrier for the constraints: 
	 * <br>fi(X)-s, i=1,...,n
	 */
	public BarrierFunction createPhase1BarrierFunction(){
		
		final int dimPh1 = dim +1;
		ConvexMultivariateRealFunction[] inequalitiesPh1 = new ConvexMultivariateRealFunction[this.fi.length];
		for(int i=0; i<inequalitiesPh1.length; i++){
			
			final ConvexMultivariateRealFunction originalFi = this.fi[i];
			
			ConvexMultivariateRealFunction fi = new ConvexMultivariateRealFunction() {
				
				public double value(DoubleMatrix1D Y) {
					DoubleMatrix1D X = Y.viewPart(0, dim);
					return originalFi.value(X) - Y.get(dimPh1-1);
				}
				
				public DoubleMatrix1D gradient(DoubleMatrix1D Y) {
					DoubleMatrix1D X = Y.viewPart(0, dim);
					DoubleMatrix1D origGrad = originalFi.gradient(X);
					DoubleMatrix1D ret = F1.make(1, -1);
					ret = F1.append(origGrad, ret);
					return ret;
				}
				
				public DoubleMatrix2D hessian(DoubleMatrix1D Y) {
					DoubleMatrix1D X = Y.viewPart(0, dim);
					DoubleMatrix2D origHess;
					DoubleMatrix2D origFiHessX = originalFi.hessian(X);
					if(origFiHessX == FunctionsUtils.ZEROES_MATRIX_PLACEHOLDER){
						return FunctionsUtils.ZEROES_MATRIX_PLACEHOLDER;
					}else{
						origHess = origFiHessX;
						DoubleMatrix2D[][] parts = new DoubleMatrix2D[][]{{origHess, null},{null, F2.make(1, 1)}};
						return F2.compose(parts);
					}
				}
				
				public int getDim() {
					return dimPh1;
				}
			};
			inequalitiesPh1[i] = fi;
		}
		
		BarrierFunction bfPh1 = new LogarithmicBarrier(inequalitiesPh1, dimPh1);
		return bfPh1;
	}
	
	/**
	 * Calculates the initial value for the s parameter in Phase I.
	 * Return s = max(fi(x))
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.6.2"
	 */
	public double calculatePhase1InitialFeasiblePoint(DoubleMatrix1D originalNotFeasiblePoint, double tolerance){
		//DoubleMatrix1D X0NF = F1.make(originalNotFeasiblePoint);
		DoubleMatrix1D fiX0NF = F1.make(fi.length);
		for(int i=0; i<fi.length; i++){
			fiX0NF.set(i, this.fi[i].value(originalNotFeasiblePoint));
		}
		
		//lucky strike?
		int maxIneqIndex = ColtUtils.getMaxIndex(fiX0NF);
		if(fiX0NF.get(maxIneqIndex) < 0){
			//the given notFeasible starting point is in fact already feasible
			return -1;
		}
		
		double s = Math.pow(tolerance,-0.5);
		for(int i=0; i<fiX0NF.size(); i++){
			s = Math.max(s, fiX0NF.get(i)*Math.pow(tolerance,-0.5));
		}
		
		return s;
	}
}
