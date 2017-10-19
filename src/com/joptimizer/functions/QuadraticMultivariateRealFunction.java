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
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.DoubleProperty;

import com.joptimizer.util.ColtUtils;

/**
 * Represent a function in the form of
 * <br>f(x) := 1/2 x.P.x + q.x + r
 * <br>where x, q &#8712; R<sup>n</sup>, P is a symmetric nXn matrix and r &#8712; R.
 * 
 * <br>NOTE1: remember the two following propositions hold:
 * <ol>
 * 	<li>A function f(x) is a quadratic form if and only if it can be written as 
 * f(x) = x.P.x 
 * for a symmetric matrix P (f can even be written as x.P1.x with P1 not symmetric, for example 
 * <br>f = x^2 + 2 x y + y^2 we can written with P={{1, 1}, {1, 1}} symmetric or 
 * with P1={{1, -1}, {3, 1}} not symmetric, but here we are interested in 
 * symmetric matrices for they convexity properties).</li>
 * 	<li>Let f(x) = x.P.x be a quadratic form with associated symmetric matrix P, then we have:
 * 		<ul>
 * 			<li>f is convex <=> P is positive semidefinite</li>
 * 			<li>f is concave <=> P is negative semidefinite</li>
 * 			<li>f is strictly convex <=> P is positive definite</li>
 * 			<li>f is strictly concave <=> P is negative definite</li>
 * 		</ul>
 *  </li>
 * </ol>
 * 
 * NOTE2: precisely speaking, this class should have been named "PolynomialOfDegreeTwo", because
 * by definition a quadratic form in the variables x1,x2,...,xn is a polynomial function where all terms 
 * in the functional expression have order two. A general polynomial function f(x) of degree two can be written
 * as the sum of a quadratic form Q = x.P.x and a linear form L = q.x  (plus a constant term r):
 * <br>f(x) = Q + L + r
 * <br>Because L is convex, f is convex if so is Q.
 *  
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * @see "Eivind Eriksen, Quadratic Forms and Convexity"
 */
public class QuadraticMultivariateRealFunction implements TwiceDifferentiableMultivariateRealFunction {

	/**
	 * Dimension of the function argument.
	 */
	protected int dim = -1;

	/**
	 * Quadratic factor.
	 */
	protected DoubleMatrix2D P = null;

	/**
	 * Linear factor.
	 */
	protected DoubleMatrix1D q = null;

	/**
	 * Constant factor.
	 */
	protected double r = 0;

	private DoubleMatrix2D cachedHessian = null;
	
	private DenseDoubleAlgebra ALG = DenseDoubleAlgebra.DEFAULT;
	
	public QuadraticMultivariateRealFunction(DoubleMatrix2D PMatrix, DoubleMatrix1D qVector, double r, boolean checkSymmetry){
		this.P = PMatrix;
		this.q = qVector;
		this.r = r;
		
		if(P==null && q==null){
			throw new IllegalArgumentException("Impossible to create the function");
		}
		if (P != null && !DoubleProperty.DEFAULT.isSquare(P)) {
			throw new IllegalArgumentException("P is not square");
		}
		if (P != null && checkSymmetry && !DoubleProperty.DEFAULT.isSymmetric(P)) {
			throw new IllegalArgumentException("P is not symmetric");
		}
		
		this.dim = (int) ((P != null)? P.columns() : q.size());
		if (this.dim < 0) {
			throw new IllegalArgumentException("Impossible to create the function");
		}
  }

	public QuadraticMultivariateRealFunction(DoubleMatrix2D PMatrix, DoubleMatrix1D qVector, double r) {
		this(PMatrix, qVector, r, false);
	}

	public final double value(DoubleMatrix1D X) {
		double ret = r;
		if (P != null) {
			ret += 0.5 * ALG.mult(X, ALG.mult(P, X));
		}
		if (q != null) {
			ret += ALG.mult(q, X);
		}
		return ret;
	}

	public final DoubleMatrix1D gradient(DoubleMatrix1D X) {
		DoubleMatrix1D ret = null;
		if(P!=null){
			if (q != null) {
				//P.x + q
				//ret = P.zMult(x, q.copy(), 1, 1, false);
				ret = ColtUtils.zMult(P, X, q, 1);
			} else {
				ret = ALG.mult(P, X);
			}
		}else{
			ret = q;
		}
		return ret;
		
	}

	public final DoubleMatrix2D hessian(DoubleMatrix1D X) {
		if (cachedHessian != null) {
			return cachedHessian;
		}

		DoubleMatrix2D ret = null;
		if(P!=null){
			ret = P;
		}else{
			//ret = DoubleFactory2D.dense.make(dim, dim);
			return FunctionsUtils.ZEROES_MATRIX_PLACEHOLDER;
		}

		cachedHessian = ret;
		return cachedHessian;
	}

	public DoubleMatrix2D getP() {
		return P;
	}

	public DoubleMatrix1D getQ() {
		return q;
	}

	public double getR() {
		return r;
	}

	public int getDim() {
		return this.dim;
	}

}
