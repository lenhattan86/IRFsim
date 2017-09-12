package cluster.datastructures;

import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.Utils;

public class Resources {
  
  public static Resource ZEROS = new Resource(0.0);
  
  public static Resource initResources(boolean isNormalized, double size) {
    Resource res = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      double scale = 1.0;
      if (isNormalized)
        scale = Simulator.cluster.getClusterMaxResAlloc().resource(i);
      double resSize = size * scale;
      res.resources[i] = Utils.roundDefault(resSize);
    }
    return res;
  }
  
  public static Resource min(Resource a, Resource b) {
    if (a.greaterOrEqual(b))
      return Resources.clone(b);
    return Resources.clone(a);
  }
  
  public static Resource piecewiseMin(Resource a, Resource b) {
    Resource ret = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      ret.resources[i] = Math.min(a.resources[i], b.resources[i]);
    }
    return ret;
  }
  
  public static Resource piecewiseMax(Resource a, Resource b) {
    Resource ret = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      ret.resources[i] = Math.max(a.resources[i], b.resources[i]);
    }
    return ret;
  }


  public static Resource subtract(Resource total, Resource decr) {
    Resource subtractedRes = new Resource(0.0);
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      subtractedRes.resources[i] = Utils.roundDefault(total.resources[i] - decr.resources[i]);
    }
    return subtractedRes;
  }

  public static Resource subtractPositivie(Resource total, Resource decr) {
    Resource subtractedRes = new Resource(0.0);
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      subtractedRes.resources[i] = Math.max(Utils.roundDefault(total.resources[i] - decr.resources[i]), 0);
    }
    return subtractedRes;
  }

  public static Resource clone(Resource res) {
    Resource clonedRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      clonedRes.resources[i] = res.resources[i];
    }
    return clonedRes;
  }


  public static Resource divide(Resource res, int factor) {
    assert (factor > 0);
    Resource normalizedRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      normalizedRes.resources[i] = Utils.roundDefault(res.resources[i] / factor);
    }
    return normalizedRes;
  }
  
  public static Resource divideVector(Resource res, Resource denom) {
    Resource normalizedRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      normalizedRes.resources[i] = Utils.roundDefault(res.resources[i] / denom.resource(i));
    }
    return normalizedRes;
  }
  
  public static Resource divide(Resource res, double factor) {
    assert (factor > 0);
    Resource normalizedRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      normalizedRes.resources[i] = Utils.roundDefault(res.resources[i] / factor);
    }
    return normalizedRes;
  }
  
  public static Resource divideNoRound(Resource res, double factor) {
    assert (factor > 0);
    Resource normalizedRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      normalizedRes.resources[i] = res.resources[i] / factor;
    }
    return normalizedRes;
  }
  
  public static Resource multiply(Resource res, int factor) {
    assert (factor > 0);
    Resource normalizedRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      normalizedRes.resources[i] = Utils.roundDefault(res.resources[i] * factor);
    }
    return normalizedRes;
  }
  
  public static Resource multiply(Resource res, double factor) {
    assert (factor > 0);
    Resource normalizedRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      normalizedRes.resources[i] = Utils.roundDefault(res.resources[i] * factor);
    }
    return normalizedRes;
  }
  
  public static Resource sum(Resource res, Resource addedRes) {
    Resource newRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      newRes.resources[i] = res.resources[i] + Utils.roundDefault(addedRes.resources[i]);
    }
    return newRes;
  }
  
  public static Resource sumRound(Resource res, Resource addedRes) {
    Resource newRes = new Resource();
    for (int i = 0; i < Globals.NUM_DIMENSIONS; i++) {
      newRes.resources[i] = Utils.roundDefault(res.resources[i] + addedRes.resources[i]);
    }
    return newRes;
  }
  

}
