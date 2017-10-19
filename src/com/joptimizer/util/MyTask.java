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
package com.joptimizer.util;

import java.util.concurrent.Callable;

/**
 * Parallelized task.
 * @see "http://embarcaderos.net/2011/01/23/parallel-processing-and-multi-core-utilization-with-java/"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 *
 */
public class MyTask implements Callable<Integer> {
	private int id;

	public MyTask() {
	}

	public MyTask(int id) {
		this.id = id;
	}

	public Integer call() {
		String str = "";
		long t0 = new java.util.Date().getTime();
		System.out.println("start - Task " + id);
		try {
			// sleep for 1 second to simulate a remote call,
			// just waiting for the call to return
			//Thread.sleep(1000);
			// loop that just concatenate a str to simulate
			// work on the result form remote call
			for (int i = 0; i < 100000000*Math.random(); i++) {
				//str = str + 't';
				for(int k=0; k<10000; k++){
					new java.util.Date().getTime();
				}
			}
		} catch (Exception e) {

		}
		Double secs = new Double((new java.util.Date().getTime() - t0) * 0.001);
		System.out.println("run time for " +id + ": " + secs + " secs");
		return this.id;
	}
}
