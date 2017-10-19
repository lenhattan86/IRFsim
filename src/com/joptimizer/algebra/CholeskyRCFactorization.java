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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.util.Utils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DoubleProperty;

/**
 * Cholesky L.L[T] factorization for symmetric and positive matrix.
 * L is stores in a Row-Compressed way.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * @TODO: implement the solve method
 */
public class CholeskyRCFactorization {

	private int dim;
	private DoubleMatrix2D Q;
	double[] LData;
	private DoubleMatrix2D L;
	private DoubleMatrix2D LT;
	protected DoubleFactory2D F2 = DoubleFactory2D.dense;
	protected DoubleFactory1D F1 = DoubleFactory1D.dense;
	private static Log log = LogFactory.getLog(CholeskyRCFactorization.class.getName());
	
	public CholeskyRCFactorization(DoubleMatrix2D Q) throws JOptimizerException{
		this.dim = Q.rows();
		this.Q = Q;
	}
	
	public void factorize() throws JOptimizerException{
		factorize(false);
	}
	
	/**
	 * Cholesky factorization L of psd matrix, Q = L.LT
	 */
	public void factorize(boolean checkSymmetry) throws JOptimizerException{
		if (checkSymmetry && !DoubleProperty.TWELVE.isSymmetric(Q)) {
			throw new JOptimizerException("Matrix is not symmetric");
		}
		
		double threshold = Utils.getDoubleMachineEpsilon();
		this.LData = new double[dim * dim];

		for (int i = 0; i < dim; i++) {
			int iShift = i*dim;
			for (int j = 0; j < i+1; j++) {
				int jShift = j*dim;
				double sum = 0.0;
				for (int k = 0; k < j; k++) {
					sum += LData[jShift + k] * LData[iShift + k];
				}
				if (i == j){
					double d = Q.getQuick(i, i) - sum;
					if(!(d > threshold)){
						throw new JOptimizerException("not positive definite matrix");
					}
					LData[iShift + i] = Math.sqrt(d);
				} else {
					LData[iShift + j] = 1.0 / LData[jShift + j] * (Q.getQuick(i, j) - sum);
				}
			}
		}
	}
	
	/**
	 * 
	 * @deprecated use the solve() methods instead
	 */
	@Deprecated
	public DoubleMatrix2D getInverse() {

		//QInv = LTInv * LInv, but for symmetry (QInv=QInvT)
		//QInv = LInvT * LTInvT = LInvT * LInv, so
		//LInvT = LTInv, and we calculate
		//QInv = LInvT * LInv

		// LTInv calculation (it will be x)
		// NB: LInv is lower-triangular
		double[] LInv = new double[dim*dim];
//		for(int i=0; i<dim; i++){
//			//diagonal filling
//			LInv[i*dim + i] = 1.;
//		}
		for (int j = 0; j < dim; j++) {
			int jShift = j*dim;
			LInv[jShift + j] = 1.;//diagonal filling
			final double lTJJ = LData[jShift + j];
			for (int k = 0; k < j+1; ++k) {
				LInv[jShift + k] /= lTJJ;
			}
			for (int i = j + 1; i < dim; i++) {
				int iShift = i*dim;
				final double lTJI = LData[iShift + j];
				if(Double.compare(lTJI, 0.)!=0){
					for (int k = 0; k < j+1; ++k) {
						LInv[iShift + k] -= LInv[jShift + k] * lTJI;
					}
				}
			}
		}
		
		//log.debug("LInv: " + ArrayUtils.toString(LInv));

		// QInv
		// NB: LInvT is upper-triangular, so LInvT[i][j]=0 if i>j
		final DoubleMatrix2D QInvData = F2.make(dim, dim);
		for (int row = 0; row < dim; row++) {
			//final double[] LInvTDataRow = LInvTData[row];
			final DoubleMatrix1D QInvDataRow = QInvData.viewRow(row);
			for (int col = row; col < dim; col++) {// symmetry of QInv
				//final double[] LInvTDataCol = LInvTData[col];
				double sum = 0;
				for (int i = col; i < dim; i++) {// upper triangular
					sum += LInv[i*dim + row] * LInv[i*dim + col];
				}
				QInvDataRow.setQuick(col, sum);
				QInvData.setQuick(col, row, sum);// symmetry of QInv
			}
		}

		return QInvData;
	}
	
	/**
	 * @TODO: implement this method
	 */
	public DoubleMatrix1D solve(DoubleMatrix1D b) {
		if (b.size() != dim) {
			log.error("wrong dimension of vector b: expected " + dim +", actual " + b.size());
			throw new RuntimeException("wrong dimension of vector b: expected " + dim +", actual " + b.size());
		}
		
		throw new RuntimeException("not yet implemented");
	}
	  
	/**
	 * @TODO: implement this method
	 */
	public DoubleMatrix2D solve(DoubleMatrix2D B) {
		  if (B.rows() != dim) {
				log.error("wrong dimension of vector b: expected " + dim +", actual " + B.rows());
				throw new RuntimeException("wrong dimension of vector b: expected " + dim +", actual " + B.rows());
			}
		  throw new RuntimeException("not yet implemented");
	}
	
	public DoubleMatrix2D getL() {
		if (L == null) {
			double[][] myL = new double[dim][dim];
			for (int i = 0; i < dim; i++) {
				int iShift = i * dim;
				double[] myLI = myL[i];
				for (int j = 0; j <= i; j++) {
					myLI[j] = LData[iShift + j];
				}
			}
			this.L = F2.make(myL);
		}

		return L;
	}
	
	public DoubleMatrix2D getLT() {
		if(this.LT == null){
			this.LT = getL().viewDice();
		}
		return this.LT;
	}
	
}
