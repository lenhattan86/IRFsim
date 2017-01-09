package cluster.data;

import cluster.datastructures.Sessions;

public class SessionData {
  public Sessions[] sessionsArray = new Sessions[3];
  public static double scaleFactor = 1.0;
  
  // static data
  // single periodic session
  public static double[] singleStartTimes = {0.0};
  public static double[] singleAlphaDurations = {20.0}; 
  public static double[] singlePeriods  = {200.0};
  public static int[] singleJobNums = {10};
//  public static double[] singleAlphas = {1.0};
  public static double[] singleAlphas = {1.0*scaleFactor};
  
  // multiple periodic sessions
  public static double[] m2pleStartTimes = {0.0, 220.0};
  public static double[] m2pleAlphaDurations = {20.0, 20.0}; 
  public static double[] m2plePeriods  = {100.0, 200.0};
  public static int[] m2pleJobNums = {2, 2};
  public static double[] m2pleAlphas = {1.0*scaleFactor, 1.0*scaleFactor};
  
  // aperiodic sessions
  public static double[] a3StartTimes = {30, 90.0, 250.0, };
  public static double[] a3AlphaDurations = {20.0, 20.0, 20.0}; 
  public static double[] a3Periods  = {0, 0, 0}; // not important
  public static int[]    a3JobNums  = {1, 1, 1}; // always one
  public static double[] a3Alphas   = {1.0*scaleFactor, 1.0*scaleFactor, 1.0*scaleFactor}; // capacity = 1.0;
  
  public SessionData(){
    sessionsArray[0] = new Sessions(singleStartTimes, singleAlphaDurations, singlePeriods, singleJobNums, singleAlphas);
    sessionsArray[1] = new Sessions(m2pleStartTimes, m2pleAlphaDurations, m2plePeriods, m2pleJobNums, m2pleAlphas);
    sessionsArray[2] = new Sessions(a3StartTimes, a3AlphaDurations, a3Periods, a3JobNums, a3Alphas);
  }
  
  public static SessionData SESSION_DATA = new SessionData();
}
