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

import com.joptimizer.algebra.Matrix1NormRescaler;
import com.joptimizer.algebra.QRSparseFactorization;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.exception.KKTSolutionException;
import com.joptimizer.util.ColtUtils;

/**
 * Solves the KKT system
 * 
 * H.v + [A]T.w = -g, <br>
 * A.v = -h
 * 
 * as a whole. Note that we can not use the Cholesky factorization
 * for inverting the full KKT matrix, because it is symmetric but not
 * positive in general. 
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 542"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class FullKKTSolver extends KKTSolver {

	private static Log log = LogFactory.getLog(FullKKTSolver.class.getName());

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
		
		//compose the full KKT matrix
		DoubleMatrix2D KKT = null;
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
				
				b = F1.append(g, h).assign(DoubleFunctions.mult(-1));
			}else{
				//H.v + [A]T.w = -g
				DoubleMatrix2D[][] parts = {{ HFull, this.AT }};
				if(HFull instanceof SparseDoubleMatrix2D && A instanceof SparseDoubleMatrix2D){
					KKT = DoubleFactory2D.sparse.compose(parts);
				}else{
					KKT = DoubleFactory2D.dense.compose(parts);
				}
				b = ColtUtils.scalarMult(g, -1);
			}
		}else{
			KKT = HFull;
			b = ColtUtils.scalarMult(g, -1);
		}
		
		//factorization
		//LDLTPermutedFactorization KKTFact = new LDLTPermutedFactorization(KKT, new RequestFilter());
		QRSparseFactorization KKTFact = new QRSparseFactorization((SparseDoubleMatrix2D)KKT, new Matrix1NormRescaler());
		try{
			KKTFact.factorize();
		}catch(Exception e){
			log.error(KKTSolutionException.SINGULAR_SYSTEM);
			throw new KKTSolutionException(KKTSolutionException.SINGULAR_SYSTEM);
		}
		
		DoubleMatrix1D x = KKTFact.solve(b);
		v = x.viewPart(0, H.rows());
		w = x.viewPart(H.rows()-1, (int) (x.size()-H.rows()));

		// solution checking
		if (this.checkKKTSolutionAccuracy && !this.checkKKTSolutionAccuracy(v, w)) {
			log.error(KKTSolutionException.SOLUTION_FAILED);
			throw new KKTSolutionException(KKTSolutionException.SOLUTION_FAILED);
		}

		DoubleMatrix1D[] ret = new DoubleMatrix1D[2];
		ret[0] = v;// dim equals cols of A
		ret[1] = w;
		return ret;
	}
}
