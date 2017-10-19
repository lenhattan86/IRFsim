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

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

import com.joptimizer.algebra.CholeskyFactorization;
import com.joptimizer.algebra.Matrix1NormRescaler;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.exception.KKTSolutionException;
import com.joptimizer.util.ColtUtils;

/**
 * H.v + [A]T.w = -g, <br>
 * A.v = -h
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 542"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class BasicKKTSolver extends KKTSolver {

	private boolean avoidScaling = false;
	private static Log log = LogFactory.getLog(BasicKKTSolver.class.getName());

	public BasicKKTSolver(){
		this(false);
	}

	public BasicKKTSolver(boolean avoidScaling) {
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
			log.debug("H: " + ArrayUtils.toString(H.toArray()));
			log.debug("g: " + ArrayUtils.toString(g.toArray()));
			if (A != null) {
				log.debug("A: " + ArrayUtils.toString(A.toArray()));
			}
			if (h != null) {
				log.debug("h: " + ArrayUtils.toString(h.toArray()));
			}
		}
		CholeskyFactorization HFact = new CholeskyFactorization(H, new Matrix1NormRescaler());
		boolean isHReducible = true;
		try{
			HFact.factorize();
		}catch(Exception e){
			isHReducible = false;
		}

		if (isHReducible) {
			// Solving KKT system via elimination
			DoubleMatrix1D HInvg = HFact.solve(g);
			if (A != null) {
				DoubleMatrix2D HInvAT = HFact.solve(AT);
				DoubleMatrix2D MenoSLower = ColtUtils.subdiagonalMultiply(A, HInvAT);
				DoubleMatrix1D AHInvg = ALG.mult(A, HInvg);
				
				CholeskyFactorization MSFact = new CholeskyFactorization(MenoSLower, new Matrix1NormRescaler());
				MSFact.factorize();
				if(h == null){
					w = MSFact.solve(ColtUtils.scalarMult(AHInvg, -1));
				}else{
					//w = MSFact.solve(h.copy().assign(AHInvg, Functions.minus));
					w = MSFact.solve(ColtUtils.sum(h, AHInvg, -1));
				}
				
				v = HInvg.assign(ALG.mult(HInvAT, w), DoubleFunctions.plus).assign(DoubleFunctions.mult(-1));
			} else {
				w = null;
				v = HInvg.assign(DoubleFunctions.mult(-1));
			}
		} else {
			// H is singular
			// Solving the full KKT system
			if(A!=null){
//				KKTSolver kktSolver = new BasicKKTSolver();
//				kktSolver.setCheckKKTSolutionAccuracy(false);
				DoubleMatrix1D[] fullSol =  this.solveAugmentedKKT();
				v = fullSol[0];
				w = fullSol[1];
			}else{
				//@TODO: try with rescaled H
				log.error(KKTSolutionException.SOLUTION_FAILED);
				throw new KKTSolutionException(KKTSolutionException.SOLUTION_FAILED);
			}
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
}