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

public class IterationsLimitException extends JOptimizerException{
	
	public static final String MAX_ITERATIONS_EXCEEDED = "max iteration limit exceeded";
	
	public IterationsLimitException(){
		super();
	}
	
	public IterationsLimitException(String message){
		super(message);
	}
}
