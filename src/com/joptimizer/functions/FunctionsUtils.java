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

import java.util.Arrays;

import com.joptimizer.util.ColtUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class FunctionsUtils {

	/**
	 * Placeholder for 2D-arrays of zeroes.
	 */
	//public static final DoubleMatrix2D ZEROES_2D_ARRAY_PLACEHOLDER = new double[0][0];
	
	/**
	 * Placeholder for a matrix of zeroes.
	 */
	public static final DoubleMatrix2D ZEROES_MATRIX_PLACEHOLDER = DoubleFactory2D.dense.make(0, 0);
	private static DoubleFactory1D F1 = DoubleFactory1D.dense;
	private static DoubleFactory2D F2 = DoubleFactory2D.dense;
	
	public static ConvexMultivariateRealFunction createCircle(final int dim,
			final double radius) {
		double[] center = new double[dim];
		return createCircle(dim, radius, center);
	}

	public static ConvexMultivariateRealFunction createCircle(final int dim,
			final double radius, final double[] center) {

		final DoubleMatrix1D C = F1.make(center);
		return new ConvexMultivariateRealFunction() {

			/**
			 * Sum[ (x[i]-center[i])^2 ] - radius^2.
			 */
			public double value(DoubleMatrix1D X) {
				DoubleMatrix1D D = ColtUtils.sum(X, C, -1);
				double d = D.zDotProduct(D) - Math.pow(radius, 2);
				return d;
			}

			public DoubleMatrix1D gradient(DoubleMatrix1D X) {
				DoubleMatrix1D D = ColtUtils.sum(X, C, -1);
				return D.assign(DoubleFunctions.mult(2));
			}

			public DoubleMatrix2D hessian(DoubleMatrix1D X) {
				double[] d = new double[dim];
				Arrays.fill(d, 2);
				return F2.diagonal(F1.make(d));
			}

			public int getDim() {
				return dim;
			}
		};
	}
}
