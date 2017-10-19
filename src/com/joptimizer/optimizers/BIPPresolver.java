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
package com.joptimizer.optimizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.ArithmeticUtils;

import com.joptimizer.exception.InfeasibleProblemException;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.util.ColtUtils;
import com.joptimizer.util.Utils;

import cern.colt.function.tint.IntIntIntFunction;
import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;
import cern.colt.matrix.tint.impl.SparseIntMatrix2D;

/**
 * Presolver for a binary integer problem in the form of:
 * 
 * <br>min c.x s.t. 
 * <br>G.x <= h 
 * <br>x in {0, 1}<sup>n</sup>
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * @see G. Gamrath, T. Koch, A. Martin, M. Miltenberger, D. Weninger, "Progress in Presolving for Mixed Integer Programming" [1]
 * @see Bob Bixby, Zonghao Gu, Ed Rothberg "Presolve for Linear and Mixed-Integer Programming" [2]
 * @see A. Fugenschuh, A. Martin "Computational Integer Programming and Cutting Planes" [3]
 */
public class BIPPresolver {

	private boolean removeDominatingColumns = true;
	private boolean checkMinMaxFeasibility = true;
	private boolean resolveConnectedComponents = false;
	private boolean checkProgress = false;

	private int originalN;// number of variables
	private int originalMieq;// number of variables

	// after presolving fields
	private int presolvedN = -1;
	private int presolvedMieq = -1;
	private boolean[] indipendentVariables;
	private boolean[] indipendentConstraints;
	private int[] presolvedColumnsPos;
	private int[] presolvedRowsPos;
	private int[] presolvedColumns;
	private int[] presolvedRows;
	private int[] selectionColumnIndexes;
	private int[] selectionRowIndexes;
	private IntMatrix1D presolvedC = null;
	private IntMatrix2D presolvedG = null;
	private IntMatrix1D presolvedH = null;
	private List<Set<Integer>> connectedComponents = null;

	/**
	 * Row by row non-zeroes entries of G (the rows supports). 
	 * <br>Remember that for a vector x in R<sup>n</sup>, 
	 * supp(x) = {i in {1,2,...,n} | x[i]!=0} is the <i>support</i> of x.
	 */
	private int[][] rowsSupport;
	/**
	 * Column by column non-zeroes entries of G (the columns supports).
	 * <br>Remember that for a vector x in R<sup>n</sup>, 
	 * supp(x) = {i in {1,2,...,n} | x[i]!=0} is the <i>support</i> of x.
	 */
	private int[][] colsSupport;
	int[] duplicatedColsArray = new int[] {};
	private int[] L;// min constraints values (L[i] <= G[i].x)
	private int[] U;// max constraints values (U[i] >= G[i].x)
	/**
	 * All the coefficients of the constraint are >= 0.
	 * Its is calculated on the row support, hence the sign >=0
	 */
	private boolean[] allNotNegative;
	/**
	 * All the coefficients of the constraint are <= 0.
	 * Its is calculated on the row support, hence the sign <=0
	 */
	private boolean[] allNotPositive;
	private int[][] vRowLengthMap;
	private int[][] vColLengthMap;
	private boolean someReductionDone = true;
	private List<PresolvingStackElement> presolvingStack = new ArrayList<PresolvingStackElement>();
	private static Log log = LogFactory.getLog(BIPPresolver.class.getName());
	
	public void presolve(final IntMatrix1D originalC, final IntMatrix2D originalG, final IntMatrix1D originalH) throws JOptimizerException{
		long t0 = System.currentTimeMillis();
		
		if(1==2 && log.isDebugEnabled()){
			StringBuffer sb = new StringBuffer("BIP problem:\n");
			sb.append("min(c.x) s.t\n");
			sb.append("G.x <= h\n");
			sb.append("where\n");
			sb.append("c=" + ArrayUtils.toString(originalC.toArray()) + "\n");
			sb.append("G=" + ArrayUtils.toString(originalG.toArray()) + "\n");
			sb.append("h=" + ArrayUtils.toString(originalH.toArray()) + "\n");
			log.debug(sb.toString());
		}

		// working entities definition
		final IntMatrix1D c = originalC;//this must NOT change
		final IntMatrix2D G = originalG;//this must NOT change
		final IntMatrix1D h = (originalH != null) ? originalH.copy() : IntFactory1D.dense.make(0);//this can change
		
		this.originalN = originalG.columns();
		this.originalMieq = originalG.rows();
		this.indipendentVariables = new boolean[originalN];
		Arrays.fill(indipendentVariables, true);
//		this.L = new int[originalMieq];
//		this.U = new int[originalMieq];
		this.allNotNegative = new boolean[originalMieq];
		this.allNotPositive = new boolean[originalMieq];
		this.vRowLengthMap = new int[1 + originalN][];// the first position is for 0-length rows
		this.vColLengthMap = new int[1 + originalMieq][];// the first position is for 0-length columns
		int entries = originalN * originalMieq;
		int cardinality = 0;
		int[] vColCounter = new int[originalN];// counter of non-zero values in each column
		rowsSupport = new int[originalMieq][0];
		if (G instanceof SparseIntMatrix2D) {
			final int[] cardinalityHolder = new int[] { cardinality };
			final int[] vColCounterPH = new int[originalN];

			// view G column by column
			for (int column = 0; column < originalN; column++) {
				// log.debug("column:" + c);
				final int[] currentColumnIndexHolder = new int[] { column };
				IntMatrix2D GPart = G.viewPart(0, column, originalMieq, 1);
				GPart.forEachNonZero(new IntIntIntFunction() {
					public int apply(int i, int j, int qij) {
						// log.debug("i:"+i+",j:"+currentColumnIndexHolder[0]+", qij="+qij);
						cardinalityHolder[0] = cardinalityHolder[0] + 1;
						if (rowsSupport[i] == null) {
							rowsSupport[i] = new int[] {};
						}
						rowsSupport[i] = ArrayUtils.add(rowsSupport[i], rowsSupport[i].length, currentColumnIndexHolder[0]);
						vColCounterPH[currentColumnIndexHolder[0]] = (vColCounterPH[currentColumnIndexHolder[0]] + 1);
						//G.setQuick(i, currentColumnIndexHolder[0], qij);
						return qij;
					}
				});
			}

			cardinality = cardinalityHolder[0];
			vColCounter = vColCounterPH;

			// check empty row
			for (int i = 0; i < originalMieq; i++) {
				int[] rowsSupportI = rowsSupport[i];
				if (rowsSupportI.length < 1) {
					if (h.getQuick(i) < 0) {
						// 0 <= negativo
						//log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
						throw new InfeasibleProblemException(InfeasibleProblemException.INFEASIBLE_PROBLEM);
					}
				}
				if (this.vRowLengthMap[rowsSupportI.length] == null) {
					vRowLengthMap[rowsSupportI.length] = new int[] { i };
				} else {
					vRowLengthMap[rowsSupportI.length] = Utils.addToSortedArray(vRowLengthMap[rowsSupportI.length], i);
				}
			}
		} else {
			for (int i = 0; i < originalMieq; i++) {
				int[] rowsSupportI = new int[] {};
				for (int j = 0; j < originalN; j++) {
					int Gij = G.getQuick(i, j);
					if (!Utils.isZero(Gij)) {
						cardinality++;
						rowsSupportI = ArrayUtils.add(rowsSupportI, rowsSupportI.length, j);
						vColCounter[j]++;
					}
				}
				// check empty row
				if (rowsSupportI.length < 1) {
					if (h.getQuick(i) < 0) {
						// 0 <= negativo
						//log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
						throw new InfeasibleProblemException();
					}
				}
				rowsSupport[i] = rowsSupportI;
				if (this.vRowLengthMap[rowsSupportI.length] == null) {
					vRowLengthMap[rowsSupportI.length] = new int[] { i };
				} else {
					vRowLengthMap[rowsSupportI.length] = Utils.addToSortedArray(vRowLengthMap[rowsSupportI.length], i);
				}
			}
		}

		// check empty columns
//		for (int j = 0; j < vColCounter.length; j++) {
//			if (vColCounter[j] == 0) {
//				// empty column
//				if (originalC.getQuick(j) > 0) {
//					// variable x fixed at its lower bound
//					log.debug("found empty column: " + j);
//					addToPresolvingStack(new LinearDependency(j, null, null, 0));
//					removeFixedVariable(c, G, h, j, 0);
//				} else if (originalC.getQuick(j) < 0) {
//					// variable x fixed at its upper bound
//					log.debug("found empty column: " + j);
//					addToPresolvingStack(new LinearDependency(j, null, null, 1));
//					removeFixedVariable(c, G, h, j, 0);
//				}
//			}
//		}

		// fill not-zero columns holders
		this.colsSupport = new int[originalN][];
		for (int j = 0; j < originalN; j++) {
			int length = vColCounter[j];
			this.colsSupport[j] = new int[length];
			if (this.vColLengthMap[length] == null) {
				vColLengthMap[length] = new int[] { j };
			} else {
				vColLengthMap[length] = Utils.addToSortedArray(vColLengthMap[length], j);
			}
		}
		for (int i = 0; i < rowsSupport.length; i++) {
			int[] rowsSupportI = rowsSupport[i];
			for (int j = 0; j < rowsSupportI.length; j++) {
				int col = rowsSupportI[j];
				this.colsSupport[col][colsSupport[col].length - vColCounter[col]] = i;
				vColCounter[rowsSupportI[j]]--;
			}
		}

		vColCounter = null;
		if(log.isDebugEnabled()){
			log.debug("sparsity index: " + 100 * ((double) (entries - cardinality)) / ((double) entries) + "% (" + cardinality 	+ "nz/" + entries + "tot)");
		}

		// check empty columns
		//this is not a reduction, because colsSupport will not change (it is already empty for an empty column) 
		for (int j = 0; j < colsSupport.length; j++) {
			if (colsSupport[j].length == 0) {
				// empty column
				if (c.getQuick(j) < 0) {
					// variable x fixed at its upper bound
					if(log.isDebugEnabled()){
						log.debug("found empty column: x[" + j + "]=" + 1);
					}
					addToPresolvingStack(new LinearDependency(j, 1));
					removeFixedVariable(c, G, h, j, 0);
				} else {
					// variable x fixed at its lower bound
					if(log.isDebugEnabled()){
						log.debug("found empty column: x[" + j + "]=" + 0);
					}
					addToPresolvingStack(new LinearDependency(j, 0));
					removeFixedVariable(c, G, h, j, 0);
				}
			}
		}
		
		//calculateConstraintsActivity(c, G, h);
		
		// repeat
		int iteration = 0;
		while (someReductionDone) {
			iteration++;
			someReductionDone = false;// reset
			
			if (log.isDebugEnabled()) {
				log.debug("iteration: " + iteration);
				// log.debug("c: "+ArrayUtils.toString(c));
				// log.debug("A: "+ArrayUtils.toString(A));
				// log.debug("b: "+ArrayUtils.toString(b));
				// log.debug("lb: "+ArrayUtils.toString(lb));
				// log.debug("ub: "+ArrayUtils.toString(ub));
			}
			
			checkProgress(c, G, h);
			
			// remove all forcing constraints
			// rimettimi: 
			removeForcingConstraints(c, G, h);
			// rimettimi: 
			checkProgress(c, G, h);
			
			// remove redundant constraints
			// rimettimi: 
			removeRedundantConstraints(c, G, h);
			// rimettimi: checkProgress(c, G, h);

			// same sign reduction
			// rimettimi: 
			sameSignReduction(c, G, h);
			// rimettimi: checkProgress(c, G, h);
			
			// tightening bounds
			// rimettimi: 
			tighteningBounds(c, G, h);
			// rimettimi: checkProgress(c, G, h);
			
			// duality fixing
			// rimettimi: 
			dualityFixing(c, G, h);
			// rimettimi: checkProgress(c, G, h);
			
			// coefficient reduction
			// rimettimi: 
			coefficientReduction(c, G, h);
			// rimettimi: checkProgress(c, G, h);
			
			// dominating column reduction
			if(removeDominatingColumns){
				removeDominatingColumns(c, G, h);
				checkProgress(c, G, h);
			}
			
			calculateConstraintsActivity(c, G, h);
		}
		
		//check feasibility with min and max support
		if(checkMinMaxFeasibility){
			checkMinMaxFeasibility(c, G, h);
		}
		
		
		// conflicting graph reduction
		if(resolveConnectedComponents){
			conflictingGraphReduction(c, G, h);
			checkProgress(c, G, h);
			
			resolveConnectedComponents(c, G, h);
		}
		
		removeAllEmptyRowsAndColumns(c, G, h);

		presolvedN = 0;
		presolvedColumns = new int[originalN];// longer than it needs
		Arrays.fill(presolvedColumns, -1);
		presolvedColumnsPos = new int[originalN];
		Arrays.fill(presolvedColumnsPos, -1);
		for (int j = 0; j < indipendentVariables.length; j++) {
			if (indipendentVariables[j]) {
				presolvedColumns[presolvedN] = j;
				presolvedColumnsPos[j] = presolvedN;
				presolvedN++;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("final n              : " + presolvedN);
			//log.debug("indipendentVariables : " + ArrayUtils.toString(indipendentVariables));
			log.debug("presolvedColumns     : " + ArrayUtils.toString(presolvedColumns));
			log.debug("presolvedColumnsPos  : " + ArrayUtils.toString(presolvedColumnsPos));
		}

		presolvedMieq = 0;
		presolvedRows = new int[originalMieq];// longer than it needs
		Arrays.fill(presolvedRows, -1);
		presolvedRowsPos = new int[originalMieq];
		Arrays.fill(presolvedRowsPos, -1);
		for (int i = 0; i < rowsSupport.length; i++) {
			if (rowsSupport[i].length > 0) {
				presolvedRows[presolvedMieq] = i;
				presolvedRowsPos[i] = presolvedMieq;
				presolvedMieq++;
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("final mieq           : " + presolvedMieq);
			log.debug("presolvedRows        : " + ArrayUtils.toString(presolvedRows));
			log.debug("presolvedRowsPos     : " + ArrayUtils.toString(presolvedRowsPos));
		}

		this.selectionColumnIndexes = new int[] {};
		if (presolvedN > 0) {
			selectionColumnIndexes = new int[presolvedN];
			int cntC = 0;
			for (int j = 0; j < originalN; j++) {
				if (colsSupport[j].length > 0) {
					selectionColumnIndexes[cntC] = j;
					cntC++;
				}
			}
		}
		
		this.selectionRowIndexes = new int[] {};
		if (presolvedMieq > 0) {
			selectionRowIndexes = new int[presolvedMieq];
			int cntR = 0;
			for (int i = 0; i < originalMieq; i++) {
				if (rowsSupport[i].length > 0) {
					selectionRowIndexes[cntR] = i;
					cntR++;
				}
			}
		}
		
		presolvedC = (presolvedN > 0)? c.viewSelection(selectionColumnIndexes) : null;
		presolvedG = (presolvedN > 0)? G.viewSelection(selectionRowIndexes, selectionColumnIndexes) : null;
		presolvedH = (presolvedN > 0)? h.viewSelection(selectionRowIndexes) : null;//values can change

		if (log.isDebugEnabled()) {
			log.debug("presolvingStack : " + presolvingStack);
		}
		if(log.isInfoEnabled()){
			log.info("end presolving (" + (System.currentTimeMillis() - t0) + " ms)");
		}
	}
	
	private void removeForcingConstraints(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("removeForcingConstraints");
		}
		
		boolean removeForcingConstraintsDone = true;
		//reiteration: fixing some variables, other variables could be removed
		while(removeForcingConstraintsDone){
			
			removeForcingConstraintsDone = false;
			calculateConstraintsActivity(c, G, h);
			
			for (int i = 0; i < rowsSupport.length; i++) {
				//log.debug(g[i]+","+b[i]);
				//log.debug(h[i]+","+b[i]);
				//log.debug("delta g[" + i  + "] = " + (b.getQuick(i) - this.g[i]));
				//log.debug("delta h[" + i  + "] = " + (this.h[i] - b.getQuick(i)));
				int[] rowsSupportI = rowsSupport[i];
				if (rowsSupportI.length > 0) {
					int[] forcedVariablesI = new int[] {};
					int[] forcedVariablesV = new int[] {};
					if (Utils.isZero(this.L[i] - h.getQuick(i))) {
						// forcing constraint
						if(log.isDebugEnabled()){
							log.debug("found forcing constraint at row: " + i);
						}
						// the only feasible value of xj is l[j] (u[j]) if A(i,j) > 0 (A(i,j) < 0).
						// Therefore, we can fix all variables in the ith constraint.
						for (int nz = 0; nz < rowsSupportI.length; nz++) {
							int j = rowsSupportI[nz];
							int aij = G.getQuick(i, j);
							int value;
							if (aij > 0) {
								value = 0;
							} else {
								value = 1;
							}
							if(log.isDebugEnabled()){
								log.debug("x[" + j + "]=" + value);
							}
							forcedVariablesI = ArrayUtils.add(forcedVariablesI, j);
							forcedVariablesV = ArrayUtils.add(forcedVariablesV, value);
							//addToPresolvingStack(new LinearDependency(j, null, null, value));
						}
					}
					if (forcedVariablesI.length > 0) {
						//immediate substitution
						removeForcingConstraintsDone = true;
						someReductionDone = true;
						// there are forced variables to substitute
						for (int fv = 0; fv < forcedVariablesI.length; fv++) {
							int x = forcedVariablesI[fv];
							int value = forcedVariablesV[fv];
							addToPresolvingStack(new LinearDependency(x, value));
							//updateConstraintsActivity(c, G, h, x, value);
							removeFixedVariable(c, G, h, x, value);
						}
					}
				}
			}
		}
	}
	
	/**
	 * @see [2] p. 19.
	 * Note: this method is isolated from {@link BIPPresolver#removeForcingConstraints} because it does not reiterate.
	 */
	private void removeRedundantConstraints(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("removeRedundantConstraints");
		}
		
		calculateConstraintsActivity(c, G, h);
		
		for (int i = 0; i < rowsSupport.length; i++) {
			// log.debug(g[i]+","+b[i]);
			// log.debug(h[i]+","+b[i]);
			int[] rowsSupportI = rowsSupport[i];
			if (rowsSupportI.length > 0) {
				if(this.U[i] <= h.getQuick(i)){
					//this constraint is redundant
					if(log.isDebugEnabled()){
						log.debug("redundant constraint at row " + i);
					}
					for(int x : rowsSupport[i]){
						int[] colsSupportX = colsSupport[x];
						changeColumnsLengthPosition(x, colsSupportX.length, colsSupportX.length - 1);
						colsSupport[x] = Utils.removeFromSortedArray(colsSupportX, i);
						if(colsSupport[x].length < 1){
							//this variables can be fixed
							int value = (c.getQuick(x) > 0) ? 0 : 1; 
							addToPresolvingStack(new LinearDependency(x, value));
							if(log.isDebugEnabled()){
								log.debug("x[" + x + "]=" + value);	
							}
							removeFixedVariable(c, G, h, x, value);
						}
					}
					h.setQuick(i, 0);
					changeRowsLengthPosition(i, rowsSupport[i].length, 0);
					rowsSupport[i] = new int[]{};
					this.someReductionDone = true;
				} else {
					// look for duplicated rows
					int[] vRowLengthMapI = vRowLengthMap[rowsSupportI.length];
					for (int vir = 0; vir < vRowLengthMapI.length; vir++) {
						int r = vRowLengthMapI[vir];
						if (r != i) {
							int[] rowsSupportR = rowsSupport[r];
//							if(rowsSupportR.length != rowsSupportI.length){
//								throw new IllegalStateException();
//							}
							if (isSameSparsityPattern(rowsSupportR, rowsSupportI)) {
								//log.debug("same sparsity pattern for row " + i + " and " + r);
								// are rows equals?
								boolean areDuplicated = true;
								for (int k = 0; areDuplicated && k < rowsSupportI.length; k++) {
									if(log.isDebugEnabled()){
										log.debug("r=" + r + ",k=" + k);
									}
									areDuplicated = Utils.isZero(G.getQuick(r, rowsSupportR[k]) - G.getQuick(i, rowsSupportI[k]));
								}
								if(areDuplicated){
									//log.debug("duplicated constraints at row " + i + " and " + r);
									int redundantRow = (h.getQuick(r) > h.getQuick(i)) ? r : i;
									//this constraint is redundant
									if(log.isDebugEnabled()){
										log.debug("redundant constraint at row " + redundantRow);
									}
									for(int x : rowsSupport[redundantRow]){
										int[] colsSupportX = colsSupport[x];
										changeColumnsLengthPosition(x, colsSupportX.length, colsSupportX.length - 1);
										colsSupport[x] = Utils.removeFromSortedArray(colsSupportX, redundantRow);
										if(colsSupport[x].length < 1){
											//this variables can be fixed
											int value = (c.getQuick(x) > 0) ? 0 : 1; 
											addToPresolvingStack(new LinearDependency(x, value));
											if(log.isDebugEnabled()){
												log.debug("x[" + x + "]=" + value);
											}
											removeFixedVariable(c, G, h, x, value);
										}
									}
									h.setQuick(redundantRow, 0);
									changeRowsLengthPosition(redundantRow, rowsSupport[redundantRow].length, 0);
									rowsSupport[redundantRow] = new int[]{};
									this.someReductionDone = true;
									
									if(redundantRow == i){
										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void sameSignReduction(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("sameSignReduction");
		}
		
		boolean sameSignReductionDone = true;
		while(sameSignReductionDone){
			
			sameSignReductionDone = false;
			calculateConstraintsActivity(c, G, h);
			
			for (int i = 0; i < rowsSupport.length; i++) {
				// log.debug(g[i]+","+b[i]);
				// log.debug(h[i]+","+b[i]);
				int[] rowsSupportI = rowsSupport[i];
				if (rowsSupportI.length > 0) {
					int[] forcedVariablesPos = new int[] {};
					int[] forcedVariablesVal = new int[] {};
					// check if we can tight upper bounds. leveraging the fact that,
					// typically,
					// the problem has lb => 0 for most variables.
					// if coefficients are all positive or all negative the bounds can be limited
					boolean t1 = this.allNotNegative[i] && (this.L[i] >= 0 && h.getQuick(i) >= 0);
					boolean t2 = this.allNotPositive[i] && (this.U[i] <= 0 && h.getQuick(i) <= 0);
					if (t1) {
						for (int nz = 0; nz < rowsSupportI.length; nz++) {
							int nzj = rowsSupportI[nz];
							double d = ((double)h.getQuick(i)) / G.getQuick(i, nzj);
							// we can limit upper bound to d
							if (d < 1) {
								if(d < 0){
									//log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
									throw new InfeasibleProblemException(InfeasibleProblemException.INFEASIBLE_PROBLEM);
								}else{
									if(log.isDebugEnabled()){
										log.debug("all same signs reduction at row " + i);
										log.debug("x[" + nzj + "]=" + 0);
									}
									forcedVariablesPos = ArrayUtils.add(forcedVariablesPos, nzj);
									forcedVariablesVal = ArrayUtils.add(forcedVariablesVal, 0);
								}
							}
						}
					}
					if (t2) {
						//@TODO to be implemented
					}
					if (forcedVariablesPos.length > 0) {
						//immediate substitution
						sameSignReductionDone = true;
						someReductionDone = true;
						// there are forced variables to substitute
						for (int fv = 0; fv < forcedVariablesPos.length; fv++) {
							int x = forcedVariablesPos[fv];
							int value = forcedVariablesVal[fv];
							addToPresolvingStack(new LinearDependency(x, value));
							//updateConstraintsActivity(c, G, h, x, value);
							removeFixedVariable(c, G, h, x, value);
						}
					}
				}
			}//end of rows loop
		}
	}
	
	private void tighteningBounds(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("tighteningBounds");
		}
		
		boolean tighteningBoundsDone = true;
		while (tighteningBoundsDone) {

			tighteningBoundsDone = false;
			calculateConstraintsActivity(c, G, h);

			for (int r = 0; r < rowsSupport.length; r++) {
				// log.debug(g[i]+","+b[i]);
				// log.debug(h[i]+","+b[i]);
				int[] rowsSupportI = rowsSupport[r];
				int[] forcedVariablesPos = new int[] {};
				int[] forcedVariablesVal = new int[] {};
				for (int nzj : rowsSupportI) {
					int grj = G.getQuick(r, nzj);
					if(grj > 0){
						double uPrj = ((double)(h.getQuick(r) - L[r] + 0 * grj)) / grj;
						if(uPrj < 1){
							if(log.isDebugEnabled()){
								log.debug("uPrj:" + uPrj);
								log.debug("tightening bound at row " + r);
								log.debug("x[" + nzj + "]=" + 1);
							}
							forcedVariablesPos = ArrayUtils.add(forcedVariablesPos, nzj);
							forcedVariablesVal = ArrayUtils.add(forcedVariablesVal, 0);
						}
					}else{
						double lPrj = ((double)(h.getQuick(r) - L[r] + 1 * grj)) / grj;
						if(lPrj > 0){
							if(log.isDebugEnabled()){
								log.debug("lPrj:" + lPrj);
								log.debug("tightening bound at row " + r);
								log.debug("x[" + nzj + "]=" + 1);
							}
							forcedVariablesPos = ArrayUtils.add(forcedVariablesPos, nzj);
							forcedVariablesVal = ArrayUtils.add(forcedVariablesVal, 1);
						}
						if(h.getQuick(r) < L[r] + 1 * grj ){
							//the limit is less than the minimum constraint activity withot the contribution of this variable:
							//the only choice is to set the variable to its upper bound
							forcedVariablesPos = ArrayUtils.add(forcedVariablesPos, nzj);
							forcedVariablesVal = ArrayUtils.add(forcedVariablesVal, 1);
						}
					}
				}
				if (forcedVariablesPos.length > 0) {
					//immediate substitution
					tighteningBoundsDone = true;
					someReductionDone = true;
					// there are forced variables to substitute
					for (int fv = 0; fv < forcedVariablesPos.length; fv++) {
						int x = forcedVariablesPos[fv];
						int value = forcedVariablesVal[fv];
						addToPresolvingStack(new LinearDependency(x, value));
						removeFixedVariable(c, G, h, x, value);
					}
				}
			}//end of rows loop
		}
	}

	/**
	 * @see [1], eq (4) and (5).
	 */
//	private void tighteningBoundsNEW(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) {
//		log.info("tighteningBounds");
//		
//		boolean tighteningBoundsDone = true;
//		while (tighteningBoundsDone) {
//
//			tighteningBoundsDone = false;
//			calculateConstraintsActivity(c, G, h);
//
//			for (int r = 0; r < rowsSupport.length; r++) {
//				// log.debug(g[i]+","+b[i]);
//				// log.debug(h[i]+","+b[i]);
//				int[] rowsSupportI = rowsSupport[r];
//				for (int nzj : rowsSupportI) {
//					int forcedVariablesPos = -1;
//					int forcedVariablesVal = -1;
//					double grj = G.getQuick(r, nzj);
//					if(grj > 0){
//						double uPrj = (h.getQuick(r) - this.L[r] + 0 * grj) / grj;
//						if(uPrj < 1){
//							log.debug("uPrj:" + uPrj);
//							log.debug("tightening bound at row " + r);
//							log.debug("x[" + nzj + "]=" + 1);
//							forcedVariablesPos = nzj;
//							forcedVariablesVal = 0;
//						}
//					}else{
//						double lPrj = (h.getQuick(r) - this.L[r] + 1 * grj) / grj;
//						if(lPrj > 0){
//							log.debug("lPrj:" + lPrj);
//							log.debug("tightening bound at row " + r);
//							log.debug("x[" + nzj + "]=" + 1);
//							forcedVariablesPos = nzj;
//							forcedVariablesVal = 1;
//						}else if(h.getQuick(r) < this.L[r] - 1 * grj ){
//							//the limit is less than the minimum constraint activity without the contribution of this variable:
//							//the only choice is to set the variable to its upper bound
//							log.debug("tightening bound at row " + r);
//							log.debug("x[" + nzj + "]=" + 1);
//							forcedVariablesPos = nzj;
//							forcedVariablesVal = 1;
//						}
//					}
//					if (forcedVariablesPos > -1) {
//						//immediate substitution
//						tighteningBoundsDone = true;
//						someReductionDone = true;
//						// there are forced variables to substitute
//						int x = forcedVariablesPos;
//						int value = forcedVariablesVal;
//						addToPresolvingStack(new LinearDependency(x, value));
//						//updateConstraintsActivity(c, G, h, x, value);
//						removeFixedVariable(c, G, h, x, value);
//						calculateConstraintsActivity(c, G, h);
//					}
//				}
//			}//end of rows loop
//		}
//	}
	
	/**
	 * @see [3] p.5
	 */
	private void dualityFixing(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("dualityFixing");
		}
		
		for (int j = 0; j < this.colsSupport.length; j++) {
			int fixedValue = -1;
			int[] colsSupportJ = this.colsSupport[j];
			if (colsSupportJ.length > 0) {
				if (fixedValue < 0 && c.get(j) >= 0) {
					boolean fixColumn = true;
					for (int r = 0; fixColumn && r < colsSupportJ.length; r++) {
						int i = colsSupportJ[r];
						fixColumn = G.getQuick(i, j) >= 0;
					}
					if (fixColumn) {
						fixedValue = 0;
					}
				}
				if (fixedValue < 0 && c.get(j) <= 0) {
					boolean fixColumn = true;
					for (int r = 0; fixColumn && r < colsSupportJ.length; r++) {
						int i = colsSupportJ[r];
						fixColumn = G.getQuick(i, j) <= 0;
					}
					if (fixColumn) {
						fixedValue = 1;
					}
				}
			}
			
			if (fixedValue > -1) {
				if(log.isDebugEnabled()){
					log.debug("duality fixing at column " + j);
					log.debug("x[" + j + "]=" + fixedValue);
				}
				addToPresolvingStack(new LinearDependency(j, fixedValue));
				//updateConstraintsActivity(c, G, h, j, fixedValue);
				removeFixedVariable(c, G, h, j, fixedValue);
				this.someReductionDone = true;
			}
		}
	}
	
	/**
	 * @see [1] p.3
	 */
//	private void stuffingSingletonColumns(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) {
//		int[] vColLengthMap1 = this.vColLengthMap[1];//the set of column singletons
//		for (int j = 0; vColLengthMap1 != null && j < vColLengthMap1.length; j++) {
//			//j is a column singleton
//			for (int r = 0; r < this.rowsSupport.length; r++) {
//				int[] rowsSupportR = rowsSupport[r];
//				if(ArrayUtils.contains(rowsSupportR, j)){//@TODO: search in an ordered array
//					double cj = c.getQuick(j);
//					double GRJ = G.getQuick(r, j);
//					if (cj / GRJ < 0) {
//						//@see [1] p.3
//						// consider all other column singleton of this row
//						int[] forcedVariablesPos = new int[] {};
//						int[] forcedVariablesVal = new int[] {};
//						final boolean case1 = GRJ > 0 && cj < 0;
//						//boolean case2 = GRJ < 0 && cj > 0;//this is !case1
//						double UTildeR = 0d;
//						double LTildeR = 0d;
//						List<double[]> ratios = new ArrayList<double[]>();
//						for (int nzj : rowsSupportR) {
//							double cnzj = c.getQuick(nzj);
//							double Gnzj = G.getQuick(r, nzj);
//							boolean isInJR = (case1) ? (Gnzj > 0 && cnzj < 0) : (Gnzj < 0 && cnzj > 0);
//							isInJR &= ArrayUtils.contains(vColLengthMap1, nzj);
//							if (isInJR) {
//								//nzj in J(r)
//								UTildeR += Gnzj * 0;
//								LTildeR += Gnzj * 0;
//								ratios.add(ratios.size(), new double[]{nzj, cnzj / Gnzj});
//							}else{
//								UTildeR += ((Gnzj > 0) ? Gnzj * 1 : 0);
//								LTildeR += ((Gnzj < 0) ? Gnzj * 1 : 0);
//							}
//						}
//						//sorting
//						Collections.sort(ratios, new Comparator<double[]>() {
//							public int compare(double[] o1, double[] o2) {
//								if(case1){
//									return Double.compare(o1[1], o2[1]);
//								}else{
//									return Double.compare(o2[1], o1[1]);
//								}
//							}
//						});
//						for(int i=0; i<ratios.size(); i++){
//							double[] ratiosI = ratios.get(i);
//							int x = (int)ratiosI[0];
//							double alpha = G.getQuick(r, x) * 1;
//							double beta  = G.getQuick(r, x) * 0;
//							boolean b1 = (case1)? !(alpha > h.getQuick(r) - UTildeR + beta) : !(alpha > h.getQuick(r) - LTildeR + beta);
//							boolean b2 = (case1)? !(beta > LTildeR) : !(beta > UTildeR);
//							if(b1){
//								log.debug("stuffing column singleton at row " + r);
//								log.debug("x[" + x + "]=" + 1);
//								forcedVariablesPos = ArrayUtils.add(forcedVariablesPos, x);
//								forcedVariablesVal = ArrayUtils.add(forcedVariablesVal, 1);
//							}else if(b2){
//								log.debug("stuffing column singleton at row " + r);
//								log.debug("x[" + x + "]=" + 0);
//								forcedVariablesPos = ArrayUtils.add(forcedVariablesPos, x);
//								forcedVariablesVal = ArrayUtils.add(forcedVariablesVal, 0);
//							}
//							UTildeR = UTildeR + alpha - beta;
//							LTildeR = LTildeR + alpha - beta;
//						}
//						if (forcedVariablesPos.length > 0) {
//							// there are forced variables to substitute
//							for (int fv = 0; fv < forcedVariablesPos.length; fv++) {
//								int forcedPos = forcedVariablesPos[fv];
//								int forcedVal = forcedVariablesVal[fv];
//								addToPresolvingStack(new LinearDependency(forcedPos, null, null, forcedVal));
//								removeFixedVariable(c, G, h, forcedPos, forcedVal);
//							}
//							someReductionDone = true;
//						}
//					}
//					break;//the only row has been found
//				}
//			}
//		}
//	}
	
	/**
	 * Note: we only reduce for new coefficient = 0.
	 * @see [3] p. 6
	 */
	private void coefficientReduction(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("coefficientReduction");
		}
		
		calculateConstraintsActivity(c, G, h);
		
		for (int i = 0; i < rowsSupport.length; i++) {
			// log.debug(g[i]+","+b[i]);
			// log.debug(h[i]+","+b[i]);
			int[] rowsSupportI = rowsSupport[i];
			int Ui = U[i];
			int hi = h.getQuick(i);
			for (int k = 0; k < rowsSupportI.length; k++) {//ordered loop
				int j = rowsSupportI[k];
				int gij = G.getQuick(i, j);
				if(gij < 0 && Ui + gij < hi){
					int gijNew = hi - Ui;
					if(Utils.isZero(gijNew)){
						//we can reduce without updating Ui because gij < 0 (and so Ui will not be changed)
						if(log.isDebugEnabled()){
							log.debug("coefficient reduction at row " + i + " G[" + i + "," + j + "]=" + gijNew);
						}
						//G.setQuick(i, j, 0);
						rowsSupport[i] = Utils.removeFromSortedArray(rowsSupport[i], j);
						changeRowsLengthPosition(i, rowsSupport[i].length + 1, rowsSupport[i].length);
						colsSupport[j] = Utils.removeFromSortedArray(colsSupport[j], i);
						changeColumnsLengthPosition(j, colsSupport[j].length + 1, colsSupport[j].length);
						if(colsSupport[j].length < 1){
							//this variables can be fixed
							int value = (c.getQuick(j) > 0) ? 0 : 1; 
							addToPresolvingStack(new LinearDependency(j, value));
							if(log.isDebugEnabled()){
								log.debug("x[" + j + "]=" + value);
							}
						}
						this.someReductionDone = true;
					}
				}else if(gij > 0 && Ui - gij < hi){
					int gijNew = Ui - hi;
					if(Utils.isZero(gijNew)){
						//we must update Ui because gij > 0
						int hiNew = Ui - gij;
						if(log.isDebugEnabled()){
							log.debug("coefficient reduction at row " + i + " G[" + i + "," + j + "]=" + gijNew + ", h[" + i + "]="	+ hiNew);
						}
						//G.setQuick(i, j, 0);
						h.setQuick(i, hiNew);
						rowsSupport[i] = Utils.removeFromSortedArray(rowsSupport[i], j);
						changeRowsLengthPosition(i, rowsSupport[i].length + 1, rowsSupport[i].length);
						colsSupport[j] = Utils.removeFromSortedArray(colsSupport[j], i);
						changeColumnsLengthPosition(j, colsSupport[j].length + 1, colsSupport[j].length);
						if(colsSupport[j].length < 1){
							//this variables can be fixed
							int value = (c.getQuick(j) > 0) ? 0 : 1; 
							addToPresolvingStack(new LinearDependency(j, value));
							if(log.isDebugEnabled()){
								log.debug("x[" + j + "]=" + value);
							}
						}
						calculateConstraintsActivity(c, G, h, i);
						this.someReductionDone = true;
					}
				}
			}
		}
	}
	
	/**
	 * We start with the row that contains the fewest non-zeros 
	 * and compare only columns that have a non-zero entry in this row. 
	 * After one row was executed, the processed variables therein are not compared to other variables anymore.
	 * NOTE: this method can be very slow.
	 * @see [1] p. 4
	 */
	private void removeDominatingColumns(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("removeDominatingColumns");
			
			if(1==2){
				removeAllEmptyRowsAndColumns(c, G, h);

				presolvedN = 0;
				presolvedColumns = new int[originalN];// longer than it needs
				Arrays.fill(presolvedColumns, -1);
				presolvedColumnsPos = new int[originalN];
				Arrays.fill(presolvedColumnsPos, -1);
				for (int jj = 0; jj < indipendentVariables.length; jj++) {
					if (indipendentVariables[jj]) {
						presolvedColumns[presolvedN] = jj;
						presolvedColumnsPos[jj] = presolvedN;
						presolvedN++;
					}
				}
				if (log.isDebugEnabled()) {
					log.debug("final n              : " + presolvedN);
					//log.debug("indipendentVariables : " + ArrayUtils.toString(indipendentVariables));
					log.debug("presolvedColumns     : " + ArrayUtils.toString(presolvedColumns));
					log.debug("presolvedColumnsPos  : " + ArrayUtils.toString(presolvedColumnsPos));
				}

				presolvedMieq = 0;
				presolvedRows = new int[originalMieq];// longer than it needs
				Arrays.fill(presolvedRows, -1);
				presolvedRowsPos = new int[originalMieq];
				Arrays.fill(presolvedRowsPos, -1);
				for (int ii = 0; ii < rowsSupport.length; ii++) {
					if (rowsSupport[ii].length > 0) {
						presolvedRows[presolvedMieq] = ii;
						presolvedRowsPos[ii] = presolvedMieq;
						presolvedMieq++;
					}
				}

				if (log.isDebugEnabled()) {
					log.debug("final mieq           : " + presolvedMieq);
					log.debug("presolvedRows        : " + ArrayUtils.toString(presolvedRows));
					log.debug("presolvedRowsPos     : " + ArrayUtils.toString(presolvedRowsPos));
				}

				this.selectionColumnIndexes = new int[] {};
				if (presolvedN > 0) {
					selectionColumnIndexes = new int[presolvedN];
					int cntC = 0;
					for (int jj = 0; jj < originalN; jj++) {
						if (colsSupport[jj].length > 0) {
							selectionColumnIndexes[cntC] = jj;
							cntC++;
						}
					}
				}
				
				this.selectionRowIndexes = new int[] {};
				if (presolvedMieq > 0) {
					selectionRowIndexes = new int[presolvedMieq];
					int cntR = 0;
					for (int ii = 0; ii < originalMieq; ii++) {
						if (rowsSupport[ii].length > 0) {
							selectionRowIndexes[cntR] = ii;
							cntR++;
						}
					}
				}
				
				presolvedC = (presolvedN > 0)? c.viewSelection(selectionColumnIndexes) : null;
				presolvedG = (presolvedN > 0)? G.viewSelection(selectionRowIndexes, selectionColumnIndexes) : null;
				presolvedH = (presolvedN > 0)? h.viewSelection(selectionRowIndexes) : null;//values can change
				if (log.isDebugEnabled()) {
					log.debug("presolvedC: " + ArrayUtils.toString(presolvedC.toArray()));
					log.debug("presolvedG: " + ArrayUtils.toString(presolvedG.toArray()));
					log.debug("presolvedH: " + ArrayUtils.toString(presolvedH.toArray()));
				}
			}
			
			
		}
		
		int[] processedVariables = new int[] {};
		//int[] forcedVariablesPos = new int[] {};
		//int[] forcedVariablesVal = new int[] {};
		
		boolean removeDominatingColumnsDone = true;
		while (removeDominatingColumnsDone) {
			removeDominatingColumnsDone = false;
			
			for (int l = 1; !removeDominatingColumnsDone && l < vRowLengthMap.length; l++) {
				int[] vRowLengthMapL = vRowLengthMap[l];
				if (vRowLengthMapL != null && vRowLengthMapL.length > 0) {
					for (int rl = 0; !removeDominatingColumnsDone && rl < vRowLengthMapL.length; rl++) {
						int r = vRowLengthMapL[rl];
						int[] rowsSupportR = rowsSupport[r];
						for (int j = 0; !removeDominatingColumnsDone && j < rowsSupportR.length; j++) {
							int xj = rowsSupportR[j];
							if(ArrayUtils.contains(processedVariables, xj)){
								continue;
							}
							processedVariables = Utils.addToSortedArray(processedVariables, xj);
							
							for (int i = 0; !removeDominatingColumnsDone && i < rowsSupportR.length; i++) {
								int xi = rowsSupportR[i];
								if(xi == xj){
									continue;
								}
								
								// dominance test
								if (!(c.getQuick(xj) > c.getQuick(xi))) {
									boolean dominance = true;
									for (int row = 0; dominance && row < originalMieq; row++) {
										int[] rowsSupportRow = rowsSupport[row];
										if(rowsSupportRow.length > 0){
											int Grj = (Utils.isInSortedArray(rowsSupportRow, xj))? G.getQuick(row, xj) : 0;
											int Gri = (Utils.isInSortedArray(rowsSupportRow, xi))? G.getQuick(row, xi) : 0;
											dominance &= !(Grj > Gri);
										}
									}
									if (dominance) {
										// xj the dominating variable and xi the dominated variable
										// xj >> xi
										int forcedPos = -1;
										int forcedVal = -1;
										String dominanceCase = null;
										
										if (forcedPos < 0) {
											double maxLtsGhxixj0 = maxLts(G, h, xi, xj, 0);
											if (Double.MAX_VALUE > maxLtsGhxixj0 && !(maxLtsGhxixj0 < 1)) {
												// Corollary 1 case 1
												forcedPos = xj;
												forcedVal = 1;
												dominanceCase = "1.1";
											}
										} 
										if (forcedPos < 0) {
											double minLtsGhxjxi1 = minLts(G, h, xj, xi, 1);
											if (-Double.MAX_VALUE < minLtsGhxjxi1 && !(minLtsGhxjxi1 > 0)) {
												// Corollary 1 case 2
												forcedPos = xi;
												forcedVal = 0;
												dominanceCase = "1.2";
											}
										} 
										if (forcedPos < 0) {
											double minUtsGhxixj0 = minUts(G, h, xi, xj, 0);
											if (forcedPos != xj && !(c.getQuick(xj) > 0) && (Double.MAX_VALUE > minUtsGhxixj0 && !(minUtsGhxixj0 < 1))) {
												// Corollary 1 case 3
												forcedPos = xj;
												forcedVal = 1;
												dominanceCase = "1.3";
											}
										} 
										if (forcedPos < 0) {
											double maxUtsGhxjxi1 = maxUts(G, h, xj, xi, 1);
											if (!(c.getQuick(xi) < 0) && (-Double.MAX_VALUE < maxUtsGhxjxi1 && !(maxUtsGhxjxi1 > 0))) {
												// Corollary 1 case 4
												forcedPos = xi;
												forcedVal = 0;
												dominanceCase = "1.4";
											}
										} 
										if (forcedPos < 0) {
											double maxLtsGhxjxi1 = maxLts(G, h, xj, xi, 1);
											if (Double.MAX_VALUE > maxLtsGhxjxi1 && !(maxLtsGhxjxi1 < 0)) {
												// Corollary 2 case 1
												forcedPos = xj;
												forcedVal = 1;
												dominanceCase = "2.1";
											}
										} 
										if (forcedPos < 0) {
											double minLtsGhxixj0 = minLts(G, h, xi, xj, 0);
											if (-Double.MAX_VALUE < minLtsGhxixj0 && !(minLtsGhxixj0 > 1)) {
												// Corollary 2 case 2
												forcedPos = xi;
												forcedVal = 0;
												dominanceCase = "2.2";
											}
										}
										if (forcedPos < 0) {
											double minUtsGhxjxi1 = minUts(G, h, xj, xi, 1);
											if (!(c.getQuick(xj) > 0) && (Double.MAX_VALUE > minUtsGhxjxi1 && !(minUtsGhxjxi1 < 0))) {
												// Corollary 2 case 3
												forcedPos = xj;
												forcedVal = 1;
												dominanceCase = "2.3";
											}
										} 
										if (forcedPos < 0) {
											double maxUtsGhxixj0 = maxUts(G, h, xi, xj, 0);
											if (!(c.getQuick(xi) < 0) && (-Double.MAX_VALUE < maxUtsGhxixj0 && !(maxUtsGhxixj0 > 1))) {
												// Corollary 2 case 4
												forcedPos = xi;
												forcedVal = 0;
												dominanceCase = "2.4";
											}
										}
										
										if(forcedPos > -1){
											if(log.isDebugEnabled()){
												log.debug("column dominance (case "	+ dominanceCase	+ "): dominating=" + xj + ", dominated=" + xi + ", x[" + forcedPos + "]="	+ forcedVal);
											}
											
											// there are a forced variable to substitute
											log.debug("forcedPos: " + forcedPos);
											
											addToPresolvingStack(new LinearDependency(forcedPos, forcedVal));
											//updateConstraintsActivity(c, G, h, forcedPos, forcedVal);
											removeFixedVariable(c, G, h, forcedPos, forcedVal);
											checkProgress(c, G, h);
											//break;
											
											someReductionDone = true;
											removeDominatingColumnsDone = true;
											//break;
											
											//calculateConstraintsActivity(c, G, h);
										}

									}
								}
							}
						}
					}
				}
			}
		}
		//log.debug("processedVariables: " + ArrayUtils.toString(processedVariables));
	}
	
	/**
	 * We start with the row that contains the fewest non-zeros 
	 * and compare only columns that have a non-zero entry in this row. 
	 * After one row was executed, the processed variables therein are not compared to other variables anymore.
	 * NOTE: this method can be very slow.
	 * @see [1] p. 4
	 */
//	private void removeDominatingColumnsOLD(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
//		if(log.isDebugEnabled()){
//			log.debug("removeDominatingColumnsOLD");
//		}
//		
//		int[] processedVariables = new int[] {};
//		int[] forcedVariablesPos = new int[] {};
//		int[] forcedVariablesVal = new int[] {};
//		for (int l = 1; processedVariables.length < originalN && l < vRowLengthMap.length; l++) {
//			int[] vRowLengthMapL = vRowLengthMap[l];
//			if (vRowLengthMapL != null && vRowLengthMapL.length > 0) {
//				for (int r : vRowLengthMapL) {
//					int[] rowsSupportR = rowsSupport[r];
//					for (int j = 0; j < rowsSupportR.length; j++) {
//						int xj = rowsSupportR[j];
//						
//						if(ArrayUtils.contains(processedVariables, xj) || ArrayUtils.contains(forcedVariablesPos, xj)){
//							continue;
//						}
//						processedVariables = Utils.addToSortedArray(processedVariables, xj);
//						
//						for (int i = 0; i < rowsSupportR.length; i++) {
//							if(i == j){
//								continue;
//							}
//							
//							int xi = rowsSupportR[i];
//							if(ArrayUtils.contains(processedVariables, xi) || ArrayUtils.contains(forcedVariablesPos, xi)){
//								continue;
//							}
//							
//							// dominance test
//							if (!(c.getQuick(xj) > c.getQuick(xi))) {
//								boolean dominance = true;
//								for (int row = 0; dominance && row < originalMieq; row++) {
//									int[] rowsSupportRow = rowsSupport[row];
//									if(rowsSupportRow.length > 0){
//										int Grj = (Utils.isInSortedArray(rowsSupportRow, xj))? G.getQuick(row, xj) : 0;
//										int Gri = (Utils.isInSortedArray(rowsSupportRow, xi))? G.getQuick(row, xi) : 0;
//										dominance &= !(Grj > Gri);
//									}
//								}
//								if (dominance) {
//									// xj the dominating variable and xi the dominated variable
//									// xj >> xi
//									int forcedPos = -1;
//									int forcedVal = -1;
//									String dominanceCase = null;
//									double maxLtsGhxixj0 = maxLts(G, h, xi, xj, 0);
//									if (Double.MAX_VALUE > maxLtsGhxixj0 && !(maxLtsGhxixj0 < 1)) {
//										// Corollary 1 case 1
//										forcedPos = xj;
//										forcedVal = 1;
//										dominanceCase = "1.1";
//									} 
//									if (forcedPos != xi) {
//										double minLtsGhxjxi1 = minLts(G, h, xj, xi, 1);
//										if (-Double.MAX_VALUE < minLtsGhxjxi1 && !(minLtsGhxjxi1 > 0)) {
//											// Corollary 1 case 2
//											forcedPos = xi;
//											forcedVal = 0;
//											dominanceCase = "1.2";
//										}
//									} 
//									if (forcedPos != xj) {
//										double minUtsGhxixj0 = minUts(G, h, xi, xj, 0);
//										if (forcedPos != xj && !(c.getQuick(xj) > 0) && (Double.MAX_VALUE > minUtsGhxixj0 && !(minUtsGhxixj0 < 1))) {
//											// Corollary 1 case 3
//											forcedPos = xj;
//											forcedVal = 1;
//											dominanceCase = "1.3";
//										}
//									} 
//									if (forcedPos != xi) {
//										double maxUtsGhxjxi1 = maxUts(G, h, xj, xi, 1);
//										if (!(c.getQuick(xi) < 0) && (-Double.MAX_VALUE < maxUtsGhxjxi1 && !(maxUtsGhxjxi1 > 0))) {
//											// Corollary 1 case 4
//											forcedPos = xi;
//											forcedVal = 0;
//											dominanceCase = "1.4";
//										}
//									} 
//									if (forcedPos != xj) {
//										double maxLtsGhxjxi1 = maxLts(G, h, xj, xi, 1);
//										if (Double.MAX_VALUE > maxLtsGhxjxi1 && !(maxLtsGhxjxi1 < 0)) {
//											// Corollary 2 case 1
//											forcedPos = xj;
//											forcedVal = 1;
//											dominanceCase = "2.1";
//										}
//									} 
//									if (forcedPos != xi) {
//										double minLtsGhxixj0 = minLts(G, h, xi, xj, 0);
//										if (-Double.MAX_VALUE < minLtsGhxixj0 && !(minLtsGhxixj0 > 1)) {
//											// Corollary 2 case 2
//											forcedPos = xi;
//											forcedVal = 0;
//											dominanceCase = "2.2";
//										}
//									}
//									if (forcedPos != xj) {
//										double minUtsGhxjxi1 = minUts(G, h, xj, xi, 1);
//										if (!(c.getQuick(xj) > 0) && (Double.MAX_VALUE > minUtsGhxjxi1 && !(minUtsGhxjxi1 < 0))) {
//											// Corollary 2 case 3
//											forcedPos = xj;
//											forcedVal = 1;
//											dominanceCase = "2.3";
//										}
//									} 
//									if (forcedPos != xi) {
//										double maxUtsGhxixj0 = maxUts(G, h, xi, xj, 0);
//										if (!(c.getQuick(xi) < 0) && (-Double.MAX_VALUE < maxUtsGhxixj0 && !(maxUtsGhxixj0 > 1))) {
//											// Corollary 2 case 4
//											forcedPos = xi;
//											forcedVal = 0;
//											dominanceCase = "2.4";
//										}
//									}
//									
//									if(forcedPos > -1){
//										if(log.isDebugEnabled()){
//											log.debug("column dominance (case "	+ dominanceCase	+ "): dominating=" + xj + ", dominated=" + xi + ", x[" + forcedPos + "]="	+ forcedVal);
//										}
//										forcedVariablesPos = ArrayUtils.add(forcedVariablesPos, forcedPos);
//										forcedVariablesVal = ArrayUtils.add(forcedVariablesVal, forcedVal);
//										break;
//									}
//
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		if (forcedVariablesPos.length > 0) {
//			// there are forced variables to substitute
//			for (int fv = 0; fv < forcedVariablesPos.length; fv++) {
//				int forcedPos = forcedVariablesPos[fv];
//				int forcedVal = forcedVariablesVal[fv];
//				log.debug("forcedPos: " + forcedPos);
//				addToPresolvingStack(new LinearDependency(forcedPos, forcedVal));
//				//updateConstraintsActivity(c, G, h, forcedPos, forcedVal);
//				removeFixedVariable(c, G, h, forcedPos, forcedVal);
//				checkProgress(c, G, h);
//				//break;
//			}
//			someReductionDone = true;
//		}
//		//log.debug("processedVariables: " + ArrayUtils.toString(processedVariables));
//	}
	
	private void checkMinMaxFeasibility(IntMatrix1D c, IntMatrix2D G, IntMatrix1D h) throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("checkMinMaxFeasibility");
		}

		for (int i = 0; i < h.size(); i++) {
			int hI = h.getQuick(i);
			if (hI < 0) {
				int[] rowsSupportI = rowsSupport[i];
				int[] GI = new int[rowsSupportI.length];
				if(this.allNotPositive[i]){
					//the situation is: Gi.x <= pi, Gi<=0 and pi<0
					//count the minimum number of xi that must be =1
					//NOTE: on rowsSupportI we have all GI < 0
					int minCount = -1;
					
					for (int ii = 0; ii < h.size(); ii++) {
						if(ii == i){
							continue;
						}
						int hII = h.getQuick(ii);
						if (hII > 0 && allNotNegative[ii]) {
							
							if(minCount < 0){
								for (int k = 0; k < rowsSupportI.length; k++) {
									GI[k] = G.getQuick(i, rowsSupportI[k]);
								}
								Arrays.sort(GI);
								int sumN = 0;
								int k = 0;
								for (k = 0; sumN > hI && k < GI.length; k++) {
									sumN += GI[k];
								}
								minCount = k;
							}
							
							//the situation is: a.x >= pi, ai>=0 and pi>0
							//count the maximum number of xi that can be =1
							//NOTE: we try on the same support of index i, so we consider potential coefficients = 0 
							int[] rowsSupportII = rowsSupport[ii];
							int[] GII = new int[rowsSupportI.length];
							for (int kk = 0; kk < rowsSupportI.length; kk++) {
								int jj = rowsSupportI[kk];
								if(Utils.isInSortedArray(rowsSupportII, jj)){
									GII[kk] = G.getQuick(ii, jj);
								}else{
									GII[kk] = 0;
								}
							}
							Arrays.sort(GII);
							int sumP = 0;
							int kk = 0;
							for (kk = 0; kk < GII.length; kk++) {
								if(sumP + GII[kk] <= hII){
									sumP += GII[kk];
								}else{
									break;
								}
							}
							int maxCount = kk;
							if(maxCount < minCount){
								if(log.isDebugEnabled()){
									log.debug("checkMinMaxFeasibility found infeasibility for rows " + i + " (minCount=" + minCount + ") and " + ii + " (maxCount=" + maxCount + ")");
								}
								//log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
								throw new InfeasibleProblemException(InfeasibleProblemException.INFEASIBLE_PROBLEM);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @see [1] p. 11
	 * @see http://www3.cs.stonybrook.edu/~algorith/files/dfs-bfs.shtml
	 */
	private void conflictingGraphReduction(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) {
		//@TODO: implement this
	}
	
	/**
	 * @see [1] p. 12
	 * @see http://www3.cs.stonybrook.edu/~algorith/files/dfs-bfs.shtml
	 */
	private void resolveConnectedComponents(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) {
		log.info("resolveConnectedComponents");
		List<int[]> connectedComponents = ColtUtils.listConnectedComponents(G);
		log.info("number of Connected Components: " + connectedComponents.size());
		if(connectedComponents.size() > 1){
			throw new RuntimeException("to be implemented !!!");	
		}
	}

	private void removeAllEmptyRowsAndColumns(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D hh) {
//		int[] vRowLengthMap0 = vRowLengthMap[0];
//		if (vRowLengthMap0 != null && vRowLengthMap0.length > 0) {
//			for(int i : vRowLengthMap0){
//				//found empty row 1
//				int[] rowsSupportI = this.rowsSupport[i];
//			}
//		}
	}
	
	/**
	 * Calculate the conditional maximal activity of the linear constraint r w.r.t. xsi.
	 * @param t the column
	 * @param r the row
	 * @param xsi the value
	 * @see [1] p.6
	 */
	private int Utr(IntMatrix2D G, int t, int r, int xsi) {
		int[] rowsSupportR = this.rowsSupport[r];
		int U = (Utils.isInSortedArray(rowsSupportR, t))? G.getQuick(r, t) * xsi : 0;
		for(int k : rowsSupportR){
			if(k != t){
				int Grk = G.getQuick(r, k);
				if(Grk > 0){
					U += Grk * 1;
				}else{
					U += Grk * 0;
				}
			}
		}
		
		return U;
	}

	/**
	 * Calculate the conditional minimal activity of the linear constraint r w.r.t. xsi.
	 * @param t the column
	 * @param r the row
	 * @param xsi the value
	 * @see [1] p.6
	 */
	private int Ltr(IntMatrix2D G, int t, int r, int xsi) {
		int[] rowsSupportR = this.rowsSupport[r];
		int L = (Utils.isInSortedArray(rowsSupportR, t))? G.getQuick(r, t) * xsi : 0;
		for (int k : rowsSupportR) {
			if (k != t) {
				int Grk = G.getQuick(r, k);
				if (Grk > 0) {
					L += Grk * 0;
				} else {
					L += Grk * 1;
				}
			}
		}
		
		return L;
	}

	/**
	 * @see [1] p.7
	 */
	private double maxLts(final IntMatrix2D G, final IntMatrix1D h, int t, int s, int xsi) {
		double ret = -Double.MAX_VALUE;
		for (int r = 0; r < originalMieq; r++) {
			int[] rowsSupportR = this.rowsSupport[r];
			int Grs = (Utils.isInSortedArray(rowsSupportR, s))? G.getQuick(r, s) : 0;
			int Grt = (Utils.isInSortedArray(rowsSupportR, t))? G.getQuick(r, t) : 0;
			if (Grs < 0 && Grt < 0) {
				ret = Math.max(ret, ((double)(h.getQuick(r) - Ltr(G, t, r, xsi) + Grs * 1)) / Grs);
			}
		}

		return ret;
	}

	/**
	 * @see [1] p.6
	 */
	private double maxUts(final IntMatrix2D G, final IntMatrix1D h, int t, int s, int xsi) {
		double ret = -Double.MAX_VALUE;
		for (int r = 0; r < originalMieq; r++) {
			int[] rowsSupportR = this.rowsSupport[r];
			int Grs = (Utils.isInSortedArray(rowsSupportR, s))? G.getQuick(r, s) : 0;
			int Grt = (Utils.isInSortedArray(rowsSupportR, t))? G.getQuick(r, t) : 0;
			if (Grs < 0 && Grt < 0) {
				ret = Math.max(ret, ((double)(h.getQuick(r) - Utr(G, t, r, xsi) + Grs * 0)) / Grs);
			}
		}

		return ret;
	}

	/**
	 * @see [1] p.6
	 */
	private double minLts(final IntMatrix2D G, final IntMatrix1D h, int t, int s, int xsi) {
		double ret = Double.MAX_VALUE;
		for (int r = 0; r < originalMieq; r++) {
			int[] rowsSupportR = this.rowsSupport[r];
			int Grs = (Utils.isInSortedArray(rowsSupportR, s))? G.getQuick(r, s) : 0;
			int Grt = (Utils.isInSortedArray(rowsSupportR, t))? G.getQuick(r, t) : 0;
			if (Grs > 0 && Grt > 0) {
				ret = Math.min(ret, ((double)(h.getQuick(r) - Ltr(G, t, r, xsi) + Grs * 0)) / Grs);
			}
		}

		return ret;
	}

	/**
	 * @see [1] p.6
	 */
	private double minUts(final IntMatrix2D G, final IntMatrix1D h, int t, int s, int xsi) {
		double ret = Double.MAX_VALUE;
		for (int r = 0; r < originalMieq; r++) {
			int[] rowsSupportR = this.rowsSupport[r];
			int Grs = (Utils.isInSortedArray(rowsSupportR, s))? G.getQuick(r, s) : 0;
			int Grt = (Utils.isInSortedArray(rowsSupportR, t))? G.getQuick(r, t) : 0;
			if (Grs > 0 && Grt > 0) {
				ret = Math.min(ret, ((double)(h.getQuick(r) - Utr(G, t, r, xsi) + Grs * 1)) / Grs);
			}
		}

		return ret;
	}
	
	private void removeFixedVariable(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h, int x, int value) throws JOptimizerException{
		
		// substitution into the other equations
		for (int k = 0; k < this.rowsSupport.length; k++) {
			int[] rowsSupportK = rowsSupport[k];
			for (int nz = 0; nz < rowsSupportK.length; nz++) {
				if (rowsSupportK[nz] == x) {
					// this row contains x
					if (rowsSupportK.length == 1) {
						if (G.getQuick(k, x) * value > h.getQuick(k)) {
							//ax > b
							//log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
							throw new InfeasibleProblemException(InfeasibleProblemException.INFEASIBLE_PROBLEM);
						}
					}
					
					h.setQuick(k, h.getQuick(k) - G.getQuick(k, x) * value);
					//G.setQuick(k, x, 0.);
					rowsSupportK = ArrayUtils.remove(rowsSupportK, nz);
					rowsSupport[k] = rowsSupportK;
					changeRowsLengthPosition(k, rowsSupport[k].length + 1, rowsSupport[k].length);
					break;
				} else if (rowsSupportK[nz] > x) {
					break;
				}
			}
		}//row substitution terminated
		int[] colsSupportX = colsSupport[x];
		changeColumnsLengthPosition(x, colsSupportX.length, 0);
		colsSupport[x] = new int[] {};
		this.someReductionDone = true;
	}
	
	private abstract class PresolvingStackElement {
		abstract void postSolve(IntMatrix1D x);

		abstract void preSolve(IntMatrix1D x);
	}

	/**
	 * x = q + Sum_i[mi * xi]
	 * NOTE: the relation
	 * x = q + Sum_i[mi * xi]
	 * is NOT admitted because substitution will cause changes on the constraints matrix.
	 */
	private class LinearDependency extends PresolvingStackElement {
		int x;
		int q;

		LinearDependency(int x, int q) {
			this.x = x;
			this.q = q;
		}

		@Override
		void postSolve(IntMatrix1D postsolvedX) {
			// es x[5] = q
			postsolvedX.setQuick(this.x, postsolvedX.getQuick(this.x) + this.q);
		}

		@Override
		void preSolve(IntMatrix1D v) {
			// es x[1]=+1.0*x[2]+-5.0
			// for(int k=0; this.xi!=null && k<this.xi.length; k++){
			//   v[this.x] += this.mi[k] * v[this.xi[k]];
			// }
			// v[this.x] += this.q;
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("x[" + x + "]=");
			sb.append("+" + q);
			return sb.toString();
		}
	}

	private void addToPresolvingStack(LinearDependency linearDependency) {
		this.indipendentVariables[linearDependency.x] = false;
		presolvingStack.add(presolvingStack.size(), linearDependency);
	}

	/**
	 * This method is just for testing scope.
	 * NOTE: it is not possible to check against an expectedSolution (as in the case of convex optimization)
	 * because we can have multiple local minimum, and the presolver may select one of them.
	 * For example, xi must be = 1 for the expected solution, but xi must be = 0 for the presolver.
	 */
	private void checkProgress(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) {
		
		if(!this.checkProgress){
			return;
		}
		
		// nz Gij
		for (int i = 0; i < G.rows(); i++) {
			int[] rowsSupportI = rowsSupport[i];
			for (int j = 0; j < G.columns(); j++) {
				if (Utils.isZero(G.getQuick(i, j))) {
					if (ArrayUtils.contains(rowsSupportI, j)) {
						if(log.isDebugEnabled()){
							log.debug("entry " + i + "," + j + " is non-zero: " + G.getQuick(i, j));
						}
						throw new IllegalStateException();
					}
					if (ArrayUtils.contains(colsSupport[j], i)) {
						if(log.isDebugEnabled()){
							log.debug("entry " + i + "," + j + " is non-zero: " + G.getQuick(i, j));
						}
						throw new IllegalStateException();
					}
				}
			}
		}
		
		for (int i = 0; i < vRowLengthMap.length; i++) {
			int[] vRowLengthMapI = vRowLengthMap[i];
			for (int k = 0; vRowLengthMapI!=null && k < vRowLengthMapI.length; k++) {
				int myRow = vRowLengthMapI[k];
				int[] rowsSupportMyRow = rowsSupport[myRow];
				if(rowsSupportMyRow.length != i){
					throw new IllegalStateException();
				}
			}
		}
	}

	private void changeRowsLengthPosition(int rowIndex, int lengthIndexFrom, int lengthIndexTo) {
		if (lengthIndexFrom == 0) {
			return;
		}
		if (vRowLengthMap[lengthIndexTo] == null) {
			vRowLengthMap[lengthIndexTo] = new int[] {};
		}
		vRowLengthMap[lengthIndexTo] = Utils.addToSortedArray(vRowLengthMap[lengthIndexTo], rowIndex);
		vRowLengthMap[lengthIndexFrom] = Utils.removeFromSortedArray(vRowLengthMap[lengthIndexFrom], rowIndex);
	}

	private void changeColumnsLengthPosition(int colIndex, int lengthIndexFrom, int lengthIndexTo) {
		if (lengthIndexFrom == 0) {
			return;
		}
		if (vColLengthMap[lengthIndexTo] == null) {
			vColLengthMap[lengthIndexTo] = new int[] {};
		}
		vColLengthMap[lengthIndexTo] = Utils.addToSortedArray(vColLengthMap[lengthIndexTo], colIndex);
		vColLengthMap[lengthIndexFrom] = Utils.removeFromSortedArray(vColLengthMap[lengthIndexFrom], colIndex);
	}
	
	/**
	 * Updates the minimal (L) and maximal (U) constraints activity for all rows.
	 */
	private void calculateConstraintsActivity(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h) throws JOptimizerException{
		if (this.L == null || this.U == null || this.someReductionDone) {
			if(log.isDebugEnabled()){
				log.debug("calculateConstraintsActivity");
			}
			this.L = new int[originalMieq];
			this.U = new int[originalMieq];
			for (int row = 0; row < rowsSupport.length; row++) {
				calculateConstraintsActivity(c, G, h, row);
			}
		}
	}
	
	/**
	 * Updates the minimal (L) and maximal (U) constraints activity of a given row.
	 * @param row the row constraint to update.
	 */
	private void calculateConstraintsActivity(final IntMatrix1D c, final IntMatrix2D G, final IntMatrix1D h, int row) throws JOptimizerException{
			//log.error("i " + i);
			int[] rowsSupportI = rowsSupport[row];
			int LI = 0;
			int UI = 0;
			boolean allNotNegativeI = false;//all >= 0
			boolean allNotPositiveI = false;//all <= 0
			if (rowsSupportI.length > 0) {
				allNotNegativeI = true;
				allNotPositiveI = true;
				for (int j : rowsSupportI) {
					int GIJ = G.getQuick(row, j);
					if (Utils.isZero(GIJ)) {
						log.error("G[" + row + "][" + j + "]: expected non-zero but was " + GIJ);
						throw new IllegalStateException("G[" + row + "][" + j + "]: expected non-zero but was " + GIJ);
					} else if (GIJ > 0) {
						// j in P
						//Li += GIJ * 0;
						UI = ArithmeticUtils.addAndCheck(UI, GIJ * 1);
						allNotPositiveI = false;
					} else {
						// j in M
						LI = ArithmeticUtils.addAndCheck(LI, GIJ * 1);
						//Ui += GIJ * 0;
						allNotNegativeI = false;
					}
				}
			}
			int hi = h.getQuick(row);
			//LI <= LHS
			if (LI > hi) {
				//log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
				throw new InfeasibleProblemException(InfeasibleProblemException.INFEASIBLE_PROBLEM);
			}
			
			this.L[row] = LI;
			this.U[row] = UI;
			this.allNotNegative[row] = allNotNegativeI;
			this.allNotPositive[row] = allNotPositiveI;
	}
	
	/**
	 * Updates the minimal (L) and maximal (U) constraints activity for a fixed value of a variable.
	 * @param j the fixed variable.
	 * @param the value of the fixed variable.
	 */
//	private void xxxupdateConstraintsActivity(IntMatrix1D c, IntMatrix2D G, IntMatrix1D h, int col, int value) {
//		for (int row = 0; row < rowsSupport.length; row++) {
//			xxxupdateConstraintsActivity(c, G, h, row, col, value);
//		}
//	}
	
//	private void xxxupdateConstraintsActivity(IntMatrix1D c, IntMatrix2D G, IntMatrix1D h, int row, int col, int value) {
//		
//		if(this.L[146] > G.getQuick(146, 492)){
//			log.error("Attenzione!!!");
//		}
//		
//		int[] rowsSupportI = rowsSupport[row];
//		int LI = this.L[row];
//		int UI = this.U[row];
//		int hI = h.getQuick(row);
//
//		boolean allNotNegativeI = false;
//		boolean allNotPositiveI = false;
//		if (rowsSupportI.length > 0) {
//			allNotNegativeI = true;
//			allNotPositiveI = true;
//			for (int j : rowsSupportI) {
//				int GIJ = G.getQuick(row, j);
//				if(j == col){
//					if (GIJ > 0) {
//						// j in P
//						UI -= GIJ * value;
//					} else {
//						// j in M
// 						LI -= GIJ * value;
//					}
//					hI = h.getQuick(row) - G.getQuick(row, col) * value;
//				}else{
//					if (GIJ > 0) {
//						allNotPositiveI = false;
//					} else {
//						allNotNegativeI = false;
//					}
//				}
//			}
//			
//			//LI <= RHS
//			if (LI > hI) {
//				log.info("row="+row+", col="+col+", value="+value);
//				log.debug(INFEASIBLE_PROBLEM);
//				throw new RuntimeException(INFEASIBLE_PROBLEM);
//			}
//		}
//		
//		this.L[row] = LI;
//		this.U[row] = UI;
//		this.allNotNegative[row] = allNotNegativeI;
//		this.allNotPositive[row] = allNotPositiveI;
//		h.setQuick(row, hI);
//	}
	
	private boolean isSameSparsityPattern(int[] pattern1, int[] pattern2) {
		if (pattern1.length == pattern2.length) {
			for (int k = 0; k < pattern1.length; k++) {
				if (pattern1[k] != pattern2[k]) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * From the full x, gives back its presolved elements.
	 */
	public IntMatrix1D presolve(IntMatrix1D x) {
		if (x.size() != originalN) {
			throw new IllegalArgumentException("wrong array dimension: " + x.size());
		}
		IntMatrix1D presolvedX = x.copy();
		for (int i = 0; i < presolvingStack.size(); i++) {
			presolvingStack.get(i).preSolve(presolvedX);
		}
		int[] ret = new int[presolvedN];
		int cntPosition = 0;
		for (int i = 0; i < presolvedX.size(); i++) {
			if (indipendentVariables[i]) {
				ret[cntPosition] = presolvedX.getQuick(i);
				cntPosition++;
			}
		}
		return IntFactory1D.dense.make(ret);
	}
	
	public IntMatrix1D postsolve(IntMatrix1D x) {

		IntMatrix1D postsolvedX = IntFactory1D.dense.make(originalN);

		for (int i = 0; i < x.size(); i++) {
			postsolvedX.setQuick(presolvedColumns[i], x.getQuick(i));
		}
		for (int i = presolvingStack.size() - 1; i > -1; i--) {
			presolvingStack.get(i).postSolve(postsolvedX);
		}
		return postsolvedX;
	}

	public int getOriginalN() {
		return this.originalN;
	}

	public int getOriginalMeq() {
		return this.originalMieq;
	}

	public int getPresolvedN() {
		return this.presolvedN;
	}
	
	public int getPresolvedMieq() {
		return this.presolvedMieq;
	}
	
//	public int[] getPresolvedColumns() {
//		return this.presolvedColumns;
//	}
	
	public int[] getPresolvedRowsPos() {
		return this.presolvedRowsPos;
	}

	public IntMatrix1D getPresolvedC() {
		return this.presolvedC;
	}

	public IntMatrix2D getPresolvedG() {
		return this.presolvedG;
	}

	public IntMatrix1D getPresolvedH() {
		return this.presolvedH;
	}

	public void setRemoveDominatingColumns(boolean removeDominatingColumns) {
		this.removeDominatingColumns = removeDominatingColumns;
	}

	public void setCheckMinMaxFeasibility(boolean checkMinMaxFeasibility) {
		this.checkMinMaxFeasibility = checkMinMaxFeasibility;
	}

	public void setResolveConnectedComponents(boolean resolveConnectedComponents) {
		this.resolveConnectedComponents = resolveConnectedComponents;
	}

	public void setCheckProgress(boolean checkProgress) {
		this.checkProgress = checkProgress;
	}

}
