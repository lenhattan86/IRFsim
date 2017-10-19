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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.NonPositiveDefiniteMatrixException;
import org.apache.commons.math3.linear.RealMatrix;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

import com.joptimizer.util.ColtUtils;

/**
 * Generalized logarithmic barrier function for semidefinite programming.
 * <br>If F(x) = G + Sum[x_i * F_i(x),i] is the constraint of the problem, then we have:
 * <br><i>&Phi;</i> = -logdet(-F(x))
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 600"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class SDPLogarithmicBarrier implements BarrierFunction {

	private RealMatrix[] Fi = null;
	private RealMatrix G = null;
	private int dim = -1;
	private int p = -1;

	/**
	 * Build the genaralized logarithmic barrier function for the constraint
	 * <br>G + Sum[x_i * F_i(x),i] < 0,  F_i, G symmetric matrices
	 * @param Fi symmetric matrices
	 * @param G symmetric matrix
	 */
	public SDPLogarithmicBarrier(List<double[][]> FiMatrixList, double[][] GMatrix) {
		int dim = FiMatrixList.size();
		this.Fi = new RealMatrix[dim];
		RealMatrix Gg = new Array2DRowRealMatrix(GMatrix);
		if (Gg.getRowDimension() != Gg.getColumnDimension()) {
			throw new IllegalArgumentException("All matrices must be symmetric");
		}
		this.G = Gg;
		int pp = G.getRowDimension(); 
		for (int i = 0; i < dim; i++) {
			double[][] FiMatrix = FiMatrixList.get(i);
			RealMatrix Fii = new Array2DRowRealMatrix(FiMatrix);
			if (Fii.getRowDimension() != Fii.getColumnDimension() || pp!=Fii.getRowDimension()) {
				throw new IllegalArgumentException("All matrices must be symmetric and with the same dimensions");
			}
			this.Fi[i] = Fii;
		}
		this.dim = dim;
		this.p = pp;
	}

	/**
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 618"
	 */
	public double value(DoubleMatrix1D X) {
		RealMatrix S = buildS(X.toArray());
		try{
			CholeskyDecomposition cFact = new CholeskyDecomposition(S);
			double detS = cFact.getDeterminant();
			return -Math.log(detS);
		}catch(NonPositiveDefiniteMatrixException e){
			return Double.NaN;
		}
	}

	/**
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 618"
	 */
	public DoubleMatrix1D gradient(DoubleMatrix1D X) {
		DoubleMatrix1D ret = DoubleFactory1D.dense.make(dim);
		RealMatrix S = buildS(X.toArray());
		CholeskyDecomposition cFact = new CholeskyDecomposition(S);
		RealMatrix SInv = cFact.getSolver().getInverse();
		for (int i = 0; i < dim; i++) {
			ret.setQuick(i, SInv.multiply(this.Fi[i]).getTrace());
		}
		return ret;
	}

	/**
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 618"
	 */
	public DoubleMatrix2D hessian(DoubleMatrix1D X) {
		RealMatrix S = buildS(X.toArray());
		CholeskyDecomposition cFact = new CholeskyDecomposition(S);
		RealMatrix SInv = cFact.getSolver().getInverse();
		double[][] ret = new double[dim][dim];
		for (int i = 0; i < dim; i++) {
			for (int j = i; j < dim; j++) {
				double h = SInv.multiply(this.Fi[i]).multiply(SInv.multiply(this.Fi[j])).getTrace();
				ret[i][j] = h;
				ret[j][i] = h;
			}
		}
		return DoubleFactory2D.sparse.make(ret);
	}
	
	/**
	 * Create the barrier function for the Phase I.
	 * It is an instance of this class for the constraint: 
	 * <br>G + Sum[x_i * F_i(x),i] < t * I
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.6.2"
	 */
	public BarrierFunction createPhase1BarrierFunction(){
		List<double[][]> FiPh1MatrixList = new ArrayList<double[][]>();
		for(int i=0; i<this.Fi.length; i++){
			FiPh1MatrixList.add(FiPh1MatrixList.size(), this.Fi[i].getData());
		}
		FiPh1MatrixList.add(FiPh1MatrixList.size(), MatrixUtils.createRealIdentityMatrix(p).scalarMultiply(-1).getData());
		return new SDPLogarithmicBarrier(FiPh1MatrixList, this.G.getData());
	}
	
	/**
	 * Calculates the initial value for the s parameter in Phase I.
	 * Return s so that F(x)-s.I is negative definite
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.6.2"
	 * @see "S.Boyd and L.Vandenberghe, Semidefinite programming, 6.1"
	 */
	public double calculatePhase1InitialFeasiblePoint(DoubleMatrix1D originalNotFeasiblePoint, double tolerance){
		RealMatrix F = this.buildS(originalNotFeasiblePoint.toArray()).scalarMultiply(-1);
		RealMatrix S = F.scalarMultiply(-1);
		try{
			new CholeskyDecomposition(S);
			//already feasible
			return -1;
		}catch(NonPositiveDefiniteMatrixException ee){
			//it does NOT mean that F is negative, it can be not definite
			EigenDecomposition eFact = new EigenDecomposition(F);
			double[] eValues = eFact.getRealEigenvalues();
			double minEigenValue = eValues[ColtUtils.getMinIndex(DoubleFactory1D.dense.make(eValues))];
			return  -Math.min(minEigenValue * Math.pow(tolerance, -0.5), 0.);
		}
	}

//  public double calculatePhase1InitialFeasiblePoint(double[] originalNotFeasiblePoint, double tolerance){
//		RealMatrix F = this.buildS(originalNotFeasiblePoint).scalarMultiply(-1);
//		try{
//			CholeskyDecomposition cFact = new CholeskyDecomposition(F);
//			double detF = cFact.getDeterminant();
//			//if here, detF must be positive
//			return Math.pow(detF, 1./p)  + tolerance;
//		}catch(NonPositiveDefiniteMatrixException e){
//			//it does NOT mean that F is negative, it can be not definite
//			RealMatrix S = F.scalarMultiply(-1);
//			try{
//				new CholeskyDecomposition(S);
//				//already feasible
//				return -1;
//			}catch(NonPositiveDefiniteMatrixException ee){
//				EigenDecomposition eFact = new EigenDecomposition(S, Double.NaN);
//				double[] eValues = eFact.getRealEigenvalues();
//				double maxEigenValue = eValues[Utils.getMaxIndex(eValues)];
//				return maxEigenValue*2;
//			}
//		}
//	}
		
	/**
	 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 618"
	 */
	private RealMatrix buildS(double[] X) {
		RealMatrix S = this.G.scalarMultiply(-1);
		for (int i = 0; i < dim; i++) {
			S = S.add(this.Fi[i].scalarMultiply(-1 * X[i]));
		}
		return S;
	}

	public int getDim() {
		return this.dim;
	}
	
	public double getDualityGap(double t) {
		return ((double)p)/t;
	}

}
