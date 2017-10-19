/*
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.joptimizer.optimizers;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class OptimizationResponse {

	private double[] solution;
	
	private double value;

	public void setSolution(double[] solution) {
		this.solution = solution;
	}

	public double[] getSolution() {
		return solution;
	}

	public double getValue() {
		throw new UnsupportedOperationException("not yet implemented");
	}

	public void setValue(double value) {
		this.value = value;
	}
}
