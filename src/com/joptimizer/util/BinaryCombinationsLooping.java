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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loops aver all the possible realization of a binary array of length n.
 * Es: 
 * length: 2
 * Realizations: {0,0},{0,1},{1,0},{1,1}
 * The current realization is passed back to the given callback handler.
 */
public class BinaryCombinationsLooping {

	private int lenght;
	private CombCallbackHandler cbHandler;
	private Log log = LogFactory.getLog(this.getClass().getName());

	public BinaryCombinationsLooping(int lenght, CombCallbackHandler cbHandler) {
		this.lenght = lenght;
		this.cbHandler = cbHandler;
	}

	public void doLoop() {

		int n = this.lenght;
		int maxIter = (int) Math.pow(2, n) - 1;
		
		int realization[] = new int[n];
		int iter = -1;
		while (iter < maxIter) {
			iter = iter + 1;
			//log.debug("iter: " + iter);
			// System.out.println(Integer.toBinaryString(counter));
			String s = StringUtils.leftPad(Integer.toBinaryString(iter), n, "0");
			// System.out.println(s);
			for (int c = 0; c < n; c++) {
				realization[c] = Integer.parseInt("" + s.charAt(c));
			}
			//log.debug(ArrayUtils.toString(res));
			this.cbHandler.handle(realization);
		}
	}

	public static abstract class CombCallbackHandler {
		public abstract void handle(int[] realization);
	}
}