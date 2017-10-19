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
package com.joptimizer.solvers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.exception.KKTSolutionException;
import com.joptimizer.util.ColtUtils;
import com.joptimizer.util.Utils;

/**
 * Solves the KKT system:
 * 
 * H.v + [A]T.w = -g, <br>
 * A.v = -h, <br>
 * 
 * (H is square and symmetric)
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 542"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public abstract class KKTSolver {

	protected DoubleMatrix2D H;
	protected DoubleMatrix2D A;
	protected DoubleMatrix2D AT;
	protected DoubleMatrix1D g;
	protected DoubleMatrix1D h;
	protected double toleranceKKT = Utils.getDoubleMachineEpsilon();
	protected boolean checkKKTSolutionAccuracy;
	protected DenseDoubleAlgebra ALG = DenseDoubleAlgebra.DEFAULT;
	protected DoubleFactory2D F2 = DoubleFactory2D.dense;
	protected DoubleFactory1D F1 = DoubleFactory1D.dense;
	protected double defaultScalar = 1.e-6;
	private static Log log = LogFactory.getLog(KKTSolver.class.getName());

	/**
	 * Returns two vectors v and w solutions of the KKT system.
	 */
	public abstract DoubleMatrix1D[] solve() throws JOptimizerException;

	public void setHMatrix(DoubleMatrix2D HMatrix) {
		this.H = HMatrix;
	}

	public void setAMatrix(DoubleMatrix2D AMatrix) {
		this.A = AMatrix;
		this.AT = ALG.transpose(A);
	}

	public void setGVector(DoubleMatrix1D gVector) {
		this.g = gVector;
	}

	public void setHVector(DoubleMatrix1D hVector) {
		this.h = hVector;
	}

	/**
	 * Acceptable tolerance for system resolution.
	 */
	public void setToleranceKKT(double tolerance) {
		this.toleranceKKT = tolerance;
	}

	public void setCheckKKTSolutionAccuracy(boolean b) {
		this.checkKKTSolutionAccuracy = b;
	}
	
	protected DoubleMatrix1D[] solveAugmentedKKT() throws JOptimizerException{
		if(log.isInfoEnabled()){
			log.info("solveAugmentedKKT");
		}
		if(A==null){
			log.error(KKTSolutionException.SOLUTION_FAILED);
			throw new KKTSolutionException(KKTSolutionException.SOLUTION_FAILED);
		}
		KKTSolver kktSolver = new AugmentedKKTSolver();
		kktSolver.setCheckKKTSolutionAccuracy(false);//if the caller has true, then it will make the check, otherwise no check at all
		kktSolver.setHMatrix(H);
		kktSolver.setAMatrix(A);
		kktSolver.setGVector(g);
		kktSolver.setHVector(h);
		return kktSolver.solve();
	}
	
	protected DoubleMatrix1D[] solveFullKKT() throws JOptimizerException{
		if(log.isInfoEnabled()){
			log.info("solveFullKKT");
		}
		KKTSolver kktSolver = new FullKKTSolver();
		kktSolver.setCheckKKTSolutionAccuracy(false);//if the caller has true, then it will make the check, otherwise no check at all
		kktSolver.setHMatrix(H);
		kktSolver.setAMatrix(A);
		kktSolver.setGVector(g);
		kktSolver.setHVector(h);
		return kktSolver.solve();
	}
	
	/**
	 * Check the solution of the system
	 * 
	 * 	KKT.x = b
	 * 
	 * against the scaled residual
	 * 
	 * 	beta < gamma, 
	 * 
	 * where gamma is a parameter chosen by the user and beta is
	 * the scaled residual,
	 * 
	 * 	beta = ||KKT.x-b||_oo/( ||KKT||_oo . ||x||_oo + ||b||_oo ), 
	 * with ||x||_oo = max(||x[i]||)
	 */
	protected boolean checkKKTSolutionAccuracy(DoubleMatrix1D v, DoubleMatrix1D w) {
		DoubleMatrix2D KKT = null;
		DoubleMatrix1D x = null;
		DoubleMatrix1D b = null;
		
		if (this.A != null) {
			if(this.AT==null){
				this.AT = ALG.transpose(A);
			}
			if(h!=null){
				//H.v + [A]T.w = -g
				//A.v = -h
				DoubleMatrix2D[][] parts = { 
						{ this.H, this.AT },
						{ this.A, null } };
				if(H instanceof SparseDoubleMatrix2D && A instanceof SparseDoubleMatrix2D){
					KKT = DoubleFactory2D.sparse.compose(parts);
				}else{
					KKT = DoubleFactory2D.dense.compose(parts);
				}
				x = F1.append(v, w);
				b = F1.append(g, h).assign(DoubleFunctions.mult(-1));
			}else{
				//H.v + [A]T.w = -g
				DoubleMatrix2D[][] parts = {{ this.H, this.AT }};
				if(H instanceof SparseDoubleMatrix2D && A instanceof SparseDoubleMatrix2D){
					KKT = DoubleFactory2D.sparse.compose(parts);
				}else{
					KKT = DoubleFactory2D.dense.compose(parts);
				}
				x = F1.append(v, w);
				//b = g.copy().assign(Mult.mult(-1));
				b = ColtUtils.scalarMult(g, -1);
			}
		}else{
			//H.v = -g
			KKT = this.H;
			x = v;
			//b = g.copy().assign(Mult.mult(-1));
			b = ColtUtils.scalarMult(g, -1);
		}
		
		//checking residual
		double scaledResidual = ColtUtils.calculateScaledResidual(KKT, x, b);
		if(log.isDebugEnabled()){
			log.debug("KKT inversion scaled residual: " + scaledResidual);
		}
		return scaledResidual < toleranceKKT;
	}
	
	/**
	 * Create a full data matrix starting form a symmetric matrix filled only in its subdiagonal elements.
	 */
	protected DoubleMatrix2D createFullDataMatrix(DoubleMatrix2D SubDiagonalSymmMatrix){
		int c = SubDiagonalSymmMatrix.columns();
		DoubleMatrix2D ret = F2.make(c, c);
		for(int i=0; i<c; i++){
			for(int j=0; j<=i; j++){
				ret.setQuick(i, j, SubDiagonalSymmMatrix.getQuick(i, j));
				ret.setQuick(j, i, SubDiagonalSymmMatrix.getQuick(i, j));
			}
		}
		return ret;
	}
}
