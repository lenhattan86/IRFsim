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

public class Rational {

	private int num, denom;

	public Rational(int num, int denom) {
		this.num = num;
		this.denom = denom;
	}
	
	public Rational(double d) {
		String s = String.valueOf(d);
		int digitsDec = s.length() - 1 - s.indexOf('.');

		int denom = 1;
		for (int i = 0; i < digitsDec; i++) {
			d *= 10;
			denom *= 10;
		}
		int num = (int) Math.round(d);

		int g = Utils.gcd(num, denom);
		this.num = num / g;
		this.denom = denom / g;
	}

	public String toString() {
		return String.valueOf(this.num) + "/" + String.valueOf(this.denom);
	}
	
	public int getNum(){
		return this.num;
	}
	
	public int getDenom(){
		return this.denom;
	}
}
