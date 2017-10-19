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

import cern.colt.function.tint.IntIntIntFunction;
import cern.colt.matrix.AbstractMatrix1D;
import cern.colt.matrix.AbstractMatrix2D;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

import com.joptimizer.util.Rational;
import com.joptimizer.util.Utils;


/**
 * Converts a general BIP problem stated in the form (1):
 * <br>min(c) s.t.
 * <br>G.x <= h
 * <br>A.x = b
 * <br>where c, G, h, A, b have double coefficients
 * <br>to the standard form (2)
 * <br>min(c') s.t.
 * <br>G'.x <= b'
 * <br>where c', G', h' have integer coefficients
 * <br>
 * <br>Note 1: (3) it is not exactly the standard LP form (2) because of the more general lower and upper bounds terms.
 * <br>Note 2: if the vector lb is not passed in, all the lower bounds are assumed to be equal to the value of the field <i>unboundedLBValue</i> 
 * <br>Note 3: if the vector ub is not passed in, all the upper bounds are assumed to be equal to the value of the field <i>unboundedUBValue</i>
 */
public class BIPStandardConverter {
	
	private boolean useSparsity;
	private DoubleFactory1D F1;
	private DoubleFactory2D F2;
	private IntFactory1D F1Int;
	private IntFactory2D F2Int;
	private DenseDoubleAlgebra ALG = DenseDoubleAlgebra.DEFAULT;
	private int n = -1;
	private int meq = -1;
	private int mieq = -1;
	private IntMatrix1D standardC = null;
	private IntMatrix2D standardG = null;
	private IntMatrix1D standardH = null;
	
	public BIPStandardConverter(){
		this(true);
	}
	
	public BIPStandardConverter(boolean useSparsity){
		this.useSparsity = useSparsity;
		if(useSparsity){
			F1 = DoubleFactory1D.dense;
			F2 = DoubleFactory2D.sparse;
			F1Int = IntFactory1D.dense;
			F2Int = IntFactory2D.sparse;
		}else{
			F1 = DoubleFactory1D.dense;
			F2 = DoubleFactory2D.dense;
			F1Int = IntFactory1D.dense;
			F2Int = IntFactory2D.dense;
		}
	}
	
	public void toStandardForm(AbstractMatrix1D c, 
			AbstractMatrix2D G, AbstractMatrix1D h, 
			AbstractMatrix2D A, AbstractMatrix1D b) {
		DoubleMatrix1D cDouble = null;
		DoubleMatrix2D GDouble = null;
		DoubleMatrix1D hDouble = null;
		DoubleMatrix2D ADouble = null;
		DoubleMatrix1D bDouble = null;
		IntMatrix1D cInt = null;
		IntMatrix2D GInt = null;
		IntMatrix1D hInt = null;
		IntMatrix2D AInt = null;
		IntMatrix1D bInt = null;
		
		this.n = (int) c.size();
		this.meq = (A != null) ? A.rows() : 0;
		this.mieq = (G != null) ? G.rows() : 0;

		if (c instanceof DoubleMatrix1D) {
			cDouble = (DoubleMatrix1D) c;
		} else {
			cInt = (IntMatrix1D) c;
		}
		if (G != null) {
			if (G instanceof DoubleMatrix2D) {
				GDouble = (DoubleMatrix2D) G;
			} else {
				GInt = (IntMatrix2D) G;
			}
			if (h instanceof DoubleMatrix1D) {
				hDouble = (DoubleMatrix1D) h;
			} else {
				hInt = (IntMatrix1D) h;
			}
		}
		if (A != null) {
			if (A instanceof DoubleMatrix2D) {
				ADouble = (DoubleMatrix2D) A;
			} else {
				AInt = (IntMatrix2D) A;
			}
			if (b instanceof DoubleMatrix1D) {
				bDouble = (DoubleMatrix1D) b;
			} else {
				bInt = (IntMatrix1D) b;
			}
		}
		
		this.toStandardForm(cDouble, GDouble, hDouble, ADouble, bDouble, cInt, GInt, hInt, AInt, bInt);
		
	}
	
	private void toStandardForm(DoubleMatrix1D cDouble, DoubleMatrix2D GDouble, DoubleMatrix1D hDouble,
			DoubleMatrix2D ADouble, DoubleMatrix1D bDouble, IntMatrix1D cInt, IntMatrix2D GInt, IntMatrix1D hInt,
			IntMatrix2D AInt, IntMatrix1D bInt) {
		
		IntMatrix1D cRatio = null;
		IntMatrix2D GRatio = null;
		IntMatrix1D hRatio = null;
		IntMatrix2D ARatio = null;
		IntMatrix1D bRatio = null;
		
		if (cInt == null) {
			cRatio = rationalizeV(cDouble);
		} else {
			cRatio = cInt;
		}
		
		if(this.mieq > 0){
			if(GInt == null){
				if(hInt == null){
					Object[] rr = rationalizeMv(GDouble, hDouble);
					GRatio = (IntMatrix2D) rr[0];
					hRatio = (IntMatrix1D) rr[1];
				}else{
					Object[] rr = rationalizeMv(GDouble, hInt);
					GRatio = (IntMatrix2D) rr[0];
					hRatio = (IntMatrix1D) rr[1];
				}
			}else{
				if(hInt == null){
					Object[] rr = rationalizeMv(GInt, hDouble);
					GRatio = (IntMatrix2D) rr[0];
					hRatio = (IntMatrix1D) rr[1];
				}else{
					GRatio = GInt;
					hRatio = hInt;
				}
			}
		}
		
		if(this.meq > 0){
			if(AInt ==null){
				if(bInt == null){
					Object[] rr = rationalizeMv(ADouble, bDouble);
					ARatio = (IntMatrix2D) rr[0];
					bRatio = (IntMatrix1D) rr[1];
				}else{
					Object[] rr = rationalizeMv(ADouble, bInt);
					ARatio = (IntMatrix2D) rr[0];
					bRatio = (IntMatrix1D) rr[1];
				}
			}else{
				if(bInt == null){
					Object[] rr = rationalizeMv(AInt, bDouble);
					ARatio = (IntMatrix2D) rr[0];
					bRatio = (IntMatrix1D) rr[1];
				}else{
					ARatio = AInt;
					bRatio = bInt;
				}
			}
		}
		
		final IntMatrix2D AG = F2Int.make(2 * meq + mieq, n);
		final IntMatrix1D bh = F1Int.make(2 * meq + mieq);
		
		if(this.meq > 0){
			ARatio.forEachNonZero(new IntIntIntFunction() {
				public int apply(int i, int j, int Aij) {
					AG.setQuick(i, j, Aij);//A.x <= b
					AG.setQuick(i + meq, j, -Aij);//-A.x <= -b
					return Aij;
				}
			});
			
			for(int i=0; i<meq; i++){
				bh.setQuick(i, bRatio.getQuick(i));//A.x <= b
				bh.setQuick(i + meq, -bRatio.getQuick(i));//-A.x <= -b
			}
		}
		
		if(this.mieq > 0){
			GRatio.forEachNonZero(new IntIntIntFunction() {
				public int apply(int i, int j, int Gij) {
					AG.setQuick(2 * meq + i, j, Gij);
					return Gij;
				}
			});
			
			for(int i=0; i<mieq; i++){
				bh.setQuick(2 * meq + i, hRatio.getQuick(i));
			}
		}
		
		this.standardC = cRatio;
		this.standardG = AG;
		this.standardH = bh;
	}
	
	private IntMatrix1D rationalizeV(DoubleMatrix1D v){
		int myN = (int) v.size();
		IntMatrix1D ret = F1Int.make(myN);
		Rational rational = null;
		Rational[] rArray = new Rational[myN];
		int[] lcmArray = new int[myN];// Least Common Multiple
		for (int i = 0; i < myN; i++) {
			rational = new Rational(v.getQuick(i));
			rArray[i] = rational;
			lcmArray[i] = rational.getDenom();
		}
		int lcm = Utils.lcm(lcmArray);

		for (int j = 0; j < myN; j++) {
			int cj = rArray[j].getNum() * (lcm / rArray[j].getDenom());
			ret.setQuick(j, cj);
		}	
		
		return ret;
	}
	
	private Object[] rationalizeMv(DoubleMatrix2D M, DoubleMatrix1D v){
		int rows = M.rows();
		int cols = M.columns();
		int[] lcmArray = new int[cols];// Least Common Multiple
		IntMatrix2D ret0 = F2Int.make(rows, cols);
		IntMatrix1D ret1 = F1Int.make(rows);
		Rational rational = null;
		Rational[] rArray = null;
		for (int i = 0; i < rows; i++) {
			rArray = new Rational[cols + 1];
			lcmArray = new int[cols + 1];
			for (int j = 0; j < cols; j++) {
				rational = new Rational(M.getQuick(i, j));
				rArray[j] = rational;
				lcmArray[j] = rational.getDenom();
			}
			rational = new Rational(v.getQuick(i));
			rArray[cols] = rational;
			lcmArray[cols] = rational.getDenom();

			int lcm = Math.abs(Utils.lcm(lcmArray));
			for (int j = 0; j < cols; j++) {
				ret0.setQuick(i, j, Utils.checkBounds(rArray[j].getNum() * (lcm / rArray[j].getDenom())));
			}
			ret1.setQuick(i, Utils.checkBounds(rArray[cols].getNum() * (lcm / rArray[cols].getDenom())));
		}
		
		return new Object[]{ret0, ret1};
	}
	
	private Object[] rationalizeMv(IntMatrix2D M, DoubleMatrix1D v){
		int rows = M.rows();
		int cols = M.columns();
		int[] lcmArray = new int[cols];// Least Common Multiple
		IntMatrix2D ret0 = F2Int.make(rows, cols);
		IntMatrix1D ret1 = F1Int.make(rows);
		Rational rational = null;
		Rational[] rArray = null;
		for (int i = 0; i < rows; i++) {
			rArray = new Rational[1];
			lcmArray = new int[1];
			rational = new Rational(v.getQuick(i));
			rArray[0] = rational;
			lcmArray[0] = rational.getDenom();

			int lcm = Math.abs(Utils.lcm(lcmArray));
			for (int j = 0; j < cols; j++) {
				ret0.setQuick(i, j, Utils.checkBounds(M.getQuick(i, j) * (lcm)));
			}
			ret1.setQuick(i, Utils.checkBounds(rArray[0].getNum()));
		}
		
		return new Object[]{ret0, ret1};
	}

	private Object[] rationalizeMv(DoubleMatrix2D M, IntMatrix1D v){
		int rows = M.rows();
		int cols = M.columns();
		int[] lcmArray = new int[cols];// Least Common Multiple
		IntMatrix2D ret0 = F2Int.make(rows, cols);
		IntMatrix1D ret1 = F1Int.make(rows);
		Rational rational = null;
		Rational[] rArray = null;
		for (int i = 0; i < rows; i++) {
			rArray = new Rational[cols];
			lcmArray = new int[cols];
			for (int j = 0; j < cols; j++) {
				rational = new Rational(M.getQuick(i, j));
				rArray[j] = rational;
				lcmArray[j] = rational.getDenom();
			}

			int lcm = Math.abs(Utils.lcm(lcmArray));
			for (int j = 0; j < cols; j++) {
				ret0.setQuick(i, j, Utils.checkBounds(rArray[j].getNum() * (lcm / rArray[j].getDenom())));
			}
			ret1.setQuick(i, Utils.checkBounds(v.getQuick(i) * (lcm)));
		}
		
		return new Object[]{ret0, ret1};
	}

	
	public IntMatrix1D getStandardC() {
		return standardC;
	}
	
	public IntMatrix2D getStandardG() {
		return standardG;
	}

	public IntMatrix1D getStandardH() {
		return standardH;
	}

	
}
