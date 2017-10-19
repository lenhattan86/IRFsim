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
package com.joptimizer.algebra;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * Calculate the row and column scaling matrices R and T relative to a given
 * matrix A (scaled A = R.A.T). 
 * They may be used, for instance, to scale the matrix prior to solving a
 * corresponding set of linear equations.
 *  
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public interface MatrixRescaler {

	/**
	 * Calculates the R and T scaling factors (matrices) for a generic matrix A so that  A'(=scaled A) = R.A.T
	 * @return array with R,T 
	 */
	public DoubleMatrix1D[] getMatrixScalingFactors(DoubleMatrix2D A);

	/**
	 * Calculates the R and T scaling factors (matrices) for a symmetric matrix A so that  A'(=scaled A) = R.A.T
	 * @return array with R,T 
	 */
	public DoubleMatrix1D getMatrixScalingFactorsSymm(DoubleMatrix2D A);

	/**
	 * Check if the scaling algorithm returned proper results.
	 * @param AOriginal the ORIGINAL (before scaling) matrix
	 * @param U the return of the scaling algorithm
	 * @param V the return of the scaling algorithm
	 * @param base
	 * @return
	 */
	public boolean checkScaling(final DoubleMatrix2D AOriginal, final DoubleMatrix1D U, final DoubleMatrix1D V);

}