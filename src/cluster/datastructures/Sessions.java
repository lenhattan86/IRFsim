package cluster.datastructures;

import java.util.ArrayList;

public class Sessions {
  private ArrayList<Session> sessions;

  public Sessions() {
    sessions = new ArrayList<Session>();
  }

  public ArrayList<Session> toList() {
    return sessions;
  }

  public Sessions(double[] startTimes, double[][] alphaDurations,
      double[][] periods, int[] jobNums, Resource[][] singleAlphas) {
    sessions = new ArrayList<Session>();
    int len = startTimes.length;
    for (int i = 0; i < len; i++) {
      Session s = new Session(jobNums[i], singleAlphas[i], alphaDurations[i],
          startTimes[i], periods[i]);
      sessions.add(s);
    }
  }
}
