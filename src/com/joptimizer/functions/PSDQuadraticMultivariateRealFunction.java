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
import cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleEigenvalueDecomposition;

/**
 * 1/2 * x.P.x + q.x + r,
 * P symmetric and positive semi-definite
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class PSDQuadraticMultivariateRealFunction extends QuadraticMultivariateRealFunction implements ConvexMultivariateRealFunction {

	public PSDQuadraticMultivariateRealFunction(double[][] PMatrix,	double[] qVector, double r) {
		this((PMatrix!=null)? DoubleFactory2D.dense.make(PMatrix) : null, (qVector!= null)? DoubleFactory1D.dense.make(qVector) : null, r, false);
	}
	
	public PSDQuadraticMultivariateRealFunction(DoubleMatrix2D PMatrix,	DoubleMatrix1D qVector, double r) {
		this(PMatrix, qVector, r, false);
	}
	
	public PSDQuadraticMultivariateRealFunction(double[][] PMatrix,	double[] qVector, double r, boolean checkPSD) {
		this(DoubleFactory2D.dense.make(PMatrix), DoubleFactory1D.dense.make(qVector), r, checkPSD);
	}
	
	public PSDQuadraticMultivariateRealFunction(DoubleMatrix2D PMatrix,	DoubleMatrix1D qVector, double r, boolean checkPSD) {
		super(PMatrix, qVector, r);
		if(checkPSD){
			DenseDoubleEigenvalueDecomposition eDecomp = new DenseDoubleEigenvalueDecomposition(P);
			DoubleMatrix1D realEigenvalues = eDecomp.getRealEigenvalues();
			for (int i = 0; i < realEigenvalues.size(); i++) {
				if (realEigenvalues.get(i) < 0) {
					throw new IllegalArgumentException("Not positive semi-definite matrix");
				}
			}
		}
	}
}
