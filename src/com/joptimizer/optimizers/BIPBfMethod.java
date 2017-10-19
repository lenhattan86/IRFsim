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

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joptimizer.exception.InfeasibleProblemException;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.util.BinaryCombinationsLooping;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

/**
 * Brute Force solver for the problem:
 * min(c.x) s.t.
 * G.x <= h
 * A.x = b
 * x in {0,1}
 * 
 */
public class BIPBfMethod extends BIPOptimizationRequestHandler{
	
	private static Log log = LogFactory.getLog(BIPBfMethod.class.getName());
	
	@Override
	public void optimize() throws JOptimizerException {
		if(log.isDebugEnabled()){
			log.debug("optimize");
		}
		
		BIPOptimizationRequest bipRequest = getBIPOptimizationRequest();
		if(log.isDebugEnabled() && bipRequest.isDumpProblem()){
			log.debug("BIP problem: " + bipRequest.toString());
		}
		
		if(bipRequest.isPresolvingDisabled()){
			//optimization
//			BIPBfMethod opt = new BIPBfMethod();
//			opt.setBIPOptimizationRequest(bipRequest);
//			if(opt.optimizePresolvedBIP() == OptimizationResponse.FAILED){
//				return OptimizationResponse.FAILED;
//			}
//			bipResponse = opt.getBIPOptimizationResponse();
//			setBIPOptimizationResponse(bipResponse);
			
			this.optimizePresolvedBIP();
		}else{
			//presolving
			BIPPresolver bipPresolver = new BIPPresolver();
			bipPresolver.setRemoveDominatingColumns(bipRequest.isRemoveDominatingColumns());
			bipPresolver.setResolveConnectedComponents(bipRequest.isResolveConnectedComponents());
			final IntMatrix1D originalC = null; 
			final IntMatrix2D originalG = null;
			final IntMatrix1D originalH = null;
			bipPresolver.presolve(originalC, originalG, originalH);
			int presolvedDim = bipPresolver.getPresolvedN();
			BIPOptimizationResponse bipResponse;
			if(presolvedDim==0){
				//deterministic problem
				if(log.isDebugEnabled()){
					log.debug("presolvedDim : " + presolvedDim);
					log.debug("deterministic BIP problem");
				}
				bipResponse = new BIPOptimizationResponse();
				bipResponse.setSolution(new int[]{});
			}else{
				//solving the presolved problem
				IntMatrix1D presolvedC = bipPresolver.getPresolvedC();
				IntMatrix2D presolvedG = bipPresolver.getPresolvedG();
				IntMatrix1D presolvedH = bipPresolver.getPresolvedH();
				
				//new BIP problem (the presolved problem)
				BIPOptimizationRequest presolvedBIPRequest = bipRequest.cloneMe();
				presolvedBIPRequest.setC(presolvedC);
				presolvedBIPRequest.setG(presolvedG);
				presolvedBIPRequest.setH(presolvedH);
				
				//optimization
				BIPBfMethod opt = new BIPBfMethod();
				opt.setBIPOptimizationRequest(presolvedBIPRequest);
				opt.optimizePresolvedBIP();
				bipResponse = opt.getBIPOptimizationResponse();
			}
			
			//postsolving
			int[] postsolvedSolution = bipPresolver.postsolve(IntFactory1D.dense.make(bipResponse.getSolution())).toArray();
			bipResponse.setSolution(postsolvedSolution);
			setBIPOptimizationResponse(bipResponse);
		}
	}
	
	private void optimizePresolvedBIP() throws JOptimizerException{
		if(log.isDebugEnabled()){
			log.debug("optimizePresolvedBIP");
		}
		
		long tStart = System.currentTimeMillis();
		
		BIPOptimizationRequest bipRequest = getBIPOptimizationRequest();
		if(log.isDebugEnabled() && bipRequest.isDumpProblem()){
			log.debug("BIP problem: " + bipRequest.toString());
		}
		
		if(log.isDebugEnabled()){
			log.debug("dim : " + getDim());
			log.debug("mieq: " + getMieq());
		}
		
		int[] X = null;
		MyCombCallbackHandler cbHandler = new MyCombCallbackHandler();
		BinaryCombinationsLooping comb = new BinaryCombinationsLooping(getDim(), cbHandler);
		comb.doLoop();
		if(cbHandler.getNOfCombinations() != Math.pow(2, getDim())){
    			throw new IllegalStateException("unexpected number of combinations: " + cbHandler.getNOfCombinations() +"!=" + Math.pow(2, getDim()));
		}
		
		if(log.isDebugEnabled()){
			log.debug("nOfCombinations        : " + cbHandler.getNOfCombinations());
			log.debug("nOfFeasibleCombinations: " + cbHandler.getNOfFeasibleCombinations());
		}
		if(cbHandler.getNOfFeasibleCombinations() < 1){
			log.error(InfeasibleProblemException.INFEASIBLE_PROBLEM);
			X = new int[getDim()];
			Arrays.fill(X, -1);
			throw new InfeasibleProblemException(InfeasibleProblemException.INFEASIBLE_PROBLEM);
		}else{
			X = cbHandler.getSol();
		}
		
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

	private class MyCombCallbackHandler extends com.joptimizer.util.BinaryCombinationsLooping.CombCallbackHandler {
		private DoubleMatrix1D cDouble;
		private DoubleMatrix2D ADouble; 
		private DoubleMatrix1D bDouble; 
		private DoubleMatrix2D GDouble; 
		private DoubleMatrix1D hDouble;
		private IntMatrix1D cInt;
		private IntMatrix2D AInt; 
		private IntMatrix1D bInt; 
		private IntMatrix2D GInt; 
		private IntMatrix1D hInt;
		private int[] sol = new int[getDim()];
		private double value = Double.MAX_VALUE;
		private int nOfCombinations = 0;
		private int nOfFeasibleCombinations = 0;
		
		MyCombCallbackHandler(){
			if(getC() instanceof DoubleMatrix1D){
				cDouble = (DoubleMatrix1D) getC();
			}else{
				cInt = (IntMatrix1D) getC();
			}
			if(getA()!=null && getA() instanceof DoubleMatrix2D){
				ADouble = (DoubleMatrix2D) getA();
				bDouble = (DoubleMatrix1D) getB();
			}else{
				AInt = (IntMatrix2D) getA();
				bInt = (IntMatrix1D) getB();
			}
			if(getG()!=null && getG() instanceof DoubleMatrix2D){
				GDouble = (DoubleMatrix2D) getG();
				hDouble = (DoubleMatrix1D) getH();
			}else{
				GInt = (IntMatrix2D) getG();
				hInt = (IntMatrix1D) getH();
			}
		}
		
		@Override
		public void handle(int[] realization) {
			//log.debug("realization: " + ArrayUtils.toString(realization));
			int[] mySol = realization;
			nOfCombinations++;
			
			boolean isFeasible = true;
			
			// check equalities
			for (int c = 0; ADouble!=null && isFeasible && c < getMeq(); c++) {
				double myLimit = 0;
				for (int i = 0; i < getDim(); i++) {
					myLimit += ADouble.getQuick(c, i) * mySol[i];
				}
				isFeasible = myLimit == bDouble.getQuick(c);
			}
			
			// check equalities
			for (int c = 0; AInt!=null && isFeasible && c < getMeq(); c++) {
				int myLimit = 0;
				for (int i = 0; i < getDim(); i++) {
					myLimit += (AInt.getQuick(c, i) * mySol[i]);
				}
				isFeasible = myLimit == bInt.getQuick(c);
			}

			// check inequalities
			for (int c = 0; GDouble!=null && isFeasible && c < getMieq(); c++) {
				double myLimit = 0;
				for (int i = 0; i < getDim(); i++) {
					myLimit += GDouble.getQuick(c, i) * mySol[i];
				}
				isFeasible = myLimit <= hDouble.getQuick(c);
			}
			
			// check inequalities
			for (int c = 0; GInt!=null && isFeasible && c < getMieq(); c++) {
				int myLimit = 0;
				for (int i = 0; i < getDim(); i++) {
					myLimit += GInt.getQuick(c, i) * mySol[i];
				}
				isFeasible = myLimit <= hInt.getQuick(c);
			}
			
			if(isFeasible){
				nOfFeasibleCombinations++;
				double myValue = 0;
				for (int i = 0; cDouble!=null && i < getDim(); i++) {
					myValue += cDouble.getQuick(i) * mySol[i];
				}
				for (int i = 0; cInt!=null && i < getDim(); i++) {
					myValue += cInt.getQuick(i) * mySol[i];
				}
				
				//log.debug("myValue: " + myValue);
				if(myValue < value){
					System.arraycopy(mySol, 0, sol, 0, getDim());
					value = myValue;
					if(log.isDebugEnabled()){
						log.debug("new sol  : " + ArrayUtils.toString(sol));
						log.debug("new value: " + value);
					}
				}
			}
		}
		
		int[] getSol() {
			return sol;
		}

		int getNOfCombinations() {
			return nOfCombinations;
		}

		int getNOfFeasibleCombinations() {
			return nOfFeasibleCombinations;
		}
	}
		
}
