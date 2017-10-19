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
package com.joptimizer.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.linear.RealMatrix;

import com.joptimizer.exception.JOptimizerException;

import cern.colt.function.tdouble.IntIntDoubleFunction;
import cern.colt.function.tint.IntIntIntFunction;
import cern.colt.matrix.AbstractMatrix1D;
import cern.colt.matrix.AbstractMatrix2D;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;
import cern.colt.matrix.tint.impl.SparseIntMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.csparsej.tdouble.Dcs_common;

/**
 * Support class for recurrent algebra with Colt.
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class ColtUtils {
	
	public static Log log = LogFactory.getLog(ColtUtils.class);
	
	/**
	 * Matrix-vector multiplication with diagonal matrix.
	 * @param diagonalM diagonal matrix M
	 * @param vector
	 * @return M.x
	 */
//	public static final DoubleMatrix1D diagonalMatrixMult(DoubleMatrix2D diagonalM, DoubleMatrix1D vector){
//		int n = diagonalM.rows();
//		DoubleMatrix1D ret = DoubleFactory1D.dense.make(n);
//		for(int i=0; i<n; i++){
//			ret.setQuick(i, diagonalM.getQuick(i, i) * vector.getQuick(i));
//		}
//		return ret;
//	}
	
	public static final DoubleMatrix1D createDoubleMatrix1D(int size, double value){
		double[] values = new double[size];
		Arrays.fill(values, value);
		return DoubleFactory1D.dense.make(values);
	}
	
	public static DoubleMatrix1D randomValuesVector(int dim, double min, double max) {
		return randomValuesVector(dim, min, max, null);
	}

	public static DoubleMatrix1D randomValuesVector(int dim, double min, double max, Long seed) {
		Random random = (seed != null) ? new Random(seed) : new Random();

		double[] v = new double[dim];
		for (int i = 0; i < dim; i++) {
			v[i] = min + random.nextDouble() * (max - min);
		}
		return DoubleFactory1D.dense.make(v);
	}
	
	public static DoubleMatrix2D randomValuesMatrix(int rows, int cols, double min, double max) {
		return randomValuesMatrix(rows, cols, min, max, null);
	}

	public static DoubleMatrix2D randomValuesMatrix(int rows, int cols, double min, double max, Long seed) {
		Random random = (seed != null) ? new Random(seed) : new Random();

		double[][] matrix = new double[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				matrix[i][j] = min + random.nextDouble() * (max - min);
			}
		}
		return DoubleFactory2D.dense.make(matrix);
	}
	
	public static DoubleMatrix2D randomValuesSparseMatrix(int rows, int cols, double min, double max, double sparsityIndex, Long seed) {
		Random random = (seed != null) ? new Random(seed) : new Random();
		double minThreshold = min + sparsityIndex * (max-min);
		
		double[][] matrix = new double[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				double d = min + random.nextDouble() * (max - min);
				if(d > minThreshold){
					matrix[i][j] = d;
				}
			}
		}
		return DoubleFactory2D.sparse.make(matrix);
	}
	
	/**
	 * @TODO: check this!!!
	 * @see "http://mathworld.wolfram.com/PositiveDefiniteMatrix.html"
	 */
	public static DoubleMatrix2D randomValuesPositiveMatrix(int rows, int cols, double min, double max, Long seed) {
		DoubleMatrix2D Q = ColtUtils.randomValuesMatrix(rows, cols, min, max, seed);
		DoubleMatrix2D P = DenseDoubleAlgebra.DEFAULT.mult(Q, DenseDoubleAlgebra.DEFAULT.transpose(Q.copy()));
		return DenseDoubleAlgebra.DEFAULT.mult(P, P);
	}
	
	/**
	 * Calculate the scaled residual 
	 * <br> ||Ax-b||_oo/( ||A||_oo . ||x||_oo + ||b||_oo ), with
	 * <br> ||x||_oo = max(||x[i]||)
	 */
	public static double calculateScaledResidual(DoubleMatrix2D A, DoubleMatrix2D X, DoubleMatrix2D B) {
		double residual = -Double.MAX_VALUE;
		double niX = DenseDoubleAlgebra.DEFAULT.normInfinity(X);
		double niB = DenseDoubleAlgebra.DEFAULT.normInfinity(B);
		if (Utils.isZero(niX) && Utils.isZero(niB)) {
			return 0;
		} else {
			double num = DenseDoubleAlgebra.DEFAULT.normInfinity(DenseDoubleAlgebra.DEFAULT.mult(A, X).assign(B, DoubleFunctions.minus));
			double den = DenseDoubleAlgebra.DEFAULT.normInfinity(A) * niX + niB;
			residual = num / den;
			// log.debug("scaled residual: " + residual);

			return residual;
		}
	}
	
	/**
	 * Calculate the scaled residual
	 * <br> ||Ax-b||_oo/( ||A||_oo . ||x||_oo + ||b||_oo ), with
	 * <br> ||x||_oo = max(||x[i]||)
	 */
	public static double calculateScaledResidual(DoubleMatrix2D A, DoubleMatrix1D x, DoubleMatrix1D b){
		double residual = -Double.MAX_VALUE;
		double nix = DenseDoubleAlgebra.DEFAULT.normInfinity(x);
		double nib = DenseDoubleAlgebra.DEFAULT.normInfinity(b);
		if (Utils.isZero(nix) && Utils.isZero(nib)) {
			return 0;
		}else{
			double num = DenseDoubleAlgebra.DEFAULT.normInfinity(ColtUtils.zMult(A, x, b, -1));
			double den = DenseDoubleAlgebra.DEFAULT.normInfinity(A) * nix + nib;
			residual =  num / den;
			//log.debug("scaled residual: " + residual);
			
			return residual;
		}
	}
	
	/**
	   * Get the index of the maximum entry.
	   */
	  public static int getMaxIndex(DoubleMatrix1D v){
	  	int maxIndex = -1;
	  	double maxValue = -Double.MAX_VALUE;
	  	for(int i=0; i<v.size(); i++){
	  		if(v.getQuick(i)>maxValue){
	  			maxIndex = i;
	  			maxValue = v.getQuick(i); 
	  		}
	  	}
	  	return maxIndex; 
	  }
	  
	  /**
	   * Get the index of the minimum entry.
	   */
	  public static int getMinIndex(DoubleMatrix1D v){
	  	int minIndex = -1;
	  	double minValue = Double.MAX_VALUE;
	  	for(int i=0; i<v.size(); i++){
	  		if(v.getQuick(i)<minValue){
	  			minIndex = i;
	  			minValue = v.getQuick(i); 
	  		}
	  	}
	  	return minIndex; 
	  }
	
	/**
	 * Matrix-vector multiplication with diagonal matrix.
	 * @param diagonalM diagonal matrix M, in the form of a vector of its diagonal elements
	 * @param vector
	 * @return M.x
	 */
	public static final DoubleMatrix1D diagonalMatrixMult(DoubleMatrix1D diagonalM, DoubleMatrix1D vector){
		int n = (int) diagonalM.size();
		DoubleMatrix1D ret = DoubleFactory1D.dense.make(n);
		for(int i=0; i<n; i++){
			ret.setQuick(i, diagonalM.getQuick(i) * vector.getQuick(i));
		}
		return ret;
	}
	
	/**
	 * Return diagonalU.A with diagonalU diagonal.
	 * @param diagonal matrix U, in the form of a vector of its diagonal elements
	 * @return U.A
	 */
	public static final DoubleMatrix2D diagonalMatrixMult(final DoubleMatrix1D diagonalU, DoubleMatrix2D A){
		int r = (int) diagonalU.size();
		int c = A.columns();
		final DoubleMatrix2D ret;
		if (A instanceof SparseDoubleMatrix2D) {
			ret = DoubleFactory2D.sparse.make(r, c);
			A.forEachNonZero(new IntIntDoubleFunction() {
				public double apply(int i, int j, double aij) {
					ret.setQuick(i, j, aij * diagonalU.getQuick(i));
					return aij;
				}
			});
		} else {
			ret = DoubleFactory2D.dense.make(r, c);
			for (int i = 0; i < r; i++) {
				for (int j = 0; j < c; j++) {
					ret.setQuick(i, j, A.getQuick(i, j) * diagonalU.getQuick(i));
				}
			}
		}

		return ret;
	}
	
	/**
	 * Return A.diagonalU with diagonalU diagonal.
	 * @param diagonal matrix U, in the form of a vector of its diagonal elements
	 * @return U.A
	 */
	public static final DoubleMatrix2D diagonalMatrixMult(DoubleMatrix2D A, final DoubleMatrix1D diagonalU){
		int r = (int) diagonalU.size();
		int c = A.columns();
		final DoubleMatrix2D ret;
		if (A instanceof SparseDoubleMatrix2D) {
			ret = DoubleFactory2D.sparse.make(r, c);
			A.forEachNonZero(new IntIntDoubleFunction() {
				public double apply(int i, int j, double aij) {
					ret.setQuick(i, j, aij * diagonalU.getQuick(j));
					return aij;
				}
			});
		} else {
			ret = DoubleFactory2D.dense.make(r, c);
			for (int i = 0; i < r; i++) {
				for (int j = 0; j < c; j++) {
					ret.setQuick(i, j, A.getQuick(i, j) * diagonalU.getQuick(j));
				}
			}
		}

		return ret;
	}
	
	/**
	 * Return diagonalU.A.diagonalV with diagonalU and diagonalV diagonal.
	 * @param diagonalU diagonal matrix U, in the form of a vector of its diagonal elements
	 * @param diagonalV diagonal matrix V, in the form of a vector of its diagonal elements
	 * @return U.A.V
	 */
	public static final DoubleMatrix2D diagonalMatrixMult(final DoubleMatrix1D diagonalU, DoubleMatrix2D A, final DoubleMatrix1D diagonalV){
		int r = A.rows();
		int c = A.columns();
		final DoubleMatrix2D ret;
		if (A instanceof SparseDoubleMatrix2D) {
			ret = DoubleFactory2D.sparse.make(r, c);
			A.forEachNonZero(new IntIntDoubleFunction() {
				public double apply(int i, int j, double aij) {
					ret.setQuick(i, j, aij * diagonalU.getQuick(i) * diagonalV.getQuick(j));
					return aij;
				}
			});
		} else {
			ret = DoubleFactory2D.dense.make(r, c);
			for (int i = 0; i < r; i++) {
				for (int j = 0; j < c; j++) {
					ret.setQuick(i, j, A.getQuick(i, j) * diagonalU.getQuick(i)	* diagonalV.getQuick(j));
				}
			}
		}

		return ret;
	}
	
	/**
	 * Return the sub-diagonal result of the multiplication.
	 * If A is sparse, returns a sparse matrix (even if, generally speaking, 
	 * the multiplication of two sparse matrices is not sparse) because the result
	 * is at least 50% (aside the diagonal elements) sparse.  
	 */
	public static DoubleMatrix2D subdiagonalMultiply(final DoubleMatrix2D A, final DoubleMatrix2D B){
		final int r = A.rows();
		final int rc = A.columns();
		final int c = B.columns();
		
		if(r != c){
			throw new IllegalArgumentException("The result must be square");
		}
		
		boolean useSparsity = A instanceof SparseDoubleMatrix2D;
		DoubleFactory2D F2 = (useSparsity)? DoubleFactory2D.sparse : DoubleFactory2D.dense; 
		final DoubleMatrix2D ret = F2.make(r, c);
		
		if(useSparsity){
			//final int[] currentRowIndexHolder = new int[] { -1 };
			
			IntIntDoubleFunction myFunct = new IntIntDoubleFunction() {
				public double apply(int t, int s, double pts) {
					//int i = currentRowIndexHolder[0];
					int i = t;
					//log.debug("-->i:" + i);
					for (int j = 0; j < i + 1; j++) {
						//log.debug("->j:" + j);
						ret.setQuick(i, j, ret.getQuick(i, j) + pts * B.getQuick(s, j));
					}
					return pts;
				}
			};
			
			//view A row by row
//			for (int currentRow = 0; currentRow < r; currentRow++) {
//				DoubleMatrix2D P = A.viewPart(currentRow, 0, 1, rc);
//				currentRowIndexHolder[0] = currentRow;
//				P.forEachNonZero(myFunct);
//			}
			A.forEachNonZero(myFunct);
		}else{
			for (int i = 0; i < r; i++) {
				for (int j = 0; j < i + 1; j++) {
					double s = 0;
					for (int k = 0; k < rc; k++) {
						s += A.getQuick(i, k) * B.getQuick(k, j);
					}
					ret.setQuick(i, j, s);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Returns v = beta * A.b.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix1D zMult(final DoubleMatrix2D A, final DoubleMatrix1D b, final double beta){
		if(A.columns() != b.size()){
			throw new IllegalArgumentException("wrong matrices dimensions");
		}
		final DoubleMatrix1D ret = DoubleFactory1D.dense.make(A.rows());
		
		if(A instanceof SparseDoubleMatrix2D){
	    //if(1==2){	
			//sparse matrix
			A.forEachNonZero(new IntIntDoubleFunction() {
				public double apply(int i, int j, double Aij) {
					double vi = 0;
					vi += Aij * b.getQuick(j);
					ret.setQuick(i, ret.getQuick(i) + beta * vi);
					return Aij;
				}
			});
		}else{
			//dense matrix
			for(int i=0; i<A.rows(); i++){
				//DoubleMatrix1D AI = A.viewRow(i);
				double vi = 0;
				for(int j=0; j<A.columns(); j++){
					vi += A.getQuick(i, j) * b.getQuick(j);
				}
				ret.setQuick(i, beta * vi);
			}
		}
		
		return ret;
	}
	
	/**
	 * Returns v = A.a + beta*b.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix1D zMult(final DoubleMatrix2D A, final DoubleMatrix1D a, final DoubleMatrix1D b, final double beta){
		if(A.columns()!=a.size() || A.rows()!=b.size()){
			throw new IllegalArgumentException("wrong matrices dimensions");
		}
		final DoubleMatrix1D ret = DoubleFactory1D.dense.make(A.rows());
		
		if(A instanceof SparseDoubleMatrix2D){
		//if(1==2){	
			//sparse matrix
			A.forEachNonZero(new IntIntDoubleFunction() {
				public double apply(int i, int j, double Aij) {
					ret.setQuick(i, ret.getQuick(i) + Aij * a.getQuick(j));
					return Aij;
				}
			});
			for(int i=0; i<ret.size(); i++){
				ret.setQuick(i, ret.getQuick(i) + beta * b.getQuick(i));
			}
		}else{
		  //dense matrix
			for(int i=0; i<A.rows(); i++){
				//DoubleMatrix1D AI = A.viewRow(i);
				double vi = beta * b.getQuick(i);
				for(int j=0; j<A.columns(); j++){
					vi += A.getQuick(i, j) * a.getQuick(j);
				}
				ret.setQuick(i, vi);
			}
		}
		
		return ret;
	}
	
	/**
	 * Returns v = A[T].a + beta*b.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix1D zMultTranspose(final DoubleMatrix2D A, final DoubleMatrix1D a, final DoubleMatrix1D b, final double beta){
		if(A.rows()!=a.size() || (b!=null && A.columns()!=b.size())){
			throw new IllegalArgumentException("wrong matrices dimensions");
		}
		final DoubleMatrix1D ret = DoubleFactory1D.dense.make(A.columns());
		
		boolean isBetaNot0 = beta*beta > 0; 
		
		if(A instanceof SparseDoubleMatrix2D){
		//if(1==2){	
			A.forEachNonZero(new IntIntDoubleFunction() {
				public double apply(int i, int j, double Aij) {
					//log.debug(i + "," + j + ": " + Aij + ", "+ret.getQuick(j)+", "+a.getQuick(i));
					ret.setQuick(j, ret.getQuick(j) + Aij * a.getQuick(i));
					return Aij;
				}
			});
			if(isBetaNot0){
				for(int i=0; i<ret.size(); i++){
					ret.setQuick(i, ret.getQuick(i) + beta * b.getQuick(i));
				}
			}
		}else{
			if(isBetaNot0){
				for(int i=0; i<A.columns(); i++){
					double vi = beta * b.getQuick(i);
					for(int j=0; j<A.rows(); j++){
						vi += A.getQuick(j, i) * a.getQuick(j);
					}
					ret.setQuick(i, vi);
				}
			}else{
				for(int i=0; i<A.columns(); i++){
					double vi = 0d;
					for(int j=0; j<A.rows(); j++){
						vi += A.getQuick(j, i) * a.getQuick(j);
					}
					ret.setQuick(i, vi);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Returns C = A + B.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix2D sum(DoubleMatrix2D A, DoubleMatrix2D B){
		if(A.rows()!=B.rows() || A.columns()!=B.columns()){
			throw new IllegalArgumentException("wrong matrices dimensions");
		}
		DoubleMatrix2D ret = DoubleFactory2D.dense.make(A.rows(), A.columns());
		for(int i=0; i<ret.rows(); i++){
			//DoubleMatrix1D AI = A.viewRow(i);
			//DoubleMatrix1D BI = B.viewRow(i);
			//DoubleMatrix1D retI = ret.viewRow(i);
			for(int j=0; j<ret.columns(); j++){
				ret.setQuick(i, j, A.getQuick(i, j) + B.getQuick(i, j));
			}
		}
		
		return ret;
	}
	
	/**
	 * Returns C = A + beta*B.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix2D sum(DoubleMatrix2D A, DoubleMatrix2D B, double beta){
		if(A.rows()!=B.rows() || A.columns()!=B.columns()){
			throw new IllegalArgumentException("wrong matrices dimensions");
		}
		DoubleMatrix2D ret = DoubleFactory2D.dense.make(A.rows(), A.columns());
		for(int i=0; i<ret.rows(); i++){
			//DoubleMatrix1D AI = A.viewRow(i);
			//DoubleMatrix1D BI = B.viewRow(i);
			//DoubleMatrix1D retI = ret.viewRow(i);
			for(int j=0; j<ret.columns(); j++){
				ret.setQuick(i, j, A.getQuick(i, j) + beta*B.getQuick(i, j));
			}
		}
		
		return ret;
	}
	
	/**
	 * Returns A = A + beta*B.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final void add(DoubleMatrix2D A, DoubleMatrix2D B, double beta) {
		if (A.rows() != B.rows() || A.columns() != B.columns()) {
			throw new IllegalArgumentException("wrong matrices dimensions");
		}
		for (int i = 0; i < A.rows(); i++) {
			for (int j = 0; j < A.columns(); j++) {
				A.setQuick(i, j, A.getQuick(i, j) + beta * B.getQuick(i, j));
			}
		}
	}
	
	/**
	 * Returns v = v1 + v2.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix1D sum(DoubleMatrix1D v1, DoubleMatrix1D v2){
		if(v1.size()!=v2.size()){
			throw new IllegalArgumentException("wrong vectors dimensions");
		}
		DoubleMatrix1D ret = DoubleFactory1D.dense.make((int) v1.size());
		for(int i=0; i<ret.size(); i++){
			ret.setQuick(i, v1.getQuick(i) + v2.getQuick(i));
		}
		
		return ret;
	}
	
	/**
	 * Returns v = Sum[v1 + v2 + ...].
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix1D sum(DoubleMatrix1D[] vArray){
		int size = (int) vArray[0].size();
		for (int v = 0; v < vArray.length; v++) {
			if (vArray[v].size() != size) {
				throw new IllegalArgumentException("wrong vectors dimensions");
			}
		}
		
		DoubleMatrix1D ret = DoubleFactory1D.dense.make(size);
		for (int i = 0; i < ret.size(); i++) {
			double d = 0;
			for (int v = 0; v < vArray.length; i++) {
				d += vArray[v].getQuick(i);
			}
			ret.setQuick(i, d);
		}
		
		return ret;
	}
	
	/**
	 * Returns v = v1 + c*v2.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix1D sum(DoubleMatrix1D v1, DoubleMatrix1D v2, double c){
		if(v1.size()!=v2.size()){
			throw new IllegalArgumentException("wrong vectors dimensions");
		}
		DoubleMatrix1D ret = DoubleFactory1D.dense.make((int) v1.size());
		for(int i=0; i<ret.size(); i++){
			ret.setQuick(i, v1.getQuick(i) + c*v2.getQuick(i));
		}
		
		return ret;
	}
	
	/**
	 * Returns v1 = v1 + c*v2.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final void add(DoubleMatrix1D v1, DoubleMatrix1D v2, double c) {
		if (v1.size() != v2.size()) {
			throw new IllegalArgumentException("wrong vectors dimensions");
		}
		for (int i = 0; i < v1.size(); i++) {
			v1.setQuick(i, v1.getQuick(i) + c * v2.getQuick(i));
		}
	}
	
	/**
	 * M = M + coeff * vi.outer(v1)
	 */
	public static final void addOuterProduct(DoubleMatrix2D M, DoubleMatrix1D v1, double coeff) throws IllegalArgumentException {
		long n = v1.size();
		double v1i = 0.;
		double v = 0.;
		double coeffPd1i = 0.;
		int i = 0;
		int j = 0;

		if (!Utils.isZero(coeff)) {
			for (i = 0; i < n; i++) {
				v1i = v1.getQuick(i);
				if (!Utils.isZero(v1i)) {
					coeffPd1i = coeff * v1i;
					M.setQuick(i, i, M.getQuick(i, i) + coeffPd1i * v1i);
					for (j = i + 1; j < n; j++) {
						v = coeffPd1i * v1.getQuick(j);
						M.setQuick(i, j, M.getQuick(i, j) + v);
						M.setQuick(j, i, M.getQuick(j, i) + v);
					}
				}
			}
		}
	}
	
	/**
	 * Returns the product element by element.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix1D multElements(DoubleMatrix1D v1, DoubleMatrix1D v2){
		if(v1.size()!=v2.size()){
			throw new IllegalArgumentException("wrong vectors dimensions");
		}
		DoubleMatrix1D ret = DoubleFactory1D.dense.make((int) v1.size());
		for(int i=0; i<ret.size(); i++){
			ret.setQuick(i, v1.getQuick(i) * v2.getQuick(i));
		}
		
		return ret;
	}
	
	/**
	 * Returns v = c * v1.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix1D scalarMult(DoubleMatrix1D v1, double c){
		DoubleMatrix1D ret = DoubleFactory1D.dense.make((int) v1.size());
		for(int i=0; i<ret.size(); i++){
			ret.setQuick(i, c * v1.getQuick(i));
		}
		
		return ret;
	}
	
	/**
	 * Returns V = c * V1.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix2D scalarMult(SparseDoubleMatrix2D V1, final double c){
		
		final DoubleMatrix2D ret = DoubleFactory2D.sparse.make(V1.rows(), V1.columns());
		V1.forEachNonZero(new IntIntDoubleFunction() {
			public double apply(int i, int j, double v1ij) {
				ret.setQuick(i, j, c * v1ij);
				return v1ij;
			}
		});
		
		return ret;
	}
	
	/**
	 * Returns v = c * v1.
	 * Useful in avoiding the need of the copy() in the colt api.
	 */
	public static final DoubleMatrix2D scalarMult(DoubleMatrix2D V1, final double c){
		if(V1 instanceof SparseDoubleMatrix2D){
			final DoubleMatrix2D ret = DoubleFactory2D.sparse.make(V1.rows(), V1.columns());
			V1.forEachNonZero(new IntIntDoubleFunction() {
				public double apply(int i, int j, double v1ij) {
					ret.setQuick(i, j, c * v1ij);
					return v1ij;
				}
			});
			return ret;
		}else{
			DoubleMatrix2D ret = DoubleFactory2D.dense.make(V1.rows(), V1.columns());
			for(int i=0; i<V1.rows(); i++){
				for(int j=0; j<V1.columns(); j++){
					ret.setQuick(j, j, c * V1.getQuick(i, j));
				}
			}
			return ret;
		}
	}
	
	public static final Dcs_common.Dcs matrixToDcs(SparseDoubleMatrix2D A) {
		
		//m (number of rows):
		int m = A.rows();
		//n (number of columns) 
		int n = A.columns();
		//nz (# of entries in triplet matrix, -1 for compressed-col) 
		int nz = -1;
		//nxmax (maximum number of entries)
		int nzmax = m*n;
		//p (column pointers (size n+1) or col indices (size nzmax))
		final int[] p = new int[n+1];
		//i (row indices, size nzmax)
		final int[] i = new int[nzmax];
		//x (numerical values, size nzmax)
		final double[] x = new double[nzmax];
		
		final int[] currentColumnIndexHolder = new int[]{-1};
		
		IntIntDoubleFunction myFunct = new IntIntDoubleFunction() {
			int nzCounter = 0;
			public double apply(int r, int c, double prc) {
				//log.debug("r:" + r + ", c:" + currentColumnIndexHolder[0] + ": " + prc);
				
				i[nzCounter] = r;
				x[nzCounter] = prc;
				nzCounter++;
				
				p[currentColumnIndexHolder[0]+1] = p[currentColumnIndexHolder[0]+1] + 1;
				//log.debug("p: " + ArrayUtils.toString(p));
				
				return prc;
			}
		};
		
		//view A column by column
		for (int c = 0; c < n; c++) {
			//log.debug("column:" + c);
			DoubleMatrix2D P = A.viewPart(0, c, m, 1);
			currentColumnIndexHolder[0] = c;
			p[currentColumnIndexHolder[0]+1] = p[currentColumnIndexHolder[0]];
			P.forEachNonZero(myFunct);
		}
		
		Dcs_common.Dcs dcs = new Dcs_common.Dcs();
		dcs.m = m;
		dcs.n = n;
		dcs.nz = nz;
		dcs.nzmax = nzmax;
		dcs.p = p;
		dcs.i = i;
		dcs.x = x;
		//log.debug("dcs.p: " + ArrayUtils.toString(dcs.p));
		return dcs;
	}
	
	public static final SparseDoubleMatrix2D dcsToMatrix(Dcs_common.Dcs dcs) {
		SparseDoubleMatrix2D A = new SparseDoubleMatrix2D(dcs.m, dcs.n);
		final int[] rowIndexes = dcs.i;
        final int[] columnPointers = dcs.p;
        final double values[] = dcs.x;
        //for example
        //rowIndexes      2, 0, 2, 3, 1, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        //columnPointers  0,    2, 3,    5,    7
        //values          2.0, 1.0, 3.0, 4.0, 2.0, 4.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        int cnt =0;
        for (int j = 0; j < dcs.n; j++) {
        	int colStartIndex = columnPointers[j];//example 5
        	int colEndIndex = columnPointers[j+1];//example 7
			for (int pointer = colStartIndex; pointer < colEndIndex; pointer++) {
				int i = rowIndexes[pointer];
				A.setQuick(i, j, values[cnt]);
				cnt++;
			}
        }
        
        //log.debug("A: " + ArrayUtils.toString(A.toArray()));
        return A;
	}
	
	/**
	 * Return a new array with all the occurences of oldValue replaced by newValue.
	 */
	public static final DoubleMatrix1D replaceValues(DoubleMatrix1D v, double oldValue,	double newValue) {
		if(v == null){
			return null;
		}
		DoubleFactory1D F1 = (v instanceof SparseDoubleMatrix1D)? DoubleFactory1D.sparse : DoubleFactory1D.dense;   
		DoubleMatrix1D ret = F1.make((int) v.size());
		for (int i = 0; i < v.size(); i++) {
			double vi = v.getQuick(i);
			if (Double.compare(oldValue, vi) != 0) {
				// no substitution
				ret.setQuick(i, vi);
			} else {
				ret.setQuick(i, newValue);
			}
		}
		return ret;
	}
	
	/**
	 * inversion of {{a, b},{c, d}}
	 * @param A
	 * @return det^-1 * {{d, -b},{-c, a}}
	 */
	public static final DoubleMatrix2D invert2x2Matrix(DoubleMatrix2D A) throws JOptimizerException{
		if(2!=A.rows() || A.rows() != A.columns()){
			throw new IllegalArgumentException("matrix is not 2x2");
		}
		//ad - bc
		double det = calculateDeterminant(A);
//		if(Math.abs(det) < 1.e-16){
//			throw new JOptimizerException("Matrix is singular");
//		}
		if(Math.abs(det) < Utils.getDoubleMachineEpsilon()){
			throw new JOptimizerException("Matrix is singular");
		}
		DoubleMatrix2D ret = new DenseDoubleMatrix2D(2, 2);
		ret.setQuick(0, 0,  A.getQuick(1, 1) / det);
		ret.setQuick(1, 1,  A.getQuick(0, 0) / det);
		ret.setQuick(0, 1, -A.getQuick(0, 1) / det);
		ret.setQuick(1, 0, -A.getQuick(1, 0) / det);
		return ret;
	}
	
	public static final DoubleMatrix2D symmPermutation(DoubleMatrix2D A, int from, int to){
		int n = A.rows();
		int[] rowIndexes = new int[n];
	    int[] columnIndexes = new int[n];
	    for(int i=0; i<n; i++){
	    	rowIndexes[i] = i;
	    	columnIndexes[i] = i;
	    }
	    rowIndexes[from] = to;
	    rowIndexes[to] = from;
	    columnIndexes[from] = to;
	    columnIndexes[to] = from;
	    return DenseDoubleAlgebra.DEFAULT.permute(A, rowIndexes, columnIndexes);
	}
	
	/**
	 * Returns a lower and an upper bound for the condition number
	 * <br>kp(A) = Norm[A, p] / Norm[A^-1, p]   
	 * <br>where
	 * <br>		Norm[A, p] = sup ( Norm[A.x, p]/Norm[x, p] , x !=0 )
	 * <br>for a matrix and
	 * <br>		Norm[x, 1]  := Sum[Math.abs(x[i]), i] 				
	 * <br>		Norm[x, 2]  := Math.sqrt(Sum[Math.pow(x[i], 2), i])
	 * <br>   Norm[x, 00] := Max[Math.abs(x[i]), i]
	 * <br>for a vector.
	 *  
	 * @param A matrix you want the condition number of
	 * @param p norm order (2 or Integer.MAX_VALUE)
	 * @return an array with the two bounds (lower and upper bound)
	 * 
	 * @see Ravindra S. Gajulapalli, Leon S. Lasdon "Scaling Sparse Matrices for Optimization Algorithms"
	 */
	public static final double[] getConditionNumberRange(RealMatrix A, int p) {
		double infLimit = Double.NEGATIVE_INFINITY;
		double supLimit = Double.POSITIVE_INFINITY;
		List<Double> columnNormsList = new ArrayList<Double>();
		switch (p) {
		case 2:
			for(int j=0; j<A.getColumnDimension(); j++){
				columnNormsList.add(A.getColumnVector(j).getL1Norm());
			}
			Collections.sort(columnNormsList);
			//kp >= Norm[Ai, p]/Norm[Aj, p], for each i, j = 0,1,...,n, Ak columns of A
			infLimit = columnNormsList.get(columnNormsList.size()-1) / columnNormsList.get(0);
			break;

		case Integer.MAX_VALUE:
			double normAInf = A.getNorm();
			for(int j=0; j<A.getColumnDimension(); j++){
				columnNormsList.add(A.getColumnVector(j).getLInfNorm());
			}
			Collections.sort(columnNormsList);
			//k1 >= Norm[A, +oo]/min{ Norm[Aj, +oo], for each j = 0,1,...,n }, Ak columns of A
			infLimit = normAInf / columnNormsList.get(0);
			break;

		default:
			throw new IllegalArgumentException("p must be 2 or Integer.MAX_VALUE");
		}
		return new double[]{infLimit, supLimit};
	}

	/**
	 * Given a symm matrix S that stores just its subdiagonal elements, 
	 * reconstructs the full symmetric matrix.
	 * @FIXME: evitare il doppio setQuick
	 */
	public static final DoubleMatrix2D fillSubdiagonalSymmetricMatrix(DoubleMatrix2D S){
		
		if(S.rows() != S.columns()){
			throw new IllegalArgumentException("Not square matrix");
		}
		
		boolean isSparse = S instanceof SparseDoubleMatrix2D;
		DoubleFactory2D F2D = (isSparse)? DoubleFactory2D.sparse: DoubleFactory2D.dense;
		final DoubleMatrix2D SFull = F2D.make(S.rows(), S.rows());
		
		if (isSparse) {
			S.forEachNonZero(new IntIntDoubleFunction() {
				public double apply(int i, int j, double hij) {
					SFull.setQuick(i, j, hij);
					SFull.setQuick(j, i, hij);
					return hij;
				}
			});
		} else {
			for (int i = 0; i < S.rows(); i++) {
				for (int j = 0; j < i + 1; j++) {
					double sij = S.getQuick(i, j);
					SFull.setQuick(i, j, sij);
					SFull.setQuick(j, i, sij);
				}
			}
		}
		
		return SFull;
	}
	
	/**
	 * Brute-force determinant calculation.
	 * @TODO: leverage sparsity of A.
	 */
	public static final double calculateDeterminant(DoubleMatrix2D A) {
		double det = 0;
		int dim = A.rows();
		if (dim == 1) {
			det = A.getQuick(0, 0);
		} else if (dim == 2) {
			det = A.getQuick(0, 0) * A.getQuick(1, 1) - A.getQuick(0, 1) * A.getQuick(1, 0);
		} else {
			DoubleMatrix1D A0 = A.viewRow(0);
			for (int k = 0; k < dim; k++) {
				double A0k = A0.getQuick(k);
				if (A0k < 0 || A0k > 0) {
					//take the minor excluding row 0 and column k
					int[] rowIndexes = new int[A.rows() - 1];
					for (int i = 1; i < A.rows(); i++) {
						rowIndexes[i - 1] = i;
					}
					int[] columnIndexes = new int[A.columns() - 1];
					int cnt = 0;
					for (int j = 0; j < A.columns(); j++) {
						if (j != k) {
							columnIndexes[cnt] = j;
							cnt++;
						}
					}
					DoubleMatrix2D Aminor = A.viewSelection(rowIndexes, columnIndexes);
					if (k % 2 == 0) {
						det += A0k * calculateDeterminant(Aminor);
					} else {
						det -= A0k * calculateDeterminant(Aminor);
					}
				}
			}
		}
		return det;
	}
	
	public static final double[] toArray(AbstractMatrix1D v) {
		java.lang.reflect.Method method;
		try {
			method = v.getClass().getMethod("toArray");
			return (double[]) method.invoke(v);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static final double[][] toArray(AbstractMatrix2D M) {
		java.lang.reflect.Method method;
		try {
			method = M.getClass().getMethod("toArray");
			return (double[][]) method.invoke(M);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static final String toString(AbstractMatrix1D v) {
		java.lang.reflect.Method method;
		try {
			method = v.getClass().getMethod("toArray");
			return ArrayUtils.toString(method.invoke(v));
		} catch (Exception e) {
			return "";
		}
	}
	
	public static final String toString(AbstractMatrix2D M) {
		java.lang.reflect.Method method;
		try {
			method = M.getClass().getMethod("toArray");
			return ArrayUtils.toString(method.invoke(M));
		} catch (Exception e) {
			return "";
		}
	}
	
	public static final DoubleMatrix1D toDoubleArray(int[] intArray) {
		DoubleMatrix1D ret = DoubleFactory1D.dense.make(intArray.length);
		for (int i = 0; i < ret.size(); i++) {
			ret.setQuick(i, intArray[i]);
		}
		return ret;
	}
	
	public static final DoubleMatrix1D toDoubleArray(IntMatrix1D intArray) {
		DoubleMatrix1D ret = DoubleFactory1D.dense.make((int) intArray.size());
		for (int i = 0; i < ret.size(); i++) {
			ret.setQuick(i, intArray.getQuick(i));
		}
		return ret;
	}
	
	public static final DoubleMatrix2D toDoubleMatrix(int[][] intMatrix) {
		DoubleMatrix2D ret = DoubleFactory2D.dense.make(intMatrix.length,	intMatrix[0].length);
		for (int i = 0; i < ret.rows(); i++) {
			for (int j = 0; j < ret.columns(); j++) {
				ret.setQuick(i, j, intMatrix[i][j]);
			}
		}
		return ret;
	}

	public static final DoubleMatrix2D toDoubleMatrix(IntMatrix2D intMatrix) {
		DoubleFactory2D F2 = DoubleFactory2D.dense;
		if (intMatrix instanceof SparseIntMatrix2D) {
			F2 = DoubleFactory2D.sparse;
		}
		final DoubleMatrix2D ret = F2.make(intMatrix.rows(), intMatrix.columns());
		intMatrix.forEachNonZero(new IntIntIntFunction() {
			public int apply(int i, int j, int mij) {
				ret.setQuick(i, j, mij);
				return 0;
			}
		});
		return ret;
	}
	
	public static final List<int[]> listConnectedComponents(IntMatrix2D M) {

		int rows = M.rows();
		int cols = M.columns();

		// build the undirected graph of the matrix
		final UndirectedGraph graph = new UndirectedGraph(cols, cols);
		final Integer[] sourceVerteces = new Integer[rows];
		final Integer[] targetVerteces = new Integer[rows];
		M.forEachNonZero(new IntIntIntFunction() {
			public int apply(int i, int j, int mij) {
				Integer sourceVertexI = sourceVerteces[i];
				//Integer targetVertexI = targetVerteces[i];
				if (sourceVertexI == null) {
					sourceVerteces[i] = j;
					targetVerteces[i] = null;
				} else {
					graph.addEdge(sourceVertexI, j);
					sourceVerteces[i] = j;
					targetVerteces[i] = null;
				}
				return mij;
			}
		});

		// search for the connected components
		List<int[]> connectedComponents = graph.listConnectedComponents(true);
		return connectedComponents;
	}
}
