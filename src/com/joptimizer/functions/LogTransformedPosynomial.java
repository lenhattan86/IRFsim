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
import cern.jet.math.tdouble.DoubleFunctions;

/**
 * This function represents the logarithm of a posynomial after a change of variables x->y=log(x).
 * <br>It represents a posynomial:
 * 
 * <br>(1) f(x) = Sum[k=1, K](c[k]*x[1]^a[1,k]*x[2]^a[2,k]*****x[n]^a[n,k]), c[k]>0
 * 
 * <br>in the form of:
 * 
 * <br>(2) g(y) = log(Sum[k=1, K](e^(aT[k]*y+b[k])))
 * 
 * <br>where a[k]=(a[1,k],..,a[n,k]) and b[k]=log(c[k])
 * 
 * <br>It is useful in geometric programming.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 4.5.3"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class LogTransformedPosynomial implements ConvexMultivariateRealFunction {
	
	private DoubleMatrix2D A = null;
	private DoubleMatrix2D AT = null;
	private DoubleMatrix1D b = null;
	private int dim = -1;
	private DenseDoubleAlgebra ALG = DenseDoubleAlgebra.DEFAULT;
	
	/**
	 * A representation of a posynomial (1) in the form (2).
	 * @param akArray the matrix (a[1,k],..,a[n,k]) in expression (2)
	 * @param bkArray the vector b[k] in expression (2)
	 */
	public LogTransformedPosynomial(double[][] akArray, double[] bkArray){
		this.A = DoubleFactory2D.dense.make(akArray);
		this.b = DoubleFactory1D.dense.make(bkArray);
		if(A.rows() != b.size()){
			throw new IllegalArgumentException("Impossible to create the function");
		}
		this.AT = ALG.transpose(A);
		this.dim = A.columns();
	}

	public double value(DoubleMatrix1D X) {
		DoubleMatrix1D g = ALG.mult(A, X).assign(b, DoubleFunctions.plus).assign(DoubleFunctions.exp);
		return Math.log(g.zSum());
	}

	public DoubleMatrix1D gradient(DoubleMatrix1D X) {
		DoubleMatrix1D g = ALG.mult(A, X).assign(b, DoubleFunctions.plus).assign(DoubleFunctions.exp);
		double den = g.zSum();
		DoubleMatrix1D R = DoubleFactory1D.dense.make(dim);
		for(int i=0; i<dim; i++){
			double d = 0d;
			for(int k=0; k<A.rows(); k++){
				d += g.get(k) * A.get(k, i);
			}
			R.setQuick(i, d/den);
		}
	    return R;
	}

	public DoubleMatrix2D hessian(DoubleMatrix1D X) {
		DoubleMatrix1D g = ALG.mult(A, X).assign(b, DoubleFunctions.plus).assign(DoubleFunctions.exp);
		double den = g.zSum();
		DoubleMatrix1D r = DoubleFactory1D.dense.make(dim);
		for(int i=0; i<dim; i++){
			double d = 0d;
			for(int k=0; k<A.rows(); k++){
				d += g.get(k) * A.get(k, i);
			}
			r.set(i, d);
		}
		
		DoubleMatrix2D ret = DoubleFactory2D.dense.make(dim, dim);
		ret.assign(ALG.multOuter(r, r, null).assign(DoubleFunctions.mult(-1d/Math.pow(den, 2))), DoubleFunctions.plus);
		for(int k=0; k<A.rows(); k++){
			ret.assign(ALG.multOuter(A.viewRow(k), A.viewRow(k), null).assign(DoubleFunctions.mult(g.get(k))).assign(DoubleFunctions.mult(1d/den)), DoubleFunctions.plus);
		}
		return ret;
	}

	public int getDim() {
		return this.dim;
	}

}
