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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Combinations {
	
	/**
	 * Combines several collections of elements and create permutations of all of them, taking one element from each
	 * collection, and keeping the same order in resultant lists as the one in original list of collections.
	 * 
	 * <ul>Example
	 * <li>Input  = { {a,b,c} , {1,2,3,4} }</li>
	 * <li>Output = { {a,1} , {a,2} , {a,3} , {a,4} , {b,1} , {b,2} , {b,3} , {b,4} , {c,1} , {c,2} , {c,3} , {c,4} }</li>
	 * </ul>
	 * 
	 * <b>NOTE: the number of elements of the inner lists equals 
	 * the number of the input lists (one element is taken from each input lists)</b>
	 * 
	 * @param input Original list of collections which elements have to be combined.
	 * @return Resultant collection of lists with all permutations of original list.
	 */
	public static final <T> Collection<List<T>> combinations(List<Collection<T>> input) {
		if (input == null || input.isEmpty()) {
			return Collections.emptyList();
		} else {
			Collection<List<T>> ret = new LinkedList();
			combinationsImpl(input, ret, 0, new LinkedList<T>());
			
			//check expected number of combinations
			long expectedNOfCombinations = 1;
			for (Collection coll : input) {
				expectedNOfCombinations *= coll.size();
			}
			if (ret.size() != expectedNOfCombinations) {
				throw new IllegalStateException("unexpected number of combinations: " + ret.size() + "!="
						+ expectedNOfCombinations);
			}

			return ret;
		}
	}

	/** Recursive implementation for {@link #combinations(List, Collection)} */
	private static final <T> void combinationsImpl(List<Collection<T>> input, Collection<List<T>> ret, int d, List<T> current) {
	  // if depth equals number of original collections, final reached, add and return
	  if (d == input.size()) {
	    ret.add(current);
	    return;
	  }

	  // iterate from current collection and copy 'current' element N times, one for each element
	  Collection<T> currentCollection = input.get(d);
	  for (T element : currentCollection) {
	    List<T> copy = new LinkedList<T>(current);
	    copy.add(element);
	    combinationsImpl(input, ret, d + 1, copy);
	  }
	}
}
