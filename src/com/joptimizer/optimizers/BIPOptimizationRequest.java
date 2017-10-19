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

import com.joptimizer.util.ColtUtils;

import cern.colt.matrix.AbstractMatrix1D;
import cern.colt.matrix.AbstractMatrix2D;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

/**
 * Binary Integer Programming optimization problem.
 * The problem is:
 * 
 * </br>min(c) s.t. 
 * </br>G.x <= h 
 * </br>A.x = b 
 * </br>x binary 
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class BIPOptimizationRequest {
	
	/**
	 * Linear objective function.
	 */
	private AbstractMatrix1D c;
	
	/**
	 * Linear equalities constraints matrix.
	 */
	private AbstractMatrix2D A;
	
	/**
	 * Linear equalities constraints coefficients.
	 */
	private AbstractMatrix1D b;
	
	/**
	 * Linear inequalities constraints matrix.
	 */
	private AbstractMatrix2D G;
	
	/**
	 * Linear inequalities constraints coefficients.
	 */
	private AbstractMatrix1D h;
	
	/**
	 * Dump the problem to the log file?
	 */
	private boolean dumpProblem = false;

	/**
	 * Maximum number of iteration in the search algorithm.
	 * Not mandatory, default is provided.
	 */
	private int maxIteration = 1000000;
	
	/**
	 * Should LP presolving be disabled?
	 */
	private boolean presolvingDisabled = false;
	
	/**
	 * If false, no dominating column reduction will be applied.
	 */
	private boolean removeDominatingColumns = true;
	
	/**
	 * If false, no check on min-max feasibility will be applied.
	 */
	private boolean checkMinMaxFeasibility = true;
	
	/**
	 * If false, no connected components resolution will be applied.
	 */
	private boolean resolveConnectedComponents = false;
	
	public AbstractMatrix1D getC() {
		return c;
	}

	public void setC(double[] c) {
		if(c!=null){
			setC(DoubleFactory1D.dense.make(c));
		}
	}
	
	public void setC(int[] c) {
		if(c!=null){
			setC(IntFactory1D.dense.make(c));
		}
	}
	
	public void setC(DoubleMatrix1D c) {
		this.c = c;
	}
	
	public void setC(IntMatrix1D c) {
		this.c = c;
	}
	
	public AbstractMatrix2D getA() {
		return A;
	}

	public void setA(double[][] A) {
		if(A!=null){
			setA(DoubleFactory2D.sparse.make(A));
		}
	}
	
	public void setA(DoubleMatrix2D A) {
		this.A = A;
	}

	public void setA(int[][] A) {
		if(A!=null){
			setG(IntFactory2D.sparse.make(A));
		}
	}
	
	public void setA(IntMatrix2D A) {
		this.A = A;
	}

	public AbstractMatrix1D getB() {
		return b;
	}

	public void setB(double[] b) {
		if(b!=null){
			setB(DoubleFactory1D.dense.make(b));
		}
	}
	
	public void setB(DoubleMatrix1D b) {
		this.b = b;
	}
	
	public void setB(int[] b) {
		if(b!=null){
			setB(IntFactory1D.dense.make(b));
		}
	}
	
	public void setB(IntMatrix1D b) {
		this.b = b;
	}
	
	public AbstractMatrix2D getG() {
		return G;
	}

	public void setG(double[][] G) {
		if(G!=null){
			setG(DoubleFactory2D.sparse.make(G));
		}
	}
	
	public void setG(DoubleMatrix2D G) {
		this.G = G;
	}

	public void setG(int[][] G) {
		if(G!=null){
			setG(IntFactory2D.sparse.make(G));
		}
	}
	
	public void setG(IntMatrix2D G) {
		this.G = G;
	}

	public AbstractMatrix1D getH() {
		return h;
	}

	public void setH(double[] h) {
		if(h!=null){
			setH(DoubleFactory1D.dense.make(h));
		}
	}
	
	public void setH(DoubleMatrix1D h) {
		this.h = h;
	}
	
	public void setH(int[] h) {
		if(h!=null){
			setH(IntFactory1D.dense.make(h));
		}
	}
	
	public void setH(IntMatrix1D h) {
		this.h = h;
	}
	
	public boolean isDumpProblem() {
		return this.dumpProblem;
	}
	
	public void setDumpProblem(boolean dumpProblem) {
		this.dumpProblem = dumpProblem;
	}
	
	public int getMaxIteration() {
		return maxIteration;
	}

	public void setMaxIteration(int maxIteration) {
		this.maxIteration = maxIteration;
	}

	
	@Override
	public String toString(){
		try{
			StringBuffer sb = new StringBuffer();
			sb.append(this.getClass().getName() + ": ");
			sb.append("\nmin(c) s.t.");
			if(G != null && G.rows()>0){
				sb.append("\nG.x <= h");
			}
			if(A!=null && A.rows()>0){
				sb.append("\nA.x = b");
			}
			sb.append("\nc: " + ColtUtils.toString(c));
			if(G != null){
				sb.append("\nG: " + ColtUtils.toString(G));
				sb.append("\nh: " + ColtUtils.toString(h));
			}
			if(A!=null){
				sb.append("\nA: " + ColtUtils.toString(A));
				sb.append("\nb: " + ColtUtils.toString(b));
			}
			
			return sb.toString();
		}catch(Exception e){
			return "";
		}
	}
	
	public BIPOptimizationRequest cloneMe(){
		BIPOptimizationRequest clonedBIPRequest = new BIPOptimizationRequest();
		clonedBIPRequest.setDumpProblem(isDumpProblem());
		clonedBIPRequest.setMaxIteration(getMaxIteration());
		clonedBIPRequest.setPresolvingDisabled(isPresolvingDisabled());
		clonedBIPRequest.setRemoveDominatingColumns(isRemoveDominatingColumns());
		clonedBIPRequest.setResolveConnectedComponents(isResolveConnectedComponents());
		
		return clonedBIPRequest;
	}

	public boolean isPresolvingDisabled() {
		return presolvingDisabled;
	}

	public void setPresolvingDisabled(boolean presolvingDisabled) {
		this.presolvingDisabled = presolvingDisabled;
	}

	public boolean isRemoveDominatingColumns() {
		return removeDominatingColumns;
	}

	public void setRemoveDominatingColumns(boolean removeDominatingColumns) {
		this.removeDominatingColumns = removeDominatingColumns;
	}

	public boolean isCheckMinMaxFeasibility() {
		return checkMinMaxFeasibility;
	}

	public void setCheckMinMaxFeasibility(boolean checkMinMaxFeasibility) {
		this.checkMinMaxFeasibility = checkMinMaxFeasibility;
	}

	public boolean isResolveConnectedComponents() {
		return resolveConnectedComponents;
	}

	public void setResolveConnectedComponents(boolean resolveConnectedComponents) {
		this.resolveConnectedComponents = resolveConnectedComponents;
	}
}
