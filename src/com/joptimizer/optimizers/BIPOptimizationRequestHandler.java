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

import com.joptimizer.exception.JOptimizerException;

import cern.colt.matrix.AbstractMatrix1D;
import cern.colt.matrix.AbstractMatrix2D;

public abstract class BIPOptimizationRequestHandler {
	
	protected BIPOptimizationRequest request;
	protected BIPOptimizationResponse response;
	private int dim = -1;
	private int meq = -1;
	private int mieq = -1;
	
	public abstract void optimize() throws JOptimizerException;
	
	protected boolean isDumpProblem() {
		return getBIPOptimizationRequest().isDumpProblem();
	}
	
	protected int getDim() {
		if (dim < 0) {
			dim = (int) this.getC().size();
		}
		return dim;
	}
	
	protected int getMieq() {
		if (mieq < 0) {
			mieq = (getG() == null) ? 0 : getG().rows();
		}
		return mieq;
	}
	
	protected int getMeq() {
		if (meq < 0) {
			meq = (getA() == null) ? 0 : getA().rows();
		}
		return meq;
	}
	
	protected final int getMaxIteration(){
		return request.getMaxIteration();
	}
	
	protected AbstractMatrix1D getC() {
		return getBIPOptimizationRequest().getC();
	}
	
	protected AbstractMatrix2D getA() {
		return getBIPOptimizationRequest().getA();
	}
	
	protected AbstractMatrix1D getB() {
		return getBIPOptimizationRequest().getB();
	}
	
	protected AbstractMatrix2D getG() {
		return getBIPOptimizationRequest().getG();
	}
	
	protected AbstractMatrix1D getH() {
		return getBIPOptimizationRequest().getH();
	}
	
	public BIPOptimizationRequest getBIPOptimizationRequest() {
		return request;
	}

	public void setBIPOptimizationRequest(BIPOptimizationRequest request) {
		this.request = request;
	}

	public BIPOptimizationResponse getBIPOptimizationResponse() {
		return response;
	}

	public void setBIPOptimizationResponse(BIPOptimizationResponse response) {
		this.response = response;
	}

}
