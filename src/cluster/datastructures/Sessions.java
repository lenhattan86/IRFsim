package cluster.datastructures;

import java.util.ArrayList;

public class Sessions {
  private ArrayList<Session> sessions;
  
  public Sessions(){
    sessions = new ArrayList<Session>();
  }
  
  public ArrayList<Session> toList(){
    return sessions;
  }
  
  public Sessions(double[] startTimes,
      double[] alphaDurations, double[] periods, int[] jobNums,
      double[] singleAlphas) {
    sessions = new ArrayList<Session>();
    int len = startTimes.length;
    for (int i = 0; i < len; i++) {
      Resource alpha = new Resource(singleAlphas[i]);
      boolean isPeriodic = periods[0]>0.0?true:false;      
      Session s = new Session(jobNums[i], alpha, alphaDurations[i],
          startTimes[i], periods[i],isPeriodic);
      sessions.add(s);
    }
  }
}
