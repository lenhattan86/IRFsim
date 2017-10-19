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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author alberto
 * @see http://www.sanfoundry.com/java-program-find-connected-components-undirected-graph/
 * @see http://www.java2s.com/Code/Java/2D-Graphics-GUI/Anundirectedgraphthatkeepstrackofconnectedcomponentsgroups.htm
 */
public class UndirectedGraph {

	private int maxVertices;
	private int maxDegree;
	/**
	 * The row number indicates the vertex, the relative array is for its connected vertices.
	 */
	private int[][] vertices;
	private int[] degree;
	private int nVertices;
	private int nEdges;
	private boolean[] processed;
	private boolean[] discovered;
	private int[] parent;
	private Log log = LogFactory.getLog(this.getClass().getName());

	public UndirectedGraph(int maxVertices, int maxDegree) {
		this.maxVertices = maxVertices;
		this.maxDegree = maxDegree;
		this.vertices = new int[maxVertices][];
		this.degree = new int[maxVertices];
	}

	/**
	 * @TODO definire la frima con booleano e chiamare con e senza booleano prima (x,y) poi (y,x)
	 */
	public void addEdge(int x, int y) {

		boolean added = false;
		if (x < 0) {
			throw new IllegalArgumentException("expected >= 0, actual = " + x);
		}
		if (y < 0) {
			throw new IllegalArgumentException("expected >= 0, actual = " + y);
		}

		int degreeX = degree[x];
		if (degreeX > maxDegree) {
			log.error("Max degree exceeded for vertex " + x);
			throw new RuntimeException("Max degree exceeded for vertex " + x);
		}
		int[] verticesX = vertices[x];
		if (verticesX == null) {
			verticesX = new int[maxDegree];
			Arrays.fill(verticesX, -1);
			vertices[x] = verticesX;
			nVertices++;
		}
		if (!ArrayUtils.contains(verticesX, y)) {
			verticesX[degreeX] = y;
			degree[x]++;
			added = true;
		}

		int degreeY = degree[y];
		if (degreeY > maxDegree) {
			log.error("Max degree exceeded for vertex " + y);
			throw new RuntimeException("Max degree exceeded for vertex " + y);
		}
		int[] verticesY = vertices[y];
		if (verticesY == null) {
			verticesY = new int[maxDegree];
			Arrays.fill(verticesY, -1);
			vertices[y] = verticesY;
			nVertices++;
		}
		if (!ArrayUtils.contains(verticesY, x)) {
			verticesY[degreeY] = x;
			degree[y]++;
			added = true;
		}

		if (added) {
			nEdges++;
		}
	}

	public List<int[]> listConnectedComponents(boolean ordered) {
		
		List<int[]> ret = new ArrayList<int[]>(); 
		
		// initialization
		this.processed = new boolean[this.maxVertices];
		this.discovered = new boolean[this.maxVertices];
		this.parent = new int[this.maxVertices];
		Arrays.fill(parent, -1);

		//looping
		//int component = 0;
		for (int v = 0; v < maxVertices; v++) {
			if (!discovered[v]) {
				//component++;
				//log.debug("Component " + component);
				bfs(v, ret);
				//log.debug("\n");
			}
		}
		//log.debug("number of connected components: " + ret.size());
		
		if(ordered){
			Collections.sort(ret, new Comparator<int[]>() {
				public int compare(int[] o1, int[] o2) {
					return Integer.valueOf(o2.length).compareTo(Integer.valueOf(o1.length));
				}
			});
		}
	
		return ret;
	}

	private void bfs(int v, List<int[]> ccomponents) {
		Queue<Integer> queue = new LinkedList<Integer>();
		int x, y;
		int[] toAdd = new int[]{};
		queue.offer(v);
		discovered[v] = true;
		while (!queue.isEmpty()) {
			boolean add = false;
			x = queue.remove();
			processed[x] = true;
			for (y = degree[x] - 1; y >= 0; y--) {
				add = true;
				if (!discovered[vertices[x][y]]) {
					queue.offer(vertices[x][y]);
					discovered[vertices[x][y]] = true;
					parent[vertices[x][y]] = x;
				}
			}
			if (add) {
				//log.debug(add + " " + x);
				toAdd = Utils.addToSortedArray(toAdd, x);
			}
		}
		if(toAdd.length > 0 ){
			ccomponents.add(toAdd);
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("graph " + this.getClass().getSimpleName() + ": \n");
		sb.append("n vertices " + this.nVertices + ": \n");
		sb.append("n edges " + this.nEdges + ": \n");
		for (int i = 0; i < maxVertices; i++) {
			int[] edgesI = vertices[i];
			if (edgesI != null) {
				sb.append(i + ": ");
				for (int j = 0; j < degree[i]; j++) {
					sb.append(" " + edgesI[j]);
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}

}
