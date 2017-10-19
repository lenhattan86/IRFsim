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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

import com.joptimizer.algebra.CholeskyFactorization;
import com.joptimizer.algebra.CholeskyUpperDiagonalFactorization;
import com.joptimizer.algebra.Matrix1NormRescaler;
import com.joptimizer.algebra.MatrixRescaler;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.exception.KKTSolutionException;
import com.joptimizer.util.ColtUtils;

/**
 * Solves
 * 
 * H.v + [A]T.w = -g, <br>
 * A.v = -h
 * 
 * for upper diagonal H.
 * H is expected to be diagonal in its upper left corner of dimension diagonalLength.
 * Only the subdiagonal elements are relevant.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 542"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class UpperDiagonalHKKTSolver extends KKTSolver {

	private boolean avoidScaling = false;
	private int diagonalLength;
	private static Log log = LogFactory.getLog(UpperDiagonalHKKTSolver.class.getName());
	
	public UpperDiagonalHKKTSolver(int diagonalLength){
		this(diagonalLength, false);
	}

	public UpperDiagonalHKKTSolver(int diagonalLength, boolean avoidScaling) {
		this.diagonalLength = diagonalLength;
		this.avoidScaling = avoidScaling;
	}

	/**
	 * Returns the two vectors v and w.
	 */
	@Override
	public DoubleMatrix1D[] solve() throws JOptimizerException {

		DoubleMatrix1D v = null;// dim equals cols of A
		DoubleMatrix1D w = null;// dim equals rank of A
		
    if (log.isDebugEnabled()) {
			log.debug("H: " + ArrayUtils.toString(ColtUtils.fillSubdiagonalSymmetricMatrix((SparseDoubleMatrix2D)this.H).toArray()));
			log.debug("g: " + ArrayUtils.toString(g.toArray()));
			if (A!=null) {
				log.debug("A: " + ArrayUtils.toString(A.toArray()));
				log.debug("h: " + ArrayUtils.toString(h.toArray()));
			}
		}
    	MatrixRescaler rescaler = (avoidScaling)? null : new Matrix1NormRescaler();
		CholeskyUpperDiagonalFactorization HFact = new CholeskyUpperDiagonalFactorization((SparseDoubleMatrix2D)H, diagonalLength, rescaler);
		boolean isHReducible = true;
		try{
			HFact.factorize();
		}catch(Exception e){
			if(log.isWarnEnabled()){
				log.warn("warn", e);
			}
			isHReducible = false;
		}

		if (isHReducible) {
			// Solving KKT system via elimination
			DoubleMatrix1D HInvg;
			HInvg = HFact.solve(g);
			
			if (A != null) {
				DoubleMatrix2D HInvAT;
				HInvAT = HFact.solve(AT);
				
//				double scaledResidualX = Utils.calculateScaledResidual(ColtUtils.fillSubdiagonalSymmetricMatrix(H), HInvAT, AT);
//				log.info("scaledResidualX: " + scaledResidualX);
				
				DoubleMatrix2D MenoSLower = ColtUtils.subdiagonalMultiply(A, HInvAT);
				if(log.isDebugEnabled()){
					log.debug("MenoS: " + ArrayUtils.toString(ColtUtils.fillSubdiagonalSymmetricMatrix(MenoSLower).toArray()));
				}
				DoubleMatrix1D AHInvg = ALG.mult(A, HInvg);
				
				CholeskyFactorization MSFact = new CholeskyFactorization(MenoSLower, new Matrix1NormRescaler());
				//CholeskySparseFactorization MSFact = new CholeskySparseFactorization((SparseDoubleMatrix2D)MenoSLower, new RequestFilter());
				try{
					MSFact.factorize();
					if(h == null){
						w = MSFact.solve(ColtUtils.scalarMult(AHInvg, -1));
					}else{
						//w = MSFact.solve(h.copy().assign(AHInvg, Functions.minus));
						DoubleMatrix1D hmAHInvg = ColtUtils.sum(h, AHInvg, -1);
						//log.debug("hmAHInvg: " + ArrayUtils.toString(hmAHInvg.toArray()));
						w = MSFact.solve(hmAHInvg);
					}
					
					v = HInvg.assign(ALG.mult(HInvAT, w), DoubleFunctions.plus).assign(DoubleFunctions.mult(-1));
				}catch(Exception e){
					if(log.isWarnEnabled()){
						log.warn("warn: "+ e.getMessage());
					}
					if(log.isDebugEnabled()){
						log.debug("MenoS: " + ArrayUtils.toString(ColtUtils.fillSubdiagonalSymmetricMatrix(MenoSLower).toArray()));
					}
					//is it a numeric issue? try solving the full KKT system
					try{
						//NOTE: it would be more appropriate to try solving the full KKT, but if the decomposition 
						//of the Shur complement of H (>0) in KKT fails it is certainty for a numerical issue and
						//the augmented KKT seems to be more able to recover from this situation
						//DoubleMatrix1D[] fullSol =  this.solveFullKKT();
						DoubleMatrix1D[] fullSol =  this.solveAugmentedKKT();
						v = fullSol[0];
						w = fullSol[1];
					}catch(Exception ex){
						log.error(ex.getMessage());
						throw new KKTSolutionException(ex.getMessage());
					}
				}
				
			} else {
				//A==null
				w = null;
				v = HInvg.assign(DoubleFunctions.mult(-1));
			}
		} else {
			// H not isReducible, try solving the augmented KKT system
			DoubleMatrix1D[] fullSol = this.solveAugmentedKKT();
			v = fullSol[0];
			w = fullSol[1];
		}

		// solution checking
		if (this.checkKKTSolutionAccuracy && !this.checkKKTSolutionAccuracy(v, w)) {
			log.error(KKTSolutionException.SOLUTION_FAILED);
			throw new KKTSolutionException(KKTSolutionException.SOLUTION_FAILED);
		}

		DoubleMatrix1D[] ret = new DoubleMatrix1D[2];
		ret[0] = v;
		ret[1] = w;
		return ret;
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
	 * 
	 * @TODO: avoid overriding, leave only the parent 
	 */
	@Override
	protected boolean checkKKTSolutionAccuracy(DoubleMatrix1D v, DoubleMatrix1D w) {
		if(log.isDebugEnabled()){
			log.debug("checkKKTSolutionAccuracy");
		}
		DoubleMatrix2D KKT = null;
		DoubleMatrix1D x = null;
		DoubleMatrix1D b = null;
		final DoubleMatrix2D HFull = ColtUtils.fillSubdiagonalSymmetricMatrix((SparseDoubleMatrix2D)this.H);
		
		if (this.A != null) {
			if(h!=null){
				//H.v + [A]T.w = -g
				//A.v = -h
				DoubleMatrix2D[][] parts = { 
						{ HFull, this.AT },
						{ this.A, null } };
				if(HFull instanceof SparseDoubleMatrix2D && A instanceof SparseDoubleMatrix2D){
					KKT = DoubleFactory2D.sparse.compose(parts);
				}else{
					KKT = DoubleFactory2D.dense.compose(parts);
				}
				
				x = F1.append(v, w);
				b = F1.append(g, h).assign(DoubleFunctions.mult(-1));
			}else{
				//H.v + [A]T.w = -g
				DoubleMatrix2D[][] parts = {{ HFull, this.AT }};
				if(HFull instanceof SparseDoubleMatrix2D && A instanceof SparseDoubleMatrix2D){
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
			KKT = HFull;
			x = v;
			//b = g.copy().assign(Mult.mult(-1));
			b = ColtUtils.scalarMult(g, -1);
		}
		
		//checking residual
		double scaledResidual = ColtUtils.calculateScaledResidual(KKT, x, b);
		if(log.isInfoEnabled()){
			log.info("KKT inversion scaled residual: " + scaledResidual);
		}
		return scaledResidual < toleranceKKT;
	}

	public void setDiagonalLength(int diagonalLength) {
		this.diagonalLength = diagonalLength;
	}

	public int getDiagonalLength() {
		return diagonalLength;
	}
}
