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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joptimizer.exception.InfeasibleProblemException;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.util.Utils;

import cern.colt.function.tint.IntIntIntFunction;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

public class BIPLokbaQuickPresolver {

	private BIPLokbaTable reducedLokbaTable;
	private int[] presolvedRowsPos;
	private boolean someReductionDone = false;
	private final Map<Integer, Integer> fixedVariablesMap = new HashMap<Integer, Integer>();
	private final List<Integer> obsoleteConstraintsList = new ArrayList<Integer>();
	private static Log log = LogFactory.getLog(BIPLokbaQuickPresolver.class.getName());

	public int getPresolvedN() {
		return reducedLokbaTable.getN();
	}

	public int[] getPresolvedRowsPos() {
		return this.presolvedRowsPos;
	}

	public IntMatrix1D postsolve(IntMatrix1D sol) {
		return sol;
	}

	public BIPLokbaTable getReducedLokbaTable() {
		return this.reducedLokbaTable;
	}

	public void presolve(BIPLokbaTable myLokbaTable) throws JOptimizerException {
		int iteration = 0;
		while (someReductionDone) {
			iteration++;
			someReductionDone = false;// reset

			if (log.isDebugEnabled()) {
				log.debug("iteration: " + iteration);
			}
			
			// remove obsolete constraints
			removeObsoleteConstraints(myLokbaTable);

			// remove strong variables
			removeStrongVariables(myLokbaTable);

			// remove firm variables
			removeFirmVariables(myLokbaTable);
		}
		
		int[] columnIndexes = new int[]{};
		Set<Integer> fixedVariablesPos = fixedVariablesMap.keySet();
		for(int i=0; i<myLokbaTable.getN(); i++){
			if(!fixedVariablesPos.contains(i)){
				columnIndexes = Utils.addToSortedArray(columnIndexes, i);
			}
		}
		int[] rowIndexes = new int[]{};
		presolvedRowsPos = new int[myLokbaTable.getMieq()];
		int cnt = 0;
		for(int i=0; i<myLokbaTable.getMieq(); i++){
			if(!obsoleteConstraintsList.contains(i)){
				rowIndexes = Utils.addToSortedArray(rowIndexes, i);
				presolvedRowsPos[i] = cnt;
				cnt++;
			}else{
				presolvedRowsPos[i] = -1;
			}
		}
		
		reducedLokbaTable = myLokbaTable.viewSelection(rowIndexes, columnIndexes);
	}

	/**
	 * Obsolete constraints (note that P is the RHS of a superior constraint after standardization)
	 */
	private void removeObsoleteConstraints(BIPLokbaTable myLokbaTable) {
		final IntMatrix1D P = myLokbaTable.getP();
		for (int i = 0; i < myLokbaTable.getMieq(); i++) {
			if (!(P.getQuick(i) > 0)) {
				obsoleteConstraintsList.add(i);
				someReductionDone = true;
			}
		}
	}

	/**
	 * Strong variables (note that S is the RHS of an inferior constraint after standardization)
	 */
	private void removeStrongVariables(BIPLokbaTable myLokbaTable) {
		final IntMatrix1D signs = myLokbaTable.getSigns();
		final IntMatrix2D G = myLokbaTable.getConstraints();
		final IntMatrix1D P = myLokbaTable.getP();
		final IntMatrix1D S = myLokbaTable.getS();
		G.forEachNonZero(new IntIntIntFunction() {
			public int apply(int i, int j, int gij) {
				// log.debug("i:" + i + ",j:" + j + ", gij=" + gij);
				if(!obsoleteConstraintsList.contains(i)){
					if(!fixedVariablesMap.keySet().contains(j)){
						if (Math.abs(gij) > S.getQuick(i)) {
							fixedVariablesMap.put(i, 1);//@TODO fix!!!!!!!
							someReductionDone = true;
						}
					}
				}
				return gij;
			}
		});
	}
	
	/**
	 * Firm variables (look for variables with SNCoV=0)
	 * @throws JOptimizerException 
	 */
	private void removeFirmVariables(BIPLokbaTable myLokbaTable) throws JOptimizerException {
		final IntMatrix1D signs = myLokbaTable.getSigns();
		final IntMatrix2D G = myLokbaTable.getConstraints();
		final IntMatrix1D P = myLokbaTable.getP();
		final IntMatrix1D S = myLokbaTable.getS();
		double[] CoCN = new double[myLokbaTable.getMieq()];
		for (int c = 0; c < myLokbaTable.getMieq(); c++) {
			if(!obsoleteConstraintsList.contains(c)){
				int Pc = P.getQuick(c);
				int Sc = S.getQuick(c);
				if (!(Pc > 0)) {
					throw new IllegalStateException("Not positive P at position " + c + ": " + Pc);
				}
				if (!(Sc > 0)) {
					log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
					throw new InfeasibleProblemException(InfeasibleProblemException.INFEASIBLE_PROBLEM);
				}
				CoCN[c] = ((double) Pc / Sc) / (Pc + Sc);
			}
		}

		double[] SNCoV = new double[myLokbaTable.getN()];
		for (int c = 0; c < myLokbaTable.getMieq(); c++) {
			if(!obsoleteConstraintsList.contains(c)){
				for (int i = 0; i < myLokbaTable.getN(); i++) {
					if(!fixedVariablesMap.keySet().contains(i)){
						int cCI = -1 * signs.getQuick(i) * G.getQuick(c, i);
						if (!(cCI > 0)) {
							SNCoV[i] -= cCI * CoCN[c];
						}
					}
				}
			}
		}
		
		for (int i = 0; i < myLokbaTable.getN(); i++) {
			if(!fixedVariablesMap.keySet().contains(i)){
				if (SNCoV[i] < 0) {
					throw new IllegalStateException("Negative SNCoV at position " + i + ": " + SNCoV[i]);
				} else if (SNCoV[i] == 0) {
					fixedVariablesMap.put(i, 1);//@TODO fix!!!!!!!
					someReductionDone = true;
				}
			}
		}
	}
	
}
