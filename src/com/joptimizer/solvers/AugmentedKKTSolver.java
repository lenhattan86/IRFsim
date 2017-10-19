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

import com.joptimizer.algebra.CholeskyFactorization;
import com.joptimizer.algebra.Matrix1NormRescaler;
import com.joptimizer.algebra.MatrixRescaler;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.exception.KKTSolutionException;
import com.joptimizer.util.ColtUtils;

import cern.colt.function.tdouble.IntIntDoubleFunction;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

/**
 * Solves the KKT system
 * 
 * H.v + [A]T.w = -g, <br>
 * A.v = -h
 * 
 * with singular H. The KKT matrix is nonsingular if and only if H + ATQA > 0
 * for some Q > 0, 0, in which case, H + ATQA > 0 for all Q > 0. This class uses
 * the diagonal matrix Q = s.Id with scalar s > 0 to try finding the solution.
 * NOTE: matrix A can not be null for this solver
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 547"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class AugmentedKKTSolver extends KKTSolver {

	private double s = 1.e-6;

	private static Log log = LogFactory.getLog(AugmentedKKTSolver.class.getName());

	/**
	 * Returns the two vectors v and w.
	 */
	@Override
	public DoubleMatrix1D[] solve() throws JOptimizerException {

		if (A == null) {
			throw new IllegalStateException("Matrix A cannot be null");
		}

		DoubleMatrix1D v = null;// dim equals cols of A
		DoubleMatrix1D w = null;// dim equals rank of A

		if (log.isDebugEnabled()) {
			log.debug("H: " + ArrayUtils.toString(H.toArray()));
			log.debug("g: " + ArrayUtils.toString(g.toArray()));
			log.debug("A: " + ArrayUtils.toString(A.toArray()));
			if (h != null) {
				log.debug("h: " + ArrayUtils.toString(h.toArray()));
			}
		}

		// augmentation
		final DoubleMatrix2D HAugm = ColtUtils.subdiagonalMultiply(AT, A);// H + ATQA
//		HAugm.forEachNonZero(new IntIntDoubleFunction() {
//			public double apply(int i, int j, double HAugmij) {
//				return s * HAugmij;
//			}
//		});
		H.forEachNonZero(new IntIntDoubleFunction() {
			public double apply(int i, int j, double Hij) {
				if (i + 1 > j) {
					// the subdiagonal elements
					HAugm.setQuick(i, j, Hij + HAugm.getQuick(i, j));
				}
				return Hij;
			}
		});

		DoubleMatrix1D gAugm = null;// g + ATQh
		if (h != null) {
			DoubleMatrix1D ATQh = ALG.mult(AT, ColtUtils.diagonalMatrixMult(F1.make(A.rows(), 1), h));
			DoubleMatrix1D gATQh = ColtUtils.sum(g, ATQh, defaultScalar);
			gAugm = gATQh;
		}else{
			gAugm = g.copy();
		}

		// solving the augmented system
		CholeskyFactorization HFact = new CholeskyFactorization(HAugm, (MatrixRescaler) new Matrix1NormRescaler());
		try {
			HFact.factorize();
		} catch (JOptimizerException e) {
			// The KKT matrix is nonsingular if and only if H + ATQA > 0 for some Q > 0
			log.error(KKTSolutionException.SINGULAR_SYSTEM);
			throw new KKTSolutionException(KKTSolutionException.SINGULAR_SYSTEM);
		}

		// Solving KKT system via elimination
		DoubleMatrix1D HInvg = HFact.solve(gAugm);
		DoubleMatrix2D HInvAT = HFact.solve(AT);
		DoubleMatrix2D MenoSLower = ColtUtils.subdiagonalMultiply(A, HInvAT);
		DoubleMatrix1D AHInvg = ALG.mult(A, HInvg);

		CholeskyFactorization MSFact = new CholeskyFactorization(MenoSLower, (MatrixRescaler) new Matrix1NormRescaler());
		MSFact.factorize();
		if (h == null) {
			w = MSFact.solve(ColtUtils.scalarMult(AHInvg, -1));
		} else {
			w = MSFact.solve(ColtUtils.sum(h, AHInvg, -1));
		}

		v = HInvg.assign(ALG.mult(HInvAT, w), DoubleFunctions.plus).assign(DoubleFunctions.mult(-1));

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
	
	public void setS(double s) {
		this.s = s;
	}
}
