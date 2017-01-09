package cluster.datastructures;

import cluster.simulator.Main.Globals;

import java.util.logging.Logger;

import com.sun.corba.se.impl.javax.rmi.CORBA.Util;

import cluster.simulator.Main;
import cluster.simulator.Simulator;
import cluster.utils.Utils;

@SuppressWarnings("rawtypes")
public class Resource implements Comparable {

	private static Logger LOG = Logger.getLogger(Resource.class.getName());

	private static final boolean DEBUG = true;
	public double[] resources;
	
	public Resource() {
		resources = new double[Globals.NUM_DIMENSIONS];
	}

	public Resource(double res) {
		resources = new double[Globals.NUM_DIMENSIONS];
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] = Utils.round(res, 2);
		}
	}

	// normalizedCapacity = 1 i.e. cluster capacity

	public Resource(Resource res) {
		resources = new double[Globals.NUM_DIMENSIONS];
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] = Utils.round(res.resource(i), 2);
		}
	}

	public Resource(double[] res) {
		resources = new double[Globals.NUM_DIMENSIONS];
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] = Utils.round(res[i], 2);
		}
	}

	public static double aggrResources(Resource res) {
		double aggr = 0;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			aggr += res.resource(i);
		}
		return aggr;
	}
	
	public void addRes(Resource res) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] += res.resource(i);
		}
	}


	public static double l2Norm(Resource res) {
		double l2Norm = 0;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			l2Norm += Math.pow(res.resource(i), 2);
		}
		l2Norm = Math.sqrt(l2Norm);
		return l2Norm;
	}

	public static double normDouble(double number) {
		return Math.round(number * 100) / 100;
	}

	public static Resource add(Resource res, int val) {
		Resource addedRes = Resources.clone(res);
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			addedRes.resources[i] += val;
			addedRes.resources[i] = Utils.round(addedRes.resources[i], 2);
		}
		return addedRes;
	}
	
	// same as add operation, except we don't cap to 1.0
	public void addWith(Resource res) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] += res.resources[i];
			resources[i] = Utils.round(resources[i], 2);
		}
	}

	public void subtract(Resource res) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] -= res.resources[i];
			resources[i] = Utils.round(Math.max(resources[i], 0), 2);
		}
	}

	public void subtract(double decr) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] -= decr;
			resources[i] = Utils.round(Math.max(resources[i], 0), 2);
		}
	}

	public void multiply(double factor) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] *= factor;
			// resources[i] = Utils.round(resources[i], 2);
		}
	}

	public void divide(double factor) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] /= factor;
			// resources[i] = Utils.round(resources[i], 2);
		}
	}

	public void divide(Resource res) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] /= res.resource(i);
			// resources[i] = Utils.round(resources[i], 2);
		}
	}

	public int resBottleneck() {
		int res_idx_b = -1;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (max < resources[i]) {
				max = resources[i];
				res_idx_b = i;
			}
		}
		return res_idx_b;
	}
	
	public int idOfMaxResource() {
		int maIdx = 0;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (max < resources[i]) {
				max = resources[i];
				maIdx = i;
			}
		}
		return maIdx;
	}

	public double max() {
		double max = Double.MIN_VALUE;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			max = Math.max(max, resources[i]);
		}
		return max;
	}
	
	public int idxOfMax() {
		int idx = 0;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (max < resource(i)){
				idx = i;
				max = resource(i);
			}
		}
		return idx;
	}

	
	public void copy(Resource res) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] = res.resources[i];
		}
	}

	public static double dotProduct(Resource a, Resource b) {
		double score = 0;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (a.resources[i] + (1 - b.resources[i]) > 1.0001) {
				return -1;
			} else {
				score += a.resources[i] * b.resources[i];
			}
		}
		return Utils.round(score, 2);
	}

	public boolean distinctNew(Resource res) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (resources[i] != res.resources[i])
				return true;
		}
		return false;
	}

	public boolean distinct(Resource res) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (resources[i] + .0001 < res.resources[i] || resources[i] > res.resources[i] + .0001)
				return true;
		}
		return false;
	}

	public void normalize() {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (resources[i] < 0)
				resources[i] = 0;
		}
	}

	public boolean greaterOrEqual(Resource res) {
		// every dimension should be at greater or equal
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (resources[i] < res.resource(i))
				return false;
		}
		return true;
	}

	public boolean greater(Resource res) {
		// at least equal in all dimensions
		// at least greater in one dimension
		if (!greaterOrEqual(res)) {
			return false;
		}

		// check that at least one dimension is bigger
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (resources[i] > res.resource(i)) {
				return true;
			}
		}
		return false;
	}

	public boolean smaller(Resource res) {
		// at least equal in all dimensions
		// at least greater in one dimension
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (resources[i] > res.resource(i)) {
				return false;
			}
		}

		// check that at least one dimension is smaller
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (resources[i] < res.resource(i)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean smallerOrEqual(Resource res) {
    // at least equal in all dimensions
    // at least greater in one dimension
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      if (resources[i] > res.resource(i)) {
        return false;
      }
    }

    return true;
  }

	public boolean equal(Resource res) {
		// at least equal in all dimensions
		// at least greater in one dimension
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if ((resources[i] > res.resource(i)) || (resources[i] < res.resource(i))) {
				return false;
			}
		}
		return true;
	}

	public int compareUnclearRes(Resource res) {
		double sum1 = Resource.aggrResources(this);
		double sum2 = Resource.aggrResources(res);
		if (sum2 > sum1)
			return 1;
		if (sum1 > sum2)
			return -1;
		return 0;
	}

	@Override
	public int compareTo(Object o) {
		if (((Resource) o).greater(this)) {
			return 1;
		} else if (((Resource) o).smaller(this)) {
			return -1;
		} else if (((Resource) o).equal(this)) {
			return 0;
		} else {
			return compareUnclearRes((Resource) o);
		}
	}

	@Override
	public String toString() {
		String output = "Resources: [";
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			output += resources[i] + " ";
		}
		output += "]";
		return output;
	}

	public static Resource minRes(Resource left, Resource right) {
		Resource minResources = new Resource();
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			minResources.resources[i] = Math.min(left.resources[i], right.resources[i]);
		}
		return minResources;
	}

	public void addUsage(Resource res) {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			resources[i] += res.resources[i];
		}
	}
	
	public void round(int places){
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			this.resources[i] = Utils.round(this.resources[i],places);
		}
	}
	
	public boolean isEmpty(){
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			if (this.resources[i] >= 1.0)
				return false;
		}
		return true;
	}

	public void floor() {
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			this.resources[i] = Math.floor(this.resources[i]);
		}
	}

	public double sum() {
		double sum=0.0;
		for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
			sum += this.resources[i]; //TODO need to normalize resources
		}
		return sum;
	}
	
  public double resource(int idx) {
    assert (idx >= 0 && idx < Globals.NUM_DIMENSIONS);
    return resources[idx];
  }
}
