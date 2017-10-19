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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.function.tint.IntIntIntFunction;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

import com.joptimizer.exception.InfeasibleProblemException;
import com.joptimizer.exception.IterationsLimitException;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.util.Utils;

/**
 * Solves the problem 
 * 
 * <br>min(c.x) s.t.
 * <br>G.x <= h
 * <br>A.x = b
 * <br>x in {0,1}<sup>n</sup>
 * 
 * @see [1] S. H. Pakzad-Moghadam, M. S. Shahmohammadi, R. Ghodsi
 *      "A Low-Order Knowledge-Based Algorithm (LOKBA) to Solve Binary Integer Programming Problems"
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization", Eliminating linear equality constraints     
 * 
 * @TODO: i problemi con noLoop(forcedValue) sono gia presolti
 */
public class BIPLokbaTableMethod extends BIPOptimizationRequestHandler{
	
	public static final int DEFAULT_BRUTE_FORCE_THRESHOLD = 7;
	public static final int DEFAULT_FULL_PRESOLVING_FREQUENCY = 1;
	public static final int DEFAULT_FIND_BEST_SOLUTION_ATTEMPTS = -1;
	private String problemId = null;
	private BIPLokbaTable lokbaTable;
	private double[] FCo;
	private Map<Integer, int[]> solvedColumnsMap;
	private Map<Integer, int[]> solvedRowsMap;
	//private Map<Integer, int[]> choicesMap;
	private Map<Integer, Choices> choicesMap;
	private Choices forcedHeuristicChoice;
	private int solutionLevel;
	private IntMatrix1D lokbaSolution;
	private int[] allColumnIndexes;
	private int[] allRowIndexes;
	private int bruteForceThreshold;
	private int fullPresolvingFrequency;
	private int findBestSolutionAttempts;
	private Log log = LogFactory.getLog(this.getClass().getName());
	
	
//	private List<IntMatrix1D> solTestList = new ArrayList<IntMatrix1D>(); 
	
	
	public BIPLokbaTableMethod(){
		this(DEFAULT_BRUTE_FORCE_THRESHOLD, DEFAULT_FULL_PRESOLVING_FREQUENCY, DEFAULT_FIND_BEST_SOLUTION_ATTEMPTS);
	}
	
	public BIPLokbaTableMethod(int bruteForceThreshold, int fullPresolvingFrequency, int findBestSolutionAttempts){
		this.bruteForceThreshold = bruteForceThreshold;
		this.fullPresolvingFrequency = fullPresolvingFrequency;
		this.findBestSolutionAttempts =findBestSolutionAttempts;
	}
	
	@Override
	public void optimize() throws JOptimizerException {
		if(log.isDebugEnabled()){
			log.debug("optimize");
		}
		
		long tStart = System.currentTimeMillis();
		BIPOptimizationRequest bipRequest = getBIPOptimizationRequest();
		if(log.isDebugEnabled() && bipRequest.isDumpProblem()){
			log.debug("BIP problem: " + bipRequest.toString());
		}
		
		//initialization
		this.initLokbaElements(bipRequest);
		
		//solve
		heuristicSolve();
		findBestSolution();
		int[] X = getSolution();
		
		//build response
		BIPOptimizationResponse bipResponse = new BIPOptimizationResponse();
		bipResponse.setSolution(X);
		setBIPOptimizationResponse(bipResponse);
		
		long tStop = System.currentTimeMillis();
		if(log.isDebugEnabled()){
			log.debug("time: " + (tStop - tStart));
			log.debug("sol : " + ArrayUtils.toString(bipResponse.getSolution()));
			//log.debug("ret code: " + bipResponse.getReturnCode());
		}
	}
	
	private void heuristicSolve() throws JOptimizerException{
		
		BIPLokbaTable lokbaTableSelection = this.lokbaTable;
		IntMatrix1D lokbaSolutionSelection = this.lokbaSolution; 
		int[] columnIndexes = this.allColumnIndexes;
		int[] rowIndexes = this.allRowIndexes;
		IntMatrix1D newP = null, newS = null;
		int nOfSolvedColumns = 0;
		int nOfSolvedRows = 0;
		int iteration = 0;
		while (nOfSolvedColumns < getDim()) {
			if(log.isDebugEnabled()){
				log.debug("iteration    : " + iteration);
				log.debug("solutionLevel: " + solutionLevel);
			}
			iteration++;
			
			// iteration limit condition
			if (iteration == getMaxIteration() + 1) {
				//getBIPOptimizationResponse().setReturnCode(OptimizationResponse.FAILED);
				log.error(IterationsLimitException.MAX_ITERATIONS_EXCEEDED);
				throw new IterationsLimitException(IterationsLimitException.MAX_ITERATIONS_EXCEEDED);
			}
			
//			if(solutionLevel == 375){
//				//log.info("lokbaSolution: " + ArrayUtils.toString(lokbaSolution.toArray()));
//				IntMatrix1D newSol = lokbaSolution.copy();
//				boolean allDifferent = true;
//				for(int i=0; allDifferent && i<solTestList.size(); i++){
//					IntMatrix1D solI = solTestList.get(i);
//					boolean areEqualsI = true;
//					for(int j=0; areEqualsI && j<lokbaSolution.size(); j++){
//						areEqualsI = Utils.isZero(solI.getQuick(j)  - newSol.getQuick(j));
//					}
//					allDifferent = !areEqualsI;
//				}
//				if(allDifferent){
//					solTestList.add(solTestList.size(), newSol);
//				}else{
//					throw new RuntimeException("INTORTATO!!!!!!");
//				}
//			}
			
			//solve the problem: can return one or multiple positions
			int[][] solvedColumnsAndRows = heuristicSolveSelection(lokbaTableSelection, lokbaSolutionSelection);
			int[] solvedColumns = null;
			int[] solvedRows = null;
			if(solvedColumnsAndRows != null){
			  //check quick feasibility
				IntMatrix1D[] newPAndS = calculatePAndS(lokbaTableSelection, lokbaSolutionSelection);
				newP = newPAndS[0];
				newS = newPAndS[1];
				if (checkQuickFeasibility(newP, newS) < 0) {
					// revert
					for (int i = 0; i < solvedColumnsAndRows[0].length; i++) {
						lokbaSolutionSelection.setQuick(i, -1);
					}
					// invalidate
					solvedColumns = null;
				}
				solvedColumns = solvedColumnsAndRows[0];
				solvedRows = solvedColumnsAndRows[1];
			}
			
			if(solvedColumns == null){
				if(log.isDebugEnabled()){
					log.debug("choicesMap: ");
					for(Integer key : choicesMap.keySet()){
						log.debug("solutionLevel=" + key + ", " + choicesMap.get(key));
					}
				}
				//choicesMap.remove(solutionLevel);
				choicesMap.remove(solutionLevel);
				Choices previousChoices = null;
				for(int level = solutionLevel - 1; previousChoices == null && level > -1; level--){
					int[] choices = choicesMap.get(level).values;
					if(choices.length == 1){
						//we have 1 previous choice
						previousChoices = choicesMap.get(level); 
					} else {
						//choicesMap.remove(level);
						choicesMap.remove(level);
					}
					solutionLevel--;
				}
				if(previousChoices != null){
					//there is the possibility that the infeasibility comes from a wrong choice, so we try to change the previous choice
					//restore the previous solution
					if(log.isDebugEnabled()){
						log.debug("solvedColumnsMap: ");
						for(Integer key : solvedColumnsMap.keySet()){
							log.debug("level=" + key + ", columns=" + ArrayUtils.toString(solvedColumnsMap.get(key)));
						}
						log.debug("solvedRowsMap: ");
						for(Integer key : solvedRowsMap.keySet()){
							log.debug("level=" + key + ", rows=" + ArrayUtils.toString(solvedRowsMap.get(key)));
						}
					}
					int[] allPreviousColumnIndexes = this.allColumnIndexes;
					int[] allPreviousRowIndexes = this.allRowIndexes;
					for (int level = 0; level < solutionLevel; level++) {
						int[] solvedColumnsIter = solvedColumnsMap.get(level);
						for (int c = 0; c < solvedColumnsIter.length; c++) {
							allPreviousColumnIndexes = ArrayUtils.remove(allPreviousColumnIndexes, solvedColumnsIter[c] - c);//-k because the array is shrinking
						}
						int[] solvedRowsIter = solvedRowsMap.get(level);
						for (int r = 0; r < solvedRowsIter.length; r++) {
							allPreviousRowIndexes = ArrayUtils.remove(allPreviousRowIndexes, solvedRowsIter[r] - r);//-k because the array is shrinking
						}
					}
					//revert the solution
					columnIndexes = allPreviousColumnIndexes;
					rowIndexes = allPreviousRowIndexes;
					nOfSolvedColumns = getDim();
					nOfSolvedRows = getMieq();
					//lokbaTableSelection = this.originalLokbaTable.viewColumnSelection(allPreviousColumnIndexes);
					lokbaTableSelection = this.lokbaTable.viewSelection(allPreviousRowIndexes, allPreviousColumnIndexes);
					lokbaSolutionSelection = this.lokbaSolution.viewSelection(allPreviousColumnIndexes);
					for (int i = 0; i < lokbaSolutionSelection.size(); i++) {
						lokbaSolutionSelection.setQuick(i, -1);
						nOfSolvedColumns--;
					}
					IntMatrix1D[] newPAndS = calculatePAndS(lokbaTable, lokbaSolution);//calculate full P and S for the last feasible solution
					newP = newPAndS[0];
					newS = newPAndS[1];
					updatePAndS(lokbaTableSelection, newP.viewSelection(allPreviousRowIndexes), newS.viewSelection(allPreviousRowIndexes));
					
					//fix the choice and try again
					forcedHeuristicChoice = new Choices(previousChoices.position);
					forcedHeuristicChoice.values = ArrayUtils.add(forcedHeuristicChoice.values, 1 - previousChoices.values[0]);
					if(log.isDebugEnabled()){
						log.debug("forcedChoice: " + forcedHeuristicChoice);
					}
				}else{
					//nothing to try
					//break;
					log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
					throw new InfeasibleProblemException(InfeasibleProblemException.INFEASIBLE_PROBLEM);
				}
			}else{
				solvedColumnsMap.put(solutionLevel, solvedColumns);
				solvedRowsMap.put(solutionLevel, solvedRows);
				if(log.isDebugEnabled()){
					log.debug("solvedColumns: " + ArrayUtils.toString(solvedColumns));
					log.debug("solvedRows   : " + ArrayUtils.toString(solvedRows));
				}
				for (int i = 0; i < solvedColumns.length; i++) {
					//columnIndexes = Utils.removeFromSortedArray(columnIndexes, solvedPositions[i]);
					columnIndexes = ArrayUtils.remove(columnIndexes, solvedColumns[i] - i);//-i because the array is shrinking
					nOfSolvedColumns ++;
				}
				for (int i = 0; i < solvedRows.length; i++) {
					rowIndexes = ArrayUtils.remove(rowIndexes, solvedRows[i] - i);//-i because the array is shrinking
					nOfSolvedRows ++;
				}
				
				//for P and S the selection is not rowIndexes, because the selection 
				//cannot be applied to the original P and S (they change during iterations) 
				int[] newPAndSRowIndexes = new int[] {};
				for (int r = 0; r < newP.size(); r++) {
					if (!Utils.isInSortedArray(solvedRows, r)) {
						newPAndSRowIndexes = Utils.addToSortedArray(newPAndSRowIndexes, r);
					}
				}
				
				//update and go on
				forcedHeuristicChoice = null;
				//lokbaTableSelection = this.originalLokbaTable.viewColumnSelection(columnIndexes);
				lokbaTableSelection = this.lokbaTable.viewSelection(rowIndexes, columnIndexes);
				lokbaSolutionSelection = this.lokbaSolution.viewSelection(columnIndexes);
				updatePAndS(lokbaTableSelection, newP.viewSelection(newPAndSRowIndexes), newS.viewSelection(newPAndSRowIndexes));
				solutionLevel++;
			}
		}
	}
	
	/**
	 * @return the matrix of {solved columns array, solved rows array} 
	 * @throws JOptimizerException 
	 */
	private int[][] heuristicSolveSelection(BIPLokbaTable lokbaTableSelection, IntMatrix1D lokbaSolutionSelection) throws JOptimizerException{
		
		if(log.isDebugEnabled()){
			log.debug("lokbaTableSelection: " + lokbaTableSelection);
		}
		
		int presolvedDim = -1;
		BIPPresolver bipPresolver = getPresolver(solutionLevel);
		try{
			presolve(bipPresolver, lokbaTableSelection);
			presolvedDim = getPresolvedDim(bipPresolver, lokbaTableSelection);
		}catch(InfeasibleProblemException e){
			if(log.isInfoEnabled()){
				log.info(e.getMessage());
			}
			return null;
		}
		
		IntMatrix1D tempSolution = null;
		if(log.isInfoEnabled()){
			log.info("presolvedDim: " + presolvedDim);
		}
		if (presolvedDim == 0) {
			//deterministic problem
			if(log.isDebugEnabled()){
				log.debug("deterministic BIP problem");
			}
			tempSolution = calculateReciprocal(getPostsolvedVector(bipPresolver, IntFactory1D.dense.make(0)), lokbaTableSelection);
		}else{
			//solving the presolved problem
			BIPLokbaTable presolvedLokbaTable = getPresolvedLokbaTable(bipPresolver);
			IntMatrix1D presolvedLokbaSolution = null;
			if(presolvedDim < this.bruteForceThreshold){
				//brute force solution
				if(log.isDebugEnabled()){
					log.debug("brute force solution");	
				}
				Object[] cGh = presolvedLokbaTable.asStandardForm();
				IntMatrix1D presolvedC = (IntMatrix1D) cGh[0];
				IntMatrix2D presolvedG = (IntMatrix2D) cGh[1];
				IntMatrix1D presolvedH = (IntMatrix1D) cGh[2];
				
				BIPOptimizationRequest or = new BIPOptimizationRequest();
				or.setC(presolvedC);
				or.setG(presolvedG);
				or.setH(presolvedH);
				or.setDumpProblem(false);
				or.setPresolvingDisabled(true);
				
				BIPBfMethod opt = new BIPBfMethod();
				opt.setBIPOptimizationRequest(or);
				try{
					//optimization
					opt.optimize();
				}catch(InfeasibleProblemException e){
					if(log.isInfoEnabled()){
						log.info(e.getMessage());
					}
					return null;
				}
				BIPOptimizationResponse response = opt.getBIPOptimizationResponse();
				int[] sol = response.getSolution();
				presolvedLokbaSolution = calculateReciprocal(IntFactory1D.dense.make(sol), presolvedLokbaTable);			
			}else{
				presolvedLokbaSolution = IntFactory1D.dense.make(presolvedDim, -1);
				makeHeuristicChoice(presolvedLokbaTable, presolvedLokbaSolution);
			}
			//postsolving
			IntMatrix1D presolvedSolution = calculateReciprocal(presolvedLokbaSolution, presolvedLokbaTable);
			tempSolution = calculateReciprocal(getPostsolvedVector(bipPresolver, presolvedSolution), lokbaTableSelection);
		}
		
		int[] solvedColumns = new int[] {};
		for (int i = 0; i < tempSolution.size(); i++) {
			int tsI = tempSolution.getQuick(i);
			if (tsI > -1) {
				lokbaSolutionSelection.setQuick(i, tsI);
				solvedColumns = Utils.addToSortedArray(solvedColumns, i);
			}
		}
		
		//log.info("presolvedRowsPos: " + ArrayUtils.toString(bipPresolver.getPresolvedRowsPos()));
		int[] presolvedRowsPos = getPresolvedRowsPos(bipPresolver);
		int[] solvedRows = new int[] {};
		for (int i = 0; i < presolvedRowsPos.length; i++) {
			int prI = presolvedRowsPos[i];
			if (prI < 0) {
				solvedRows = Utils.addToSortedArray(solvedRows, i);
			}
		}
		
		return new int[][] { solvedColumns, solvedRows };
	}
	
	private void makeHeuristicChoice(BIPLokbaTable myLokbaTable, IntMatrix1D myLokbaSolution){
		if(log.isDebugEnabled()){
			log.debug("makeHeuristicChoice()");
			//log.debug(lokbaTable.toString());
		}
		
		int choicePosition = -1;
		int choiceValue = -1;
		if(this.forcedHeuristicChoice != null){
			if(log.isDebugEnabled()){
				log.debug("forced heuristicChoice (" + forcedHeuristicChoice + ") for problem " + problemId);
			}
			choicePosition = forcedHeuristicChoice.position;
			choiceValue = forcedHeuristicChoice.values[0];
		}else{
			final IntMatrix1D mySigns = myLokbaTable.getSigns();
			final IntMatrix2D myG = myLokbaTable.getConstraints();
			final IntMatrix1D myP = myLokbaTable.getP();
			final IntMatrix1D myS = myLokbaTable.getS();
			
			final double[] CoCN = new double[myLokbaTable.getMieq()];
			myG.forEachNonZero(new IntIntIntFunction() {
				public int apply(int i, int j, int gij) {
					// log.debug("i:" + i + ",j:" + j + ", gij=" + gij);
					int Pi = myP.getQuick(i);
					int Si = myS.getQuick(i);
					if(!(Pi > 0)){
						throw new IllegalStateException("Not positive P at position " + i + ": " + Pi);
					}
					if(!(Si > 0)){
						throw new IllegalStateException("Not positive S at position " + i + ": " + Si);
					}
					CoCN[i] = ((double) Pi / Si) / (Pi + Si);
					return gij;
				}
			});
			if (log.isDebugEnabled()) {
				log.debug("CoCN : " + ArrayUtils.toString(CoCN));
			}
			
			final double[] SPCoV = new double[myLokbaTable.getN()];
			final double[] SNCoV = new double[myLokbaTable.getN()];
			myG.forEachNonZero(new IntIntIntFunction() {
				public int apply(int i, int j, int gij) {
					// log.debug("i:" + i + ",j:" + j + ", gij=" + gij);
					int cCI = -1 * mySigns.getQuick(j) * myG.getQuick(i, j);
					if (cCI > 0) {
						SPCoV[j] += cCI * CoCN[i];
					} else {
						SNCoV[j] -= cCI * CoCN[i];
					}
					return gij;
				}
			});
			if (log.isDebugEnabled()) {
				log.debug("SPCoV : " + ArrayUtils.toString(SPCoV));
				log.debug("SNCoV : " + ArrayUtils.toString(SNCoV));
			}
			
			//check: FCo denominator is SNCoV
			for (int i = 0; i < myLokbaTable.getN(); i++) {
				if(!(SNCoV[i] > 0)){
					throw new IllegalStateException("Not positive SNCoV at position " + i + ": " + SNCoV[i]);
				}
			}

			this.FCo = new double[myLokbaTable.getN()];
			DoubleMatrix1D ofAbs = DoubleFactory1D.dense.make((int) myLokbaTable.getOf().size());
			for (int j = 0; j < ofAbs.size(); j++) {
				ofAbs.setQuick(j, Math.abs(myLokbaTable.getOf().getQuick(j)));
			}
			double[] normalizedSPCoV = new double[myLokbaTable.getN()];
			double[] normalizedSNCoV = new double[myLokbaTable.getN()];
			double[] normalizedOf = new double[myLokbaTable.getN()];
			int sumOf = 0;
			for (int i = 0; i < myLokbaTable.getN(); i++) {
				normalizedSPCoV[i] = SPCoV[i] / myLokbaTable.getMieq();
				normalizedSNCoV[i] = SNCoV[i] / myLokbaTable.getMieq();
				sumOf += ofAbs.getQuick(i);
			}
			if (log.isDebugEnabled()) {
				log.debug("normalizedSPCoV : " + ArrayUtils.toString(normalizedSPCoV));
				log.debug("normalizedSNCoV : " + ArrayUtils.toString(normalizedSNCoV));
			}
			
			if(!Utils.isZero(sumOf)){
				for (int i = 0; i < myLokbaTable.getN(); i++) {
					normalizedOf[i] = (ofAbs.getQuick(i)) / sumOf;
					double FCoI = (normalizedSPCoV[i] + normalizedOf[i]) / normalizedSNCoV[i];
					if (FCoI < 0) {
						throw new IllegalStateException("Negative FCo at position "	+ i + ": " + FCoI);
					}
					this.FCo[i] = FCoI;
				}
			}else{
				for (int i = 0; i < myLokbaTable.getN(); i++) {
					double FCoI = (normalizedSPCoV[i]) / normalizedSNCoV[i];
					if (FCoI < 0) {
						throw new IllegalStateException("Negative FCo at position "	+ i + ": " + FCoI);
					}
					this.FCo[i] = FCoI;
				}
			}
			
			if (log.isDebugEnabled()) {
				log.debug("normalizedOf : " + ArrayUtils.toString(normalizedOf));
			}
			
			if (log.isDebugEnabled()) {
				log.debug("FCo: " + ArrayUtils.toString(FCo));
			}
			
			int minFCoPos = -1;
			int maxFCoPos = -1;
			double minFCo = Double.MAX_VALUE;
			double maxFCo = -Double.MAX_VALUE;
			for (int i = 0; i < myLokbaTable.getN(); i++) {
				double FCoI = this.FCo[i];
				// double FCoI = this.FCoNof[i];
				if (FCoI < minFCo) {
					minFCoPos = i;
					minFCo = FCoI;
				}
				if (FCoI > maxFCo) {
					maxFCoPos = i;
					maxFCo = FCoI;
				}
			}
			
			if(minFCo * maxFCo > 1){
				//assign the value 1 to the variable that has this max FCo
				choiceValue = (mySigns.getQuick(maxFCoPos) > 0) ? 1 : 1;//1:0
				choicePosition = maxFCoPos;
			}else{
				//assign 0 to the variable with min FCo
				choiceValue = (mySigns.getQuick(minFCoPos) > 0) ? 0 : 0;//0:1
				choicePosition = minFCoPos;
			}
		}
		
		if(log.isDebugEnabled()){
			log.debug("assigned value " + choiceValue + " at position " + choicePosition + " for level " + solutionLevel);
		}
		
		// addToChoicesMap(fixedValue);
		addToChoicesMap(choicePosition, choiceValue);
		myLokbaSolution.setQuick(choicePosition, choiceValue);
	}
	
	private void findBestSolution() throws JOptimizerException {
		try {
			
			BIPLokbaTable currentLokbaTable = this.lokbaTable;
			int[] currentSolution = getSolution();
			for (int attempt = 1; attempt < findBestSolutionAttempts + 1; attempt++) {
				int currentValue = currentLokbaTable.getOf().zDotProduct(IntFactory1D.dense.make(currentSolution));
				
				if(log.isInfoEnabled()){
					log.info("findBestSolution attempt          : " + attempt);
					log.info("findBestSolution current min value: " + currentValue);
				}
				
				// create new constraint
				int[][] addG = new int[1][getDim()];
				for (int i = 0; i < getDim(); i++) {
					addG[0][i] = currentLokbaTable.getOf().getQuick(i);
				}
				int[] addH = new int[] { currentValue - 1 };
				
				//create new problem with the new constraint
				BIPLokbaTable newLokbaTable = currentLokbaTable.copyWithAdditionalLEConstraints(addG, addH);
				
				Object[] cGH = newLokbaTable.asStandardForm();
				BIPOptimizationRequest newOr = getBIPOptimizationRequest().cloneMe();
				newOr.setC((IntMatrix1D) cGH[0]);
				newOr.setG((IntMatrix2D) cGH[1]);
				newOr.setH((IntMatrix1D) cGH[2]);
				
				//optimization
				BIPLokbaTableMethod newSolver = new BIPLokbaTableMethod(this.bruteForceThreshold, this.fullPresolvingFrequency, -1);
				newSolver.setBIPOptimizationRequest(newOr);
				newSolver.optimize();

//				// solve
//				BIPLokbaTableMethod newSolver = new BIPLokbaTableMethod(this.bruteForceThreshold, this.fullPresolvingFrequency, DEFAULT_FIND_BEST_SOLUTION_ATTEMPTS);
//				newSolver.heuristicSolve();
				
				//update final solution
				this.lokbaSolution = newSolver.lokbaSolution;
				
				//update iteration elements
				currentLokbaTable = newLokbaTable;
				currentSolution = newSolver.getSolution();
			}
		} catch(InfeasibleProblemException e){
			//stop the search of a better solution
			return;
		}
	}
	
	private BIPPresolver getPresolver(int solutionLevel) {
		BIPPresolver bipPresolver = new BIPPresolver();
		// full presolving is heavy for large dimension
		//boolean fullPresolvingCondition1 = (getDim() - solutionLevel) < 1000; 
		boolean fullPresolvingCondition1 = true;
		boolean fullPresolvingCondition2 = (solutionLevel % fullPresolvingFrequency == 0);
		if (fullPresolvingCondition1 && fullPresolvingCondition2) {
			if(log.isDebugEnabled()){
				log.debug("FULL presolving for level " + solutionLevel);
			}
			bipPresolver.setRemoveDominatingColumns(true);
			bipPresolver.setCheckMinMaxFeasibility(true);
		}else{
			bipPresolver.setRemoveDominatingColumns(false);
			bipPresolver.setCheckMinMaxFeasibility(false);
		}
		return bipPresolver;
	}

	private void presolve(BIPPresolver bipPresolver,	BIPLokbaTable myLokbaTable) throws JOptimizerException{
		Object[] cGh = myLokbaTable.asStandardForm();
		IntMatrix1D cOriginal = (IntMatrix1D) cGh[0];
		IntMatrix2D GOriginal = (IntMatrix2D) cGh[1];
		IntMatrix1D hOriginal = (IntMatrix1D) cGh[2];
		bipPresolver.presolve(cOriginal, GOriginal, hOriginal);
	}
	
	private int getPresolvedDim(BIPPresolver bipPresolver, BIPLokbaTable myLokbaTable){
		return bipPresolver.getPresolvedN();
	}
	
	private BIPLokbaTable getPresolvedLokbaTable(BIPPresolver bipPresolver){
		IntMatrix1D presolvedC = bipPresolver.getPresolvedC();
		IntMatrix2D presolvedG = bipPresolver.getPresolvedG();
		IntMatrix1D presolvedH = bipPresolver.getPresolvedH();
		BIPLokbaTable.toLokbaTable(presolvedC, presolvedG, presolvedH);
		BIPLokbaTable reducedLokbaTable = BIPLokbaTable.toLokbaTable(presolvedC, presolvedG, presolvedH);
		return reducedLokbaTable;
	}
	
	private IntMatrix1D getPostsolvedVector(BIPPresolver bipPresolver, IntMatrix1D sol){
		return bipPresolver.postsolve(sol);
	}
	
	private int[] getPresolvedRowsPos(BIPPresolver bipPresolver){
		return bipPresolver.getPresolvedRowsPos();
	}

	/**
	 * Once the P for all constraints (the vector of P) are non-positive 
	 * then the problem is fully feasible. On the other hand, if one element 
	 * of the vector S is negative, then the problem is infeasible. 
	 * There will be no such case that the last 2 statements (all P’s non-positive 
	 * and at least one S negative) occur simultaneously.
	 * NOTE: this check does not tell nothing about other cases of fully infeasibility.
	 * 
	 * @return 1 for feasible, -1 for infeasible, 0 otherwise 
	 */
	private int checkQuickFeasibility(IntMatrix1D myP, IntMatrix1D myS) {
		boolean isFeasible = true;
		for (int c = 0; isFeasible && c < myP.size(); c++) {
			isFeasible = !(myP.getQuick(c) > 0);
		}
		if (isFeasible) {
			return 1;
		}

		isFeasible = true;
		for (int c = 0; isFeasible && c < myS.size(); c++) {
			isFeasible = !(myS.getQuick(c) < 0);
		}
		if (!isFeasible) {
			return -1;
		}

		return 0;
	}
	
	/**
	 * Note that this a fixed value for a variable x in mySolution has the effect of changing the original values of h 
	 * in the following cases:
	 * <li>sign(x) = +1 and value = 1
	 * <li>sign(x) = -1 and value = 0 
	 */
	private IntMatrix1D[] calculatePAndS(final BIPLokbaTable myLokbaTable, final IntMatrix1D mySolution){
		int myN = myLokbaTable.getN();
		int myMieq = myLokbaTable.getMieq();
		
		if(myN != mySolution.size()){
			throw new IllegalStateException("Unexpected dimensions: " + myN +"!=" + mySolution.size());
		}
		
		IntMatrix2D myConstr = myLokbaTable.getConstraints();
		final IntMatrix1D myP = myLokbaTable.getP().copy();
		final IntMatrix1D myS = myLokbaTable.getS().copy();
		
		//When a variable is assigned to 1, 
		//the positive factors are deducted from its P and the negative factors are added to its S. 
		//When a variable is assigned to 0, 
		//the positive factors are deducted from its S and the negative factors are added to its P.
		
		//for constraint of type smaller-equal we have:
		//P = (Sum of the positive factors) - (RHS)
		//S = (RHS) - (Sum of the negative factors)
		/*
		for (int c = 0; c < myMieq; c++) {
			for (int i = 0; i < myN; i++) {
				int cCI = myConstr.getQuick(c, i);
				if(!Utils.isZero(cCI)){
					int mySolutionI = mySolution.getQuick(i);
					if(mySolutionI > -1){
						int effectiveValue = (myLokbaTable.getSigns().getQuick(i) > 0) ? mySolutionI : (1 - mySolutionI);
						if(effectiveValue == 0){
							if (cCI > 0) {
								myP.setQuick(c, myP.getQuick(c) - cCI);
								//S unchanged
							}else{
								//P unchanged
								myS.setQuick(c, myS.getQuick(c) + cCI);
							}
						}
						if(effectiveValue == 1){
							if (cCI > 0) {
								//P unchanged
								myS.setQuick(c, myS.getQuick(c) - cCI);
							}else{
								myP.setQuick(c, myP.getQuick(c) + cCI);
								//S unchanged
							}
						}
					}
				}				
			}
		}*/
		
		
		myConstr.forEachNonZero(new IntIntIntFunction() {
			public int apply(int c, int i, int cCI) {
				int mySolutionI = mySolution.getQuick(i);
				if(mySolutionI > -1){
					int effectiveValue = (myLokbaTable.getSigns().getQuick(i) > 0) ? mySolutionI : (1 - mySolutionI);
					if(effectiveValue == 0){
						if (cCI > 0) {
							myP.setQuick(c, myP.getQuick(c) - cCI);
							//S unchanged
						}else{
							//P unchanged
							myS.setQuick(c, myS.getQuick(c) + cCI);
						}
					}
					if(effectiveValue == 1){
						if (cCI > 0) {
							//P unchanged
							myS.setQuick(c, myS.getQuick(c) - cCI);
						}else{
							myP.setQuick(c, myP.getQuick(c) + cCI);
							//S unchanged
						}
					}
				}
				return cCI;
			}
		});
		
		
		
		
		
		return new IntMatrix1D[]{myP, myS};
	}
	
	private void updatePAndS(BIPLokbaTable myLokbaTable, IntMatrix1D myNewP, IntMatrix1D myNewS) {
		
		int myMieq = myLokbaTable.getMieq();

		if (myMieq != myNewP.size()) {
			throw new IllegalStateException("Unexpected dimensions: " + myMieq + "!=" + myNewP.size());
		}
		if (myMieq != myNewS.size()) {
			throw new IllegalStateException("Unexpected dimensions: " + myMieq + "!=" + myNewS.size());
		}

		IntMatrix1D myP = myLokbaTable.getP().copy();
		IntMatrix1D myS = myLokbaTable.getS().copy();
		for (int c = 0; c < myMieq; c++) {
			myP.setQuick(c, myNewP.getQuick(c));
			myS.setQuick(c, myNewS.getQuick(c));
		}
		myLokbaTable.setP(myP);
		myLokbaTable.setS(myS);
	}
	
	//end of inner class
	
	@Override
	public String toString() {
		Object[] cGh = lokbaTable.asStandardForm();
		IntMatrix1D c = (IntMatrix1D) cGh[0];
		IntMatrix2D G = (IntMatrix2D) cGh[1];
		IntMatrix1D h = (IntMatrix1D) cGh[2];

		StringBuffer sb = new StringBuffer("BIP problem "+ problemId +":\n");
		sb.append("min(c.x) s.t\n");
		sb.append("G.x <= h\n");
		sb.append("where\n");
		sb.append("c=" + ArrayUtils.toString(c.toArray()) + "\n");
		sb.append("G=" + ArrayUtils.toString(G.toArray()) + "\n");
		sb.append("h=" + ArrayUtils.toString(h.toArray()) + "\n");

		return sb.toString();
	}
	
	private int[] getSolution() {
		return calculateReciprocal(this.getLokbaSolution(), this.lokbaTable).toArray();
	}
	
	private IntMatrix1D getLokbaSolution() {
		return getLokbaSolution(true);
	}

	private IntMatrix1D getLokbaSolution(boolean checkValidity) {
		if(checkValidity){
			for (int i = 0; i < lokbaSolution.size(); i++) {
				if (lokbaSolution.getQuick(i) < 0) {
					return null;
				}
			}
		}
		return lokbaSolution;
	}
	
	private IntMatrix1D calculateReciprocal(IntMatrix1D mySolution, BIPLokbaTable myLokbaTable){
		int myN = myLokbaTable.getN();
		if (myN != mySolution.size()) {
			throw new IllegalStateException("Unexpected length: " + myN + "!="	+ mySolution.size());
		}
		IntMatrix1D ret = IntFactory1D.dense.make(myN);
		IntMatrix1D signs = myLokbaTable.getSigns();
		for (int i = 0; i < myN; i++) {
			int solutionI = mySolution.getQuick(i);
			if(solutionI < 0){
				ret.setQuick(i, -1);
			}else{
				ret.setQuick(i, (signs.getQuick(i) < 0) ? (1 - solutionI) : solutionI);
			}
		}
		return ret;
	}
	
//	private void addToChoicesMap(int value) {
//		int[] choices = choicesMap.get(solutionLevel);
//		if (choices == null) {
//			choices = new int[] { value };
//		} else {
//			choices = ArrayUtils.add(choices, value);
//		}
//		choicesMap.put(solutionLevel, choices);
//	}
	
	private void addToChoicesMap(int position, int value) {
		Choices choices = choicesMap.get(solutionLevel);
		if (choices == null) {
			choices = new Choices(position);
			choicesMap.put(solutionLevel, choices);
		} 
		if(position != choices.position){
			throw new IllegalStateException("Unexpected choices position");
		}
		choices.values = ArrayUtils.add(choices.values, value);
	}
	
	private static class Choices {
		int position;
		int[] values;
		Choices(int position) {
			this.position = position;
			this.values = new int[] {};
		}
		@Override
		public String toString(){
			return "Choices=[position=" + position + ", values=" + ArrayUtils.toString(values) + "]";
		}
	}
	
	private BIPLokbaTable buildLokbaTable(BIPOptimizationRequest bipRequest) {
		BIPStandardConverter bsc = new BIPStandardConverter();
		bsc.toStandardForm(bipRequest.getC(), bipRequest.getG(), bipRequest.getH(), bipRequest.getA(), bipRequest.getB());
		IntMatrix1D standardC = bsc.getStandardC(); 
		IntMatrix2D standardG = bsc.getStandardG(); 
		IntMatrix1D standardH = bsc.getStandardH();
		
		BIPLokbaTable lokbaTable = BIPLokbaTable.toLokbaTable(standardC, standardG, standardH);
		return lokbaTable;
	}
	
	private void initLokbaElements(BIPOptimizationRequest bipRequest) {
		this.problemId = Utils.createID();
		this.lokbaTable = buildLokbaTable(bipRequest);
		this.lokbaSolution = IntFactory1D.dense.make(getDim(), -1);
		this.allColumnIndexes = new int[getDim()];
		Utils.incrementalFill(allColumnIndexes);
		this.allRowIndexes = new int[getMieq()];
		Utils.incrementalFill(allRowIndexes);
		this.solvedColumnsMap = new HashMap<Integer, int[]>();
		this.solvedRowsMap = new HashMap<Integer, int[]>();
		this.choicesMap = new HashMap<Integer, Choices>();
		this.solutionLevel = 0;
		if(log.isDebugEnabled()){
			log.debug("initialized problem " + problemId + " with dim=" + getDim() + " and mieq=" + getMieq());
		}
	}
	
	@Override
	protected int getMieq() {
		return this.lokbaTable.getMieq();
	}
	
	@Override
	protected int getMeq() {
		return 0;
	}

}
