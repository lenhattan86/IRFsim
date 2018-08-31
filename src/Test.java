import java.io.Writer;

import com.mathworks.engine.*;

public class Test {
	public static void main(String[] args) throws Exception {
		MatlabEngine eng = MatlabEngine.startMatlab();
		
//		double[] a = { 2.0, 4.0, 6.0 };
//		double[] roots = eng.feval("sqrt", a);
//		for (double e : roots) {
//			System.out.println(e);
//		}
				
//		eng.eval("f = [8;1]; intcon = 2;A = [-1,-2; -4,-1;2,1]; b = [14;-33;20]; x = intlinprog(f,intcon,A,b)");
		double[] f = {8,1};
		double[] intcon = {1,2};
		double[][] A = {{-1,-2}, {-4,-1}, {2,1}};
		double[] b = {14,-33,20};
		Object results = eng.feval("intlinprog", MatlabEngine.NULL_WRITER,  MatlabEngine.NULL_WRITER, f, intcon, A, b);
		double[] sols = (double[]) results;
		System.out.println(sols[1]);
		eng.close();
	}
}
