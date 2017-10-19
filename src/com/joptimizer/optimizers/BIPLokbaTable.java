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

import com.joptimizer.util.Utils;

import cern.colt.function.tint.IntIntIntFunction;
import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;
import cern.colt.matrix.tint.impl.SparseIntMatrix1D;
import cern.colt.matrix.tint.impl.SparseIntMatrix2D;

/**
 * The LOKBA table is the following table of integer entries:
 * <table border="1">
 *  <tr>
 *   <td>sign[1]</td>
 *   <td>sign[...]</td>
 *   <td>sign[n]</td>
 *   <td>&nbsp;</td>
 *   <td>&nbsp;</td>
 *  <tr>
 *  <tr>
 *   <td>of[1]</td>
 *   <td>of[...]</td>
 *   <td>of[n]</td>
 *   <td>&nbsp;</td>
 *   <td>&nbsp;</td>
 *  <tr>
 *  <tr>
 *   <td>constr[1][1]</td>
 *   <td>constr[1][...]</td>
 *   <td>constr[1][n]</td>
 *   <td>P(constr[1])</td>
 *   <td>S(constr[1])</td>
 *  <tr>
 *  <tr>
 *   <td>constr[,,,][1]</td>
 *   <td>constr[,,,][...]</td>
 *   <td>constr[,,,][n]</td>
 *   <td>P(constr[,,,])</td>
 *   <td>S(constr[,,,])</td>
 *  <tr>
 *  <tr>
 *   <td>constr[m][1]</td>
 *   <td>constr[m][...]</td>
 *   <td>constr[m][n]</td>
 *   <td>P(constr[m])</td>
 *   <td>S(constr[m])</td>
 *  <tr>
 * </table>
 * <br>
 * where:
 * <ul>
 * 	<li>n: number of variables</li>
 * 	<li>m: number of constraints</li>
 * 	<li>sign[i]: the signs of the coefficients of the objective function considered as a <u>maximization</u></li>
 * 	<li>of[i]: the coefficients of the original <u>minimization</u> objective function</li>
 *  <li>constr[j][i]: the mXn integer matrix of constraints coefficients</li>
 *  <li>P(constr[j]): the RHS of a superior constraint after standardization</li>
 *  <li>S(constr[j]): the RHS of an inferior constraint after standardization</li>	
 * </ul>
 * <br>NOTE: we use a slightly different notation with respect to [1], in that:
 *  <ol>
 *   <li>we keep the original <u>minimization</u> objective function coefficients into the "of" row</li> 
 *   <li>we keep the original constraints coefficients signs into the table, the signs given by [1] are recalculated by the algorithm</li>
 *   <li>we only consider <= constraints, so we neglect the constraints sign column of [1]</li>
 *  </ol>    
 * 
 * @see [1] S. H. Pakzad-Moghadam, M. S. Shahmohammadi, R. Ghodsi
 *      "A Low-Order Knowledge-Based Algorithm (LOKBA) to Solve Binary Integer Programming Problems"
 */
public class BIPLokbaTable {
	
	private int n;
	private int mieq;
	private IntMatrix1D signs;
	private IntMatrix1D of;
	private IntMatrix2D constraints;
	private IntMatrix1D P;
	private IntMatrix1D S;
	
	private BIPLokbaTable(IntMatrix1D signs, IntMatrix1D of, IntMatrix2D constraints, IntMatrix1D P, IntMatrix1D S){
		
		if(signs.size() != of.size()){
			throw new IllegalArgumentException("Dimension mismatch: " + signs.size() + " != " + of.size());
		}
		if(P.size() != S.size()){
			throw new IllegalArgumentException("Dimension mismatch: " + P.size() + " != " + S.size());
		}
		if(P.size() != constraints.rows()){
			throw new IllegalArgumentException("Dimension mismatch: " + P.size() + " != " + constraints.rows());
		}
		if(signs.size() != constraints.columns()){
			throw new IllegalArgumentException("Dimension mismatch: " + signs.size() + " != " + constraints.columns());
		}
		
		this.n = (int) signs.size();
		this.mieq = (int) P.size();
		this.signs = signs;
		this.of = of;
		this.constraints = constraints;
		this.P = P;
		this.S = S;
	}
	
	/**
	 * Converts the given problem:
	 * <br>min(c.x) s.t.
	 * <br>G.x <= h
	 * <br>x in {0,1}<sup>n</sup>
	 * 
	 * <br>to the LOKBA table form:
	 * <br>{@link BIPLokbaTable LOKBA table}
	 * 
	 * @author trivellatoa
	 */
//	public static BIPLokbaTable toLokbaTable(DoubleMatrix1D c, DoubleMatrix2D G, DoubleMatrix1D h) {
//		
//		int myN = (int) c.size();
//		int myMieq = G.rows();
//		
//		IntMatrix1D signs = ((c instanceof SparseDoubleMatrix1D) ? IntFactory1D.sparse : IntFactory1D.dense).make(myN);
//		IntMatrix1D of = ((c instanceof SparseDoubleMatrix1D) ? IntFactory1D.sparse : IntFactory1D.dense).make(myN);
//		IntMatrix2D constr = ((G instanceof SparseDoubleMatrix2D) ? IntFactory2D.sparse : IntFactory2D.dense).make(myMieq, myN);
//		IntMatrix1D P = IntFactory1D.dense.make(myMieq);
//		IntMatrix1D S = IntFactory1D.dense.make(myMieq);
//		
//		BIPLokbaTable lokbaTable = new BIPLokbaTable(signs, of, constr, P, S);
//		
//		// double to int
//		Rational rational = null;
//		Rational[] rArray = new Rational[myN];
//		int[] lcmArray = new int[myN];// Least Common Multiple
//		for (int i = 0; i < myN; i++) {
//			rational = new Rational(c.getQuick(i));
//			rArray[i] = rational;
//			lcmArray[i] = rational.getDenom();
//		}
//		int lcm = Utils.lcm(lcmArray);
//		
//		for (int j = 0; j < myN; j++) {
//			int cj = rArray[j].getNum() * (lcm / rArray[j].getDenom());
//			int sign = Utils.getSign(-1 * c.getQuick(j));//remember: minimization to maximization
//			signs.setQuick(j, sign);
//			of.setQuick(j, cj);
//		}
//
//		for (int i = 0; i < myMieq; i++) {
//			rArray = new Rational[myN + 1];
//			lcmArray = new int[myN + 1];// Least Common Multiple
//			for (int j = 0; j < myN; j++) {
////				double gij = G.getQuick(i, j);
////				if(gij< -Integer.MAX_VALUE || gij > Integer.MAX_VALUE){
////					throw new IllegalArgumentException("max capacity exceeded");
////				}
//				rational = new Rational(G.getQuick(i, j));
//				rArray[j] = rational;
//				lcmArray[j] = rational.getDenom();
//			}
//			rational = new Rational(h.getQuick(i));
//			rArray[myN] = rational;
//			lcmArray[myN] = rational.getDenom();
//
//			lcm = Math.abs(Utils.lcm(lcmArray));
//			int p = 0;
//			int s = 0;
//			for (int j = 0; j < myN; j++) {
//				int uij = rArray[j].getNum() * (lcm / rArray[j].getDenom());
//				constr.setQuick(i, j, Utils.checkBounds(uij));
//				p = ArithmeticUtils.addAndCheck(p, (uij > 0) ? uij : 0);
//				s = ArithmeticUtils.addAndCheck(s, (uij < 0) ? uij : 0);
//			}
//			
//			P.setQuick(i, ArithmeticUtils.addAndCheck(p, - rArray[myN].getNum() * (lcm / rArray[myN].getDenom())));
//			S.setQuick(i, ArithmeticUtils.addAndCheck(rArray[myN].getNum() * (lcm / rArray[myN].getDenom()), - s));
//		}
//		
//		return lokbaTable;
//	}

	public static BIPLokbaTable toLokbaTable(IntMatrix1D c, IntMatrix2D G, IntMatrix1D h) {

		int myN = (int) c.size();
		int myMieq = G.rows();

		IntMatrix1D signs = ((c instanceof SparseIntMatrix1D) ? IntFactory1D.sparse	: IntFactory1D.dense).make(myN);
		IntMatrix1D of = c;
		final IntMatrix1D P = IntFactory1D.dense.make(myMieq);
		final IntMatrix1D S = IntFactory1D.dense.make(myMieq);
		
		BIPLokbaTable lokbaTable = new BIPLokbaTable(signs, of, G, P, S);

		for (int j = 0; j < myN; j++) {
			int sign = Utils.getSign(-1 * c.getQuick(j));//remember: minimization to maximization
			signs.setQuick(j, sign);
			//of.setQuick(j, -1 * c.getQuick(j));//remember: minimization to maximization
		}
		
//		for (int i = 0; i < myMieq; i++) {
//			int p = 0;
//			int s = 0;
//			for (int j = 0; j < myN; j++) {
//				int uij = G.getQuick(i, j);
//				p += (uij > 0) ? uij : 0;
//				s += (uij < 0) ? uij : 0;
//			}
//
//			P.setQuick(i, p - h.getQuick(i));
//			S.setQuick(i, h.getQuick(i) - s);
//		}
		
		for (int i = 0; i < myMieq; i++) {
			P.setQuick(i, -h.getQuick(i));
			S.setQuick(i, +h.getQuick(i));
		}
		G.forEachNonZero(new IntIntIntFunction() {
			public int apply(int i, int j, int gij) {
				if(gij > 0){
					P.setQuick(i, gij + P.getQuick(i));
				}else{
					S.setQuick(i, S.getQuick(i) - gij);
				}
				return gij;
			}
		});
		
		return lokbaTable;
	}

	public static BIPLokbaTable loadLokbaTable(IntMatrix2D T) {

		int myN = T.columns() - 2;
		int myMieq = T.rows() - 2;
		
		IntMatrix1D signs = T.viewRow(0).viewPart(0, myN);
		IntMatrix1D c = T.viewRow(1).viewPart(0, myN);
//		IntMatrix1D c = T.viewRow(1).viewPart(0, myN).copy();
//		for (int j = 0; j < c.size(); j++) {
//			c.setQuick(j, c.getQuick(j));
//		}
		IntMatrix2D G = T.viewPart(2, 0, myMieq, myN);
		IntMatrix1D P = T.viewColumn(myN).viewPart(2, myMieq);
		IntMatrix1D S = T.viewColumn(myN + 1).viewPart(2, myMieq);
		
		return new BIPLokbaTable(signs, c, G, P, S);
	}
	
	/**
	 * Return a selection (by reference) of the table.
	 */
	public BIPLokbaTable viewColumnSelection(int[] columnIndexes) {
		int[] rowIndexes = new int[mieq];
		for (int i = 0; i < mieq; i++) {
			rowIndexes[i] = i;
		}
		return viewSelection(rowIndexes, columnIndexes);
	}
	
	/**
	 * Return a selection (by reference) of the table.
	 */
	public BIPLokbaTable viewSelection(int[] rowIndexes, int[] columnIndexes) {
		return new BIPLokbaTable(
				signs.viewSelection(columnIndexes),
				of.viewSelection(columnIndexes), 
				constraints.viewSelection(rowIndexes, columnIndexes),
				P.viewSelection(rowIndexes), 
				S.viewSelection(rowIndexes));
	}

	/**
	 * Convert from the LOKBA table form to the standard form 
	 * <br>min(c.x) s.t.
	 * <br>G.x <= h
	 * 
	 * @return the object array {c, G, h}, where elements are by reference
	 * @see {@link BIPStandardConverter BIP standard form}
	 */
	public Object[] asStandardForm(){
		
		IntMatrix1D c = of;
//		IntMatrix1D c = ((of instanceof SparseIntMatrix1D) ? IntFactory1D.sparse : IntFactory1D.dense).make(n);
//		for (int j = 0; j < n; j++) {
//			c.setQuick(j, -1 * of.getQuick(j));//remember: maximization to minimization
//		}
		
		IntMatrix2D constr = this.getConstraints();//by reference
		
		//for a given <= constraint we have:
		//P = SumPositive - h
		//S = h - SumNegative
		IntFactory1D F1 = IntFactory1D.dense;
		final IntMatrix1D SumPositive = F1.make(mieq);
		final IntMatrix1D SumNegative = F1.make(mieq);
		constr.forEachNonZero(new IntIntIntFunction() {
			public int apply(int i, int j, int cij) {
				if(cij > 0){
					SumPositive.setQuick(i, cij + SumPositive.getQuick(i));
				} else {
					SumNegative.setQuick(i, cij + SumNegative.getQuick(i));
				}
				return cij;
			}
		});

		//for a given <= constraint we have:
		//h = SumPositive - P
		//h = S + SumNegative
		final IntMatrix1D h = F1.make(mieq);
		for (int i = 0; i < mieq; i++) {
			//it is always an inferior constraint
			int hP = SumPositive.getQuick(i) - P.getQuick(i);
			int hS = S.getQuick(i) + SumNegative.getQuick(i);
			if(hP == hS){
				h.setQuick(i, hP);	
			}else{
				throw new IllegalStateException("Inconsistent LOKBA table: " + hP + "!=" + hS);
			}
		}

		return new Object[] { c, constr, h };
	}
	
	/**
	 * This makes a copy (not by reference) of the table.
	 */
	public BIPLokbaTable copyWithAdditionalLEConstraints(int[][] addG, int[] addH) {
		IntFactory2D F2 = (this.constraints instanceof SparseIntMatrix2D) ? IntFactory2D.sparse : IntFactory2D.dense;
		int addMieq = addG.length;

		//copy the old
		final IntMatrix2D newConstraints = F2.make(mieq + addMieq, n);
		final IntMatrix1D newP = IntFactory1D.dense.make(mieq + addMieq);
		final IntMatrix1D newS = IntFactory1D.dense.make(mieq + addMieq);
		this.constraints.forEachNonZero(new IntIntIntFunction() {
			public int apply(int i, int j, int myCij) {
				newConstraints.setQuick(i, j, myCij);
				return myCij;
			}
		});
		for (int i = 0; i < mieq; i++){
			newP.setQuick(i, P.getQuick(i));
			newS.setQuick(i, S.getQuick(i));
		}
		
		//add the new
		for (int i = 0; i < addMieq; i++) {
			int[] Gi = addG[i];
			int p = 0;
			int s = 0;
			for (int j = 0; j < n; j++) {
				int gij = Gi[j];
				newConstraints.setQuick(mieq + i, j, gij);
				p += (gij > 0) ? gij : 0;
				s += (gij < 0) ? gij : 0;
			}
			newP.setQuick(mieq + i, p - addH[i]);
			newS.setQuick(mieq + i, addH[i] - s);
		}
		
		return new BIPLokbaTable(signs, of, newConstraints, newP, newS);
	}
	
	public IntMatrix1D getSigns() {
		return signs;
	}

	public IntMatrix1D getOf() {
		return of;
	}

	public IntMatrix2D getConstraints() {
		return constraints;
	}

	public IntMatrix1D getP() {
		return P;
	}
	
	public void setP(IntMatrix1D newP) {
		this.P = newP;
	}

	public IntMatrix1D getS() {
		return S;
	}

	public void setS(IntMatrix1D newS) {
		this.S = newS;
	}

	public int getN() {
		return n;
	}

	public int getMieq() {
		return mieq;
	}

	@Override
	public String toString() {

		IntMatrix2D[][] parts = {
				{ IntFactory2D.dense.make(new int[][] { this.signs.toArray() }), 
						null, null },
				{ IntFactory2D.dense.make(new int[][] { this.of.toArray() }), 
						null, null },
				{ this.constraints,
						IntFactory2D.dense.make(new int[][] { this.P.toArray() }).viewDice(),
						IntFactory2D.dense.make(new int[][] { this.S.toArray() }).viewDice() } 
		};

		IntMatrix2D comp = IntFactory2D.dense.compose(parts);
		return "lokba table: " + comp.toString();
	}
}
