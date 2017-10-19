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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import com.joptimizer.exception.JOptimizerException;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class Utils {
	
	private static Double RELATIVE_MACHINE_PRECISION = Double.NaN;
	public static Log log = LogFactory.getLog(Utils.class);
	private static long IDCOUNTER = 0;

	public static synchronized String createID() {
		return String.valueOf(IDCOUNTER++);
	}
	
	public static File getClasspathResourceAsFile(String resourceName) throws URISyntaxException {
		return new File(new URI(Thread.currentThread().getContextClassLoader().getResource(resourceName).toString()));
	}
	
	/**
	 * Calculate the scaled residual 
	 * <br> ||Ax-b||_oo/( ||A||_oo . ||x||_oo + ||b||_oo ), with
	 * <br> ||x||_oo = max(||x[i]||)
	 */
	public static double calculateScaledResidual(double[][] A, double[][] X, double[][] B){
		DoubleMatrix2D AMatrix = DoubleFactory2D.dense.make(A); 
		DoubleMatrix2D XMatrix = DoubleFactory2D.dense.make(X);
		DoubleMatrix2D BMatrix = DoubleFactory2D.dense.make(B);
		return ColtUtils.calculateScaledResidual(AMatrix, XMatrix, BMatrix);
	}
	
	/**
	 * Calculate the scaled residual 
	 * <br> ||Ax-b||_oo/( ||A||_oo . ||x||_oo + ||b||_oo ), with
	 * <br> ||x||_oo = max(||x[i]||)
	 */
	public static double calculateScaledResidual(double[][] A, double[] x, double[] b){
		DoubleMatrix2D AMatrix = DoubleFactory2D.dense.make(A); 
		DoubleMatrix1D xVector = DoubleFactory1D.dense.make(x);
		DoubleMatrix1D bVector = DoubleFactory1D.dense.make(b);
		return ColtUtils.calculateScaledResidual(AMatrix, xVector, bVector);
	}
	
	/**
	 * Residual conditions check after resolution of A.x=b.
	 * 
	 * eps := The relative machine precision
	 * N   := matrix dimension
	 * 
     * Checking the residual of the solution. 
     * Inversion pass if scaled residuals are less than 10:
	 * ||Ax-b||_oo/( (||A||_oo . ||x||_oo + ||b||_oo) . N . eps ) < 10.
	 * 
	 * @param A not-null matrix
	 * @param x not-null vector
	 * @param b not-null vector
	 */
//	public static boolean checkScaledResiduals(DoubleMatrix2D A, DoubleMatrix1D x, DoubleMatrix1D b, Algebra ALG) {
//	  //The relative machine precision
//		double eps = RELATIVE_MACHINE_PRECISION;
//		int N = A.rows();//matrix dimension
//		double residual = -Double.MAX_VALUE;
//		if(Double.compare(ALG.normInfinity(x), 0.)==0 && Double.compare(ALG.normInfinity(b), 0.)==0){
//			return true;
//		}else{
//			residual = ALG.normInfinity(ALG.mult(A, x).assign(b,	Functions.minus)) / 
//	          ((ALG.normInfinity(A)*ALG.normInfinity(x) + ALG.normInfinity(b)) * N * eps);
//			log.debug("scaled residual: " + residual);
//			return residual < 10;
//		}
//	}
	
	/**
	 * The smallest positive (epsilon) such that 1.0 + epsilon != 1.0.
	 * @see http://en.wikipedia.org/wiki/Machine_epsilon#Approximation_using_Java
	 */
	public static final double getDoubleMachineEpsilon() {
		
		if(!Double.isNaN(RELATIVE_MACHINE_PRECISION)){
			return RELATIVE_MACHINE_PRECISION;
		}
		
		synchronized(RELATIVE_MACHINE_PRECISION){
			
			if(!Double.isNaN(RELATIVE_MACHINE_PRECISION)){
				return RELATIVE_MACHINE_PRECISION;
			}
			
			double eps = 1.;
			do {
				eps /= 2.;
			} while ((double) (1. + (eps / 2.)) != 1.);
			
			if(log.isDebugEnabled()){
				log.debug("Calculated double machine epsilon: " + eps);
			}
			RELATIVE_MACHINE_PRECISION = eps;
		}
		
		return RELATIVE_MACHINE_PRECISION;
	}
	
	/**
   * Get the index of the maximum entry.
   */
  public static int getMaxIndex(RealVector v){
  	return ColtUtils.getMaxIndex(DoubleFactory1D.dense.make(v.toArray()));
  } 
	  
	public static final double[][] createConstantDiagonalMatrix(int dim, double c) {
		double[][] matrix = new double[dim][dim];
		for (int i = 0; i < dim; i++) {
			matrix[i][i] = c;
		}
		return matrix;
	}
	
	/**
	 * @deprecated avoid calculating matrix inverse, better use the solve methods
	 */
	@Deprecated
	public static final double[][] upperTriangularMatrixInverse(double[][] L) throws JOptimizerException {
		
		// Solve L*X = Id
		int dim = L.length;
		double[][] x = Utils.createConstantDiagonalMatrix(dim, 1.);
		for (int j = 0; j < dim; j++) {
			final double[] LJ = L[j];
			final double LJJ = LJ[j];
			final double[] xJ = x[j];
			for (int k = 0; k < dim; ++k) {
				xJ[k] /= LJJ;
			}
			for (int i = j + 1; i < dim; i++) {
				final double[] xI = x[i];
				final double LJI = LJ[i];
				for (int k = 0; k < dim; ++k) {
					xI[k] -= xJ[k] * LJI;
				}
			}
		}
        
		return new Array2DRowRealMatrix(x).transpose().getData();
	}
	
	/**
	 * @deprecated avoid calculating matrix inverse, better use the solve methods
	 */
	@Deprecated
	public static final double[][] lowerTriangularMatrixInverse(double[][] L) throws JOptimizerException {
		double[][] LT = new Array2DRowRealMatrix(L).transpose().getData();
		double[][] x = upperTriangularMatrixInverse(LT);
		return new Array2DRowRealMatrix(x).transpose().getData();
	}
	
	/**
	 * Brute-force determinant calculation.
	 */
	public static final double calculateDeterminant(double[][] ai, int dim) {
		double det = 0;
		if (dim == 1) {
			det = ai[0][0];
		} else if (dim == 2) {
			det = ai[0][0] * ai[1][1] - ai[0][1] * ai[1][0];
		} else {
			double ai1[][] = new double[dim - 1][dim - 1];
			for (int k = 0; k < dim; k++) {
				double ai0k = ai[0][k];
				if(ai0k < 0 || ai0k > 0){
					for (int i1 = 1; i1 < dim; i1++) {
						int j = 0;
						for (int j1 = 0; j1 < dim; j1++) {
							if (j1 != k) {
								ai1[i1 - 1][j] = ai[i1][j1];
								j++;
							}
						}
					}
					if (k % 2 == 0) {
						det += ai0k * calculateDeterminant(ai1, dim - 1);
					} else {
						det -= ai0k * calculateDeterminant(ai1, dim - 1);
					}
				}
//				else{
//					log.debug("skipped");
//				}
			}
		}
		return det;
	}
	
	public static final void writeDoubleArrayToFile(double[] v, String fileName) throws IOException {
		DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		df.applyPattern("#");
		df.setMaximumFractionDigits(16);
		String[][] ret = new String[v.length][1];
		for(int j=0; j<v.length; j++){
			if(Double.isNaN(v[j])){
				ret[j][0] = String.valueOf(v[j]);
			}else{
				ret[j][0] = df.format(v[j]);
				//ret[j][0] = String.valueOf(v[j]);
			}
		}
		CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(fileName), CSVFormat.DEFAULT.withDelimiter(','));
		try{
			csvPrinter.printRecords(ret);
		}finally{
			csvPrinter.close();
		}
	}
	
	public static final void writeDoubleMatrixToFile(double[][] m, String fileName) throws IOException {
		DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		df.applyPattern("#");
		df.setMaximumFractionDigits(16);
		String[][] ret = new String[m.length][];
		for(int i=0; i<m.length; i++){
			double[] MI = m[i];
			String[] retI = new String[MI.length];
			for(int j=0; j<MI.length; j++){
				if(Double.isNaN(MI[j])){
					retI[j] = String.valueOf(MI[j]);
				}else{
					retI[j] = df.format(MI[j]);
					//retI[j] = String.valueOf(MI[j]);
				}
			}
			ret[i] = retI;
		}
		CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(fileName), CSVFormat.DEFAULT.withDelimiter(','));
		try{
			csvPrinter.printRecords(ret);
		}finally{
			csvPrinter.close();
		}
	}
	
	public static final double[] loadDoubleArrayFromFile(String classpathFileName) throws IOException {
		//FileReader fr = new FileReader(classpathFileName);
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathFileName);
		CSVParser parser = new CSVParser(new InputStreamReader(is), CSVFormat.DEFAULT.withDelimiter(',').withCommentMarker('#'));
		List<CSVRecord> records = parser.getRecords();
		double[] v = new double[records.size()];
		for(int i=0; i<records.size(); i++){
			v[i] = Double.parseDouble(records.get(i).get(0));
		}
		return v;
	}
	
	public static final int[] loadIntArrayFromFile(String classpathFileName) throws IOException {
		//FileReader fr = new FileReader(classpathFileName);
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathFileName);
		CSVParser parser = new CSVParser(new InputStreamReader(is), CSVFormat.DEFAULT.withDelimiter(',').withCommentMarker('#'));
		List<CSVRecord> records = parser.getRecords();
		int[] v = new int[records.size()];
		for(int i=0; i<records.size(); i++){
			v[i] = (int) Double.parseDouble(records.get(i).get(0));
		}
		return v;
	}
		
	public static final double[][] loadDoubleMatrixFromFile(String classpathFileName, char fieldSeparator) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathFileName);
		CSVParser parser = new CSVParser(new InputStreamReader(is), CSVFormat.DEFAULT.withDelimiter(fieldSeparator).withCommentMarker('#'));
		List<CSVRecord> records = parser.getRecords();
		double[][] m = new double[records.size()][records.get(0).size()];
		for(int i=0; i<records.size(); i++){
			for(int j=0; j<records.get(0).size(); j++){
				m[i][j] = Double.parseDouble(records.get(i).get(j));
			}
		}
		return m;
	}
	
	public static final double[][] loadDoubleMatrixFromFile(String classpathFileName) throws IOException {
		return loadDoubleMatrixFromFile(classpathFileName, ",".charAt(0));
	}
	
	public static final int[][] loadIntMatrixFromFile(String classpathFileName, char fieldSeparator) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathFileName);
		CSVParser parser = new CSVParser(new InputStreamReader(is), CSVFormat.DEFAULT.withDelimiter(fieldSeparator).withCommentMarker('#'));
		List<CSVRecord> records = parser.getRecords();
		int[][] m = new int[records.size()][records.get(0).size()];
		for(int i=0; i<records.size(); i++){
			for(int j=0; j<records.get(0).size(); j++){
				m[i][j] = (int) Double.parseDouble(records.get(i).get(j));
			}
		}
		return m;
	}
	
	public static final int[][] loadIntMatrixFromFile(String classpathFileName) throws IOException {
		return loadIntMatrixFromFile(classpathFileName, ",".charAt(0));
	}
	
	public static final double[] toDoubleArray(int[] intArray) {
		double[] ret = new double[intArray.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = intArray[i];
		}
		return ret;
	}

	public static final double[][] toDoubleMatrix(int[][] intMatrix) {
		double[][] ret = new double[intMatrix.length][];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = toDoubleArray(intMatrix[i]);
		}
		return ret;
	}
	
	public static final int[] getFullRankSubmatrixRowIndices(RealMatrix M) {
		int row = M.getRowDimension();
		int col = M.getColumnDimension();
		
		SingularValueDecomposition dFact1 = new SingularValueDecomposition(M);
		int  r = dFact1.getRank();
		int[] ret = new int[r];
		
		if(r<row){
			//we have to find a submatrix of M with row dimension = rank
			RealMatrix fullM = MatrixUtils.createRealMatrix(1, col);
			fullM.setRowVector(0, M.getRowVector(0));
			ret[0] = 0;
			int iRank = 1;
			for(int i=1; i<row; i++){
				RealMatrix tmp = MatrixUtils.createRealMatrix(fullM.getRowDimension()+1, col);
				tmp.setSubMatrix(fullM.getData(), 0, 0);
				tmp.setRowVector(fullM.getRowDimension(), M.getRowVector(i));
				SingularValueDecomposition dFact_i = new SingularValueDecomposition(tmp);
				int ri = dFact_i.getRank();
				if(ri>iRank){
					fullM = tmp;
					ret[iRank] = i;
					iRank = ri;
					if(iRank==r){
						break;//target reached!
					}
				}
			}
		}else{
			for(int i=0; i<r; i++){
				ret[i] = i;
			}
		}
		
		return ret;
	}
	
	/**
	 * Extract the sign (the leftmost bit), exponent (the 11 following bits) 
	 * and mantissa (the 52 rightmost bits) from a double.
	 * @see http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Tech/Chapter02/floatingPt.html
	 */
	public static final long[] getExpAndMantissa(double myDouble) {
		long lbits = Double.doubleToLongBits(myDouble);
		long lsign = lbits >>> 63;// 0(+) or 1(-)
		long lexp = (lbits >>> 52 & ((1 << 11) - 1)) - ((1 << 10) - 1);
		long lmantissa = lbits & ((1L << 52) - 1);
		long[] ret = new long[] { lsign, lexp, lmantissa };
		if(log.isDebugEnabled()){
			log.debug("double  : " + myDouble);
			log.debug("sign    : " + lsign);
			log.debug("exp     : " + lexp);
			log.debug("mantissa: " + lmantissa);
			log.debug("reverse : " + Double.longBitsToDouble((lsign << 63)	| (lexp + ((1 << 10) - 1)) << 52 | lmantissa));
			log.debug("log(d)  : " + Math.log1p(myDouble));
		}
		return ret;
	}
	
	/**
	 * Return a new array with all the occurences of oldValue replaced by newValue.
	 */
	public static final double[] replaceValues(double[] v, double oldValue,	double newValue) {
		double[] ret = new double[v.length];
		for (int i = 0; i < v.length; i++) {
			double vi = v[i];
			if (Double.compare(oldValue, vi) != 0) {
				// no substitution
				ret[i] = vi;
			} else {
				ret[i] = newValue;
			}
		}
		return ret;
	}
	
	/**
	 * Fill the array with values 0, 1, 2,..., n
	 * @param v
	 * @param startValue
	 */
	public static final void incrementalFill(int[] v) {
		for(int i=0; i<v.length; i++){
			v[i] = i;
		}
	}
	
	public static final double round(double d, double precision){
		return Math.round(d * precision) / precision;
	}
	
	public static final void serializeObject(Object obj, String filename) throws IOException{
		FileOutputStream fout = new FileOutputStream(filename, true);
	  ObjectOutputStream oos = new ObjectOutputStream(fout);
	  oos.writeObject(obj);
	}
	
	public static final Object deserializeObject(String classpathFileName) throws IOException, ClassNotFoundException{
		InputStream streamIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathFileName);
		ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);
		return objectinputstream.readObject();
	}
	
	/**
	 * Greatest common divisor.
	 */
	public static final int gcd(int num, int denom) {
		if (denom == 0) {
			return num;
		}
		return Utils.gcd(denom, num % denom);
	}

	/**
	 * Least Common Multiple.
	 */
	public static final int lcm(int a, int b) {
		return a * (b / Utils.gcd(a, b));
	}

	/**
	 * Least Common Multiple.
	 */
	public static final int lcm(int[] input) {
		int result = input[0];
		for (int i = 1; i < input.length; i++) {
			result = Utils.lcm(result, input[i]);
		}
		return result;
	}
	
	/**
	 * 
	 * @param i
	 * @return -1 if i<0, +1 if i >= 0
	 */
	public static final int getSign(int i){
		int sign = (int) Math.signum(i);//-1, 0, +1
		sign = (sign < 0) ? -1 : 1;
		return sign;
	}
	
	/**
	 * @param d
	 * @return -1 if d<0, +1 if d>=0
	 */
	public static final int getSign(double d){
		int sign = (int) Math.signum(d);//-1, 0, +1
		sign = (sign < 0) ? -1 : 1;
		return sign;
	}
	
	/**
	 * Es:
	 * <br>{0,1,2,3,4,5} add 0  = {0,1,2,3,4,5}
	 * <br>{0,1,2,3,4,5} add 3  = {0,1,2,3,4,5}
	 * <br>{0,1,2,3,4,5} add -1 = {-1,0,1,2,3,4,5}
	 * <br>{0,1,2,3,4,5} add 6  = {0,1,2,3,4,5,6}
	 */
	public static final int[] addToSortedArray(int[] sortedArray, int element) {
		int[] ret = new int[sortedArray.length + 1];
		int cnt = 0;
		boolean goStraight = false;
		for (int i = 0; i < sortedArray.length; i++) {
			int s = sortedArray[i];
			if (goStraight) {
				ret[cnt] = s;
				cnt++;
			} else {
				if (s < element) {
					ret[cnt] = s;
					cnt++;
					continue;
				}
				if (s == element) {
					return sortedArray;
				}
				if (s > element) {
					ret[cnt] = element;
					cnt++;
					ret[cnt] = s;
					cnt++;
					goStraight = true;
				}
			}
		}
		if (cnt < ret.length) {
			// to be added at the last position
			ret[cnt] = element;
		}
		return ret;
	}
	
	/**
	 * Removes the first occurrence of the specified element from the specified
	 * array. All subsequent elements are shifted to the left.
	 */
	public static final int[] removeFromSortedArray(int[] sortedArray, int element) {
		if (sortedArray.length < 2) {
			//in order to avoid the call to ArrayUtils 
			return new int[] {};
		}
		return ArrayUtils.removeElement(sortedArray, element);
	}
	
	public static final boolean isInSortedArray(int[] sortedArray, int element) {
		boolean ret = false;
		for (int i = 0; i < sortedArray.length; i++) {
			int sortedArrayI = sortedArray[i];
			if (sortedArrayI == element) {
				ret = true;
				break;
			}else if(sortedArrayI > element){
				break;
			}
		}
		return ret;
	}
	
	public static <T> T[] append(T[] myArray, T newArrayElement) {
		ArrayPP<T> app = new ArrayPP<T>(myArray);
		app.append(newArrayElement);
		myArray = app.toArray();
		return myArray;
	}

	private static class ArrayPP<T> {
		private T[] t;
		public ArrayPP(T[] array) {
			t = array;
		}
		public ArrayPP<T> append(T val) {
			t = java.util.Arrays.copyOf(t, t.length + 1);
			t[t.length - 1] = val;
			return this;
		}
		public T[] toArray() {
			return t;
		}
	}
	
	public static final boolean isZero(double d) {
		return !(d * d > 0);
	}
	
	public static final boolean isZero(int i) {
		return i == 0;
	}
	
	public static final int checkBounds(int x) throws MathArithmeticException {
        if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE) {
            throw new MathArithmeticException();
        }
        return x;
    }
	
	public static final List<int[]> listConnectedComponents(double[][] M) {

		int rows = M.length;
		int cols = M[0].length;

		// build the undirected graph of the matrix
		UndirectedGraph graph = new UndirectedGraph(cols, cols);
		for (int i = 0; i < rows; i++) {
			double[] MI = M[i];
			Integer sourceVertex = null;
			Integer targetVertex = null;
			for (int j = 0; j < cols; j++) {
				double MIJ = MI[j];
				if (!isZero(MIJ)) {
					if (sourceVertex == null) {
						sourceVertex = j;
						targetVertex = null;
					} else {
						targetVertex = j;
						graph.addEdge(sourceVertex, targetVertex);
						sourceVertex = targetVertex;
						targetVertex = null;
					}
				}
			}
		}

		// search for the connected components
		List<int[]> connectedComponents = graph.listConnectedComponents(true);

		return connectedComponents;
	}
	
	public static final BlockReduction blockReduce(double[][] M) {

		List<int[]> connectedComponents = listConnectedComponents(M);
		if (connectedComponents.size() > 1) {
			//it is likely a sparse matrix
			DoubleMatrix2D M2 = DoubleFactory2D.sparse.make(M);
			int rows = M2.rows();
			int cols = M2.columns();
			int[] nRowsForComponent = new int[connectedComponents.size()];	
			
			//define the proper order of rows and columns
			int[] rowsOrder = new int[rows];//NB: not ordered array
			int[] colsOrder = new int[cols];//NB: not ordered array
			
			//rows reordering
			int rowCount = 0;
			for (int cc = 0; cc < connectedComponents.size(); cc++) {
				int[] component = connectedComponents.get(cc);
				int minC = component[0];
				int maxC = component[component.length - 1];
				for (int i = 0; i < rows; i++) {
					double[] MI = M[i];
					for (int j = minC; j < maxC; j++) {
						if (!isZero(MI[j])) {
							if (Utils.isInSortedArray(component, j)) {
								rowsOrder[rowCount] = i;
								rowCount++;
								nRowsForComponent[cc] = nRowsForComponent[cc] + 1;
								break;
							}
						}
					}
				}
			}
			for (int i = 0; i < cols; i++) {
				colsOrder[i] = i;
			}
			
			DoubleMatrix2D M3 = M2.viewSelection(rowsOrder, colsOrder);
			
			//columns reordering
			Arrays.fill(colsOrder, -1);
			int colCount = 0;
			int maxRPrev = 0;
			for (int cc = 0; cc < connectedComponents.size(); cc++) {
				int[] component = connectedComponents.get(cc);
				int maxR = maxRPrev + nRowsForComponent[cc];
				for (int j : component) {
					for (int i = maxRPrev; i < maxR; i++) {
						if (!isZero(M3.getQuick(i, j))) {
							colsOrder[colCount] = j;
							colCount++;
							break;
						}
					}
				}
				maxRPrev = maxR;
			}
			for (int i = 0; i < rows; i++) {
				rowsOrder[i] = i;
			}
			
			//return the rearranged data
			BlockReduction br = new BlockReduction();
			br.retMatrix = M3.viewSelection(rowsOrder, colsOrder).toArray();
			br.columnOrder = colsOrder;
			return br;
			
		} else {
			BlockReduction br = new BlockReduction();
			br.retMatrix = M;
			return br;
		}
	}
	
	public static final class BlockReduction {
		public double[][] retMatrix = null;
		/**
		 * The new column order.
		 */
		public int[] columnOrder = null;
		/**
		 * The (column) permutation matrix P
		 * that corresponds to the new column order (M.P). 
		 */
		public int[][] createPermutationMatrix() {
			if (columnOrder != null) {
				int n = columnOrder.length;
				int[][] ret = new int[n][n];
				for (int j = 0; j < n; j++) {
					ret[columnOrder[j]][j] = 1;
				}
				return ret;
			}
			return null;
		}
	}
	
}
