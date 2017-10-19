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
package com.joptimizer.exception;

public class KKTSolutionException extends JOptimizerException{
	
	public static final String SOLUTION_FAILED = "KKT solution failed";
	public static final String SINGULAR_SYSTEM = "singular KKT system";
	
	public KKTSolutionException(){
		super();
	}
	
	public KKTSolutionException(String message){
		super(message);
	}
}
