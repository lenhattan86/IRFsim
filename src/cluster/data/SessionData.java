package cluster.data;

import cluster.datastructures.Session;
import cluster.simulator.Main.Globals;

public class SessionData {
  public Session[] sessionsArray = new Session[1000];
  public static double scaleFactor = 1.0;

  public static double[][] LQAlphasMultipleBB = {
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 },
      { 0.63, 0.99, 0.15, 0.0, 0.66, 0.0 }, };
  public static double[] LQAlphaDurationsBB = { 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, };

  public static double[][] LQAlphasMultipleTPCDS = {
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 },
      { 0.8333, 1.0, 0.25, 0.3333, 0.25, 0.0 }, };

  public static double[] LQAlphaDurationsTPCDS = { 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, };

  public static double[][] LQAlphasMultipleTPCH = {
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 0.7344, 0.9384, 0.4896, 0.0, 0.3672, 0.0 },
      { 1.0, 0.9023, 0.1722, 0.0738, 0.2578, 0.28 }, };

  public static double[] LQAlphaDurationsTPCH = { 23.0, 23.0, 31.0, 23.0, 31.0,
      31.0, 31.0, 23.0, 31.0, 31.0, 31.0, 23.0, 31.0, 23.0, 31.0, 31.0, 23.0,
      23.0, 23.0, 23.0, 23.0, 23.0, 23.0, 31.0, 23.0, 31.0, 23.0, 31.0, 31.0,
      31.0, 31.0, 31.0, 31.0, 23.0, 31.0, 23.0, 31.0, 23.0, 23.0, 31.0, 23.0,
      31.0, 31.0, 31.0, 23.0, 31.0, 31.0, 23.0, 23.0, 23.0, 23.0, 31.0, 23.0,
      31.0, 31.0, 31.0, 23.0, 31.0, 31.0, 31.0, 23.0, 31.0, 23.0, 31.0, 31.0,
      23.0, 23.0, 23.0, 23.0, 23.0, 23.0, 23.0, 31.0, 23.0, 31.0, 23.0, 31.0,
      31.0, 31.0, 31.0, 31.0, 31.0, 23.0, 31.0, 23.0, 31.0, 23.0, 23.0, 31.0,
      23.0, 31.0, 31.0, 31.0, 23.0, 31.0, 31.0, 23.0, 23.0, 23.0, 23.0, 31.0,
      23.0, 31.0, 31.0, 31.0, 23.0, 31.0, 31.0, 31.0, 23.0, 31.0, 23.0, 31.0,
      31.0, 23.0, 23.0, 23.0, 23.0, 23.0, 23.0, 23.0, 31.0, 23.0, 31.0, 23.0,
      31.0, 31.0, 31.0, 31.0, 31.0, 31.0, 23.0, 31.0, 23.0, 31.0, 23.0, 23.0,
      31.0, 23.0, 31.0, 31.0, 31.0, 23.0, 31.0, 31.0, 23.0, 23.0, 23.0, 23.0,
      31.0, };
  // static data
  // LQ-0
  static int LQ0numOfJobs = 150;
  public static double LQ0StartTime = 50.0;
  public static double[] LQ0AlphaDurations = { 27.0, 27.0, 27.0, 27.0, 27.0,
      27.0, 27.0, 27.0, 27.0, 27.0 };
  static double LQ0Period = 200.0;
  public static double[] LQ0Periods = { LQ0Period, LQ0Period, LQ0Period,
      LQ0Period, LQ0Period };

  // LQ-1
  static int LQ1numOfJobs = 150;
  public static double LQ1StartTime = 100.0;
  static double LQ1Period = 150.0;
  public static double[] LQ1Periods = { LQ1Period, LQ1Period, LQ1Period,
      LQ1Period, LQ1Period, LQ1Period, LQ1Period, LQ1Period, LQ1Period,
      LQ1Period, };

  // LQ-2
  static int LQ2numOfJobs = 150;
  public static double LQ2StartTime = 150.0;
  static double LQ2Period = 60.0;
  public static double[] LQ2Periods = { LQ2Period, LQ2Period, LQ2Period,
      LQ2Period, LQ2Period, LQ2Period, LQ2Period, LQ2Period, LQ2Period,
      LQ2Period, LQ2Period, LQ2Period, LQ2Period, LQ2Period, LQ2Period };

  // single LQ
  static int LQnumOfJobs = 150;
  public static double LQStartTime = 100.0;
  static double LQPeriod = 800.0;
  public static double[] LQPeriods = { LQPeriod, LQPeriod, LQPeriod, LQPeriod,
      LQPeriod };

  static int simpleLQnumOfJobs = 150;
  public static double simpleLQStartTime = 200.0;
  static double simpleLQPeriod = 200.0;
  public static double[] simpleLQPeriods = { simpleLQPeriod, simpleLQPeriod,
      simpleLQPeriod, simpleLQPeriod, simpleLQPeriod }; // for 8 TQs

  static int errLQnumOfJobs = 150;
  public static double errLQStartTime = 200.0;
  static double errLQPeriod = 250.0;
  public static double[] errLQPeriods = { errLQPeriod, errLQPeriod, errLQPeriod,
      errLQPeriod, errLQPeriod }; 

  public static double[][] ERROR_10 = {
      { 0.17008, 0.03956, 0.055149, 0.097412, -0.059104, -0.0061762, },
      { -0.21157, 0.064463, 0.16884, -0.023396, -0.043576, -0.095746, },
      { -0.049521, -0.024184, -0.092575, -0.09367, 0.085076, 0.11813, },
      { -0.22231, 0.00061127, -0.0067392, -0.062558, -0.019969, -0.0049912, },
      { -0.10551, 0.067873, 0.0413, 0.058515, 0.12095, 0.02772, },
      { 0.071697, 0.085984, -0.11459, 0.20343, -0.13049, -0.17199, },
      { -0.025893, 0.0085931, -0.16309, -0.071855, 0.05011, -0.047879, },
      { 0.080091, 0.040839, -0.090334, -0.036029, 0.029027, -0.022399, },
      { -0.084683, -0.13889, -0.032089, -0.066033, 0.081002, -0.033619, },
      { 0.049196, -0.13056, 0.043515, -0.023387, 0.047093, 0.27758, },
      { -0.024211, -0.0050866, -0.24218, -0.041751, 0.083164, -0.062555, },
      { -0.029067, 0.085391, 0.07786, 0.022373, -0.074348, 0.16155, },
      { 0.10204, 0.20105, -0.14915, 0.17328, 0.14831, 0.30866, },
      { -0.094949, -0.067915, -0.04607, -0.0086713, 0.02456, 0.011188, },
      { -0.049002, 0.044971, -0.034493, 0.082161, 0.085992, -0.094732, },
      { 0.16421, 0.0029028, -0.082082, 0.11785, -0.069601, -0.15644, },
      { 0.20016, 0.099485, -0.1424, 0.091457, 0.12263, 0.034128, },
      { 0.10225, 0.015555, 0.11842, 0.16344, -0.0013363, -0.097427, },
      { -0.043145, 0.20107, 0.0095567, 0.0374, -0.14816, 0.14229, },
      { -0.25253, 0.10025, 0.1028, -0.13083, 0.0017642, -0.074023, },
      { -0.11546, -0.032031, 0.047557, 0.10765, -0.10604, -0.019816, },
      { -0.059389, -0.022216, -0.015391, -0.11383, -0.16038, -0.016591, },
      { -0.005836, -0.057765, -0.20821, -0.031045, 0.00041917, -0.028297, },
      { -0.027887, -0.0031941, -0.025831, -0.024473, 0.045829, 0.080136, },
      { -0.10333, 0.11867, -0.10223, 0.1397, -0.201, 0.05273, },
      { 0.24342, -0.093371, 0.082541, -0.074689, 0.15408, -0.028777, },
      { -0.15417, 0.10026, -0.02918, 0.047626, 0.067785, -0.13904, },
      { 0.14035, 0.07714, -0.052752, 0.14116, 0.048752, -0.026719, },
      { 0.14791, -0.078595, 0.012251, -0.059571, 0.015612, 0.13271, },
      { 0.084725, 0.084609, -0.10882, -0.062867, 0.22187, -0.038335, },
      { -0.17556, 0.085665, -0.089287, 0.011448, -0.017683, -0.13087, },
      { -0.027287, 0.18872, -0.070185, -0.023401, 0.0026583, 0.049678, },
      { -0.042283, -0.054336, -0.045989, -0.080282, -0.0033437, 0.027422, },
      { 0.046473, -0.14956, 0.052537, 0.042001, -0.00096793, -0.11327, },
      { 0.11589, -0.10737, -0.035649, 0.041429, -0.017312, 0.17155, },
      { -0.11222, 0.054137, 0.034513, 0.10748, 0.018975, 0.031642, },
      { 0.14966, 0.10776, -0.067965, 0.13876, -0.13297, 0.030506, },
      { 0.13807, 0.17684, 0.13789, -0.016327, -0.012396, -0.024828, },
      { -0.15195, -0.12543, -0.062785, 0.079358, 0.019416, -0.13466, },
      { 0.043178, 0.11016, 0.24733, -0.15293, -0.15477, 0.036085, },
      { -0.12328, 0.08842, -0.054827, -0.091052, 0.02111, 0.097347, },
      { 0.10027, -0.050981, -0.0068326, -0.14887, -0.041318, -0.054585, },
      { 0.052144, -0.008876, 0.037525, -0.046727, 0.069204, 0.11731, },
      { -0.039138, 0.24056, 0.082308, 0.083755, -0.18127, 0.1205, },
      { -0.026918, -0.07369, -0.04106, 0.099864, 0.060493, -0.091251, },
      { -0.072543, 0.05717, -0.21912, 0.094641, -0.061026, 0.039024, },
      { -0.14522, -0.045408, 0.19018, 0.18082, -0.04688, -0.089107, },
      { -0.10525, -0.059255, 0.088361, -0.044599, -0.0013425, 0.04259, },
      { -0.044465, 0.18083, -0.063394, -0.11301, 0.020834, 0.023245, },
      { 0.22013, 0.0057394, -0.045897, -0.2525, -0.028673, 0.18894, },
      { -0.13784, 0.037062, -0.13897, 0.02219, -0.16718, 0.11657, },
      { 0.089814, -0.094603, 0.012151, 0.038614, 0.082674, -0.059955, },
      { -0.11697, 0.034804, -0.013061, 0.018475, -0.16764, 0.10518, },
      { -0.013949, -0.14196, 0.024496, -0.049463, -0.015539, 0.029608, },
      { -0.00035356, -0.13403, -0.07026, 0.14437, -0.031303, -0.11929, },
      { -0.11613, 0.018284, -0.17472, -0.10269, 0.010972, 0.0039841, },
      { -0.14848, -0.057627, 0.065737, 0.25833, -0.051785, -0.069646, },
      { 0.039523, 0.092278, -0.036917, 0.010398, -0.01474, 0.043207, },
      { 0.13366, -0.078182, 0.023513, -0.028089, -0.048474, 0.002322, },
      { -0.035323, 0.10159, -0.0068216, 0.10578, -0.037755, -0.10479, },
      { 0.11787, -0.019395, 0.034951, 0.00084834, -0.15898, 0.080512, },
      { 0.0059741, -0.065718, 0.078142, 0.12807, 0.01164, 0.066045, },
      { 0.038059, -0.037906, -0.109, 0.16325, 0.038202, 0.009158, },
      { -0.051667, 0.16922, 0.16152, -0.13001, -0.085186, 0.1094, },
      { -0.23948, 0.071024, -0.0032819, 0.077267, 0.11808, -0.090793, },
      { -0.016208, 0.10987, -0.15559, 0.025329, -0.03088, -0.059964, },
      { -0.068947, 0.14288, 0.14222, 0.11023, 0.17384, 0.017245, },
      { 0.012357, 0.20626, 0.086398, -0.1748, -0.11167, 0.00017222, },
      { 0.027091, 0.11482, 0.030751, 0.11345, -0.23599, 0.048805, },
      { -0.036697, 0.03502, -0.016292, 0.040155, -0.10222, -0.011882, },
      { -0.055446, -0.036236, 0.015044, 0.16855, -0.17202, 0.10848, },
      { 0.044386, 0.12166, -0.13063, -0.20849, 0.036416, 0.19753, },
      { -0.064381, -0.14785, -0.081115, -0.001853, 0.11607, 0.063495, },
      { 0.011266, 0.084448, 0.099914, -0.0094084, -0.035636, 0.0070812, },
      { 0.057466, 0.12788, 0.062968, 0.021717, -0.014543, 0.15633, },
      { -0.032023, -0.0018399, -0.18448, 0.25385, -0.032843, -0.08138, },
      { 0.0634, -0.040394, 0.017764, 0.040248, 0.10489, 0.16182, },
      { -0.024637, -0.11341, -0.029181, 0.16314, 0.1782, 0.069992, },
      { -0.025317, -0.044457, 0.030977, -0.088574, -0.11249, 0.20506, },
      { 0.10189, -0.1539, 0.016943, -0.13198, 0.11737, -0.11105, },
      { 0.045344, 0.0069359, 0.032728, -0.038509, -0.010611, 0.018179, },
      { -0.061996, -0.082287, 0.20991, -0.086279, 0.0091825, 0.083708, },
      { 0.041976, 0.090435, -0.14611, -0.042923, 0.19553, 0.18895, },
      { 0.00016898, -0.098871, 0.10883, 0.053613, -0.095958, -0.16186, },
      { -0.040107, -0.022227, 0.12811, -0.0028782, 0.045472, 0.0067827, },
      { -0.081615, 0.090457, -0.0058113, -0.095728, -0.077322, 0.14435, },
      { -0.040527, -0.05918, -0.16742, 0.1393, -0.087934, 0.11194, },
      { 0.041565, -0.060017, -0.10864, -0.082689, -0.051591, -0.084925, },
      { -0.11859, 0.13294, 0.063313, 0.12464, 0.058021, -0.048356, },
      { 0.031688, 0.0042357, -0.14731, 0.060408, 0.10249, -0.22949, },
      { 0.033995, 0.32649, 0.033383, -0.13387, 0.11775, -0.23198, },
      { 0.036749, -0.0058622, 0.016194, 0.090843, -0.15405, -0.038386, },
      { 0.060179, -0.038886, 0.032348, 0.11253, -0.015314, 0.042829, },
      { 0.028193, 0.063948, 0.17595, 0.050521, 0.22885, -0.011838, },
      { -0.22207, 0.06682, -0.0082486, -0.022167, -0.01523, 0.21274, },
      { 0.054459, 0.041068, 0.077316, 0.046281, -0.087681, 0.015347, },
      { 0.086805, 0.019157, 0.0099797, 0.16027, -0.091408, 0.054954, },
      { 0.19004, 0.031228, 0.32613, 0.074384, 0.075461, 0.013073, },
      { -0.15118, 0.04622, 0.11314, 0.02888, 0.10883, -0.10045, },
      { 0.14398, 0.074974, -0.093113, -0.096913, -0.066391, -0.027883, },
      { -0.06003, 0.064799, -0.11027, -0.071623, 0.061958, -0.024264, },
      { -0.00038753, 0.14771, 0.10534, -0.086459, -0.097645, -0.19624, },
      { -0.12642, 0.0021592, 0.19431, -0.010838, 0.17647, 0.02299, },
      { -0.075207, -0.078796, 0.059668, 0.021462, 0.063992, -0.02356, },
      { 0.028409, -0.036879, 0.013037, 0.049627, 0.055507, 0.056272, },
      { -0.07585, -0.019487, -0.14016, 0.079943, 0.01566, -0.20494, },
      { -0.17452, -0.15778, 0.070163, 0.025911, 0.063484, -0.13647, },
      { 0.086721, -0.03777, -0.037838, 0.12982, -0.19003, 0.15909, },
      { -0.053018, 0.083236, 0.0098404, 0.03091, 0.061171, -0.0093169, },
      { 0.072295, -0.068277, 0.084398, -0.20423, -0.043306, -0.087668, },
      { -0.12639, 0.15201, -0.1885, -0.16125, -0.1707, -0.067808, },
      { 0.0067641, 0.030956, 0.01241, 0.07856, 0.066636, 0.18519, },
      { 0.081664, -0.17521, -0.17008, 0.10245, 0.0094152, 0.027375, },
      { -0.12968, 0.054514, 0.074816, -0.019456, -0.10553, -0.072951, },
      { 0.026189, -0.016737, -0.044353, 0.17244, -0.031158, 0.037859, },
      { -0.04193, 0.063172, -0.025091, -0.1409, 0.096328, -0.01187, },
      { -0.12926, -0.1105, -0.10033, -0.052759, -0.029669, 0.051289, },
      { 0.084225, 0.083734, -0.039504, -0.032024, -0.04439, 0.066457, },
      { 0.27527, 0.0052534, -0.084406, -0.061455, -0.14099, -0.081768, },
      { -0.074027, -0.0039788, 0.0012275, -0.045903, -0.050121, 0.0031958, },
      { 0.10968, -0.053379, -0.061614, -0.077672, 0.030685, 0.01433, },
      { -0.019217, 0.10311, -0.087457, 0.081122, -0.033099, 0.013524, },
      { 0.010693, -0.097447, 0.085298, -0.0033062, -0.061298, -0.03124, },
      { 0.033765, -0.068094, -0.079007, -0.20627, -0.058992, 0.025285, },
      { -0.018737, 0.0026566, 0.029458, 0.05569, 0.096612, 0.058048, },
      { -0.12895, 0.1899, -0.0070021, 0.038319, 0.0074626, 0.0052452, },
      { 0.056017, -0.060415, -0.17322, 0.15861, 0.070559, 0.02287, },
      { 0.04164, -0.10713, 0.014143, 0.023863, -0.055203, -0.052484, },
      { 0.0085945, -0.056405, 0.062458, -0.059152, 0.19324, -0.0086925, },
      { 0.041184, 0.079241, 0.057846, 0.132, 0.15317, -0.19483, },
      { -0.069908, 0.025809, -0.06989, -0.077544, -0.01987, 0.081964, },
      { 0.039918, -0.10387, -0.0039678, -0.047658, 0.15721, 0.15002, },
      { 0.16941, -0.077735, 0.01423, -0.053175, 0.079593, 0.11065, },
      { 0.16309, 0.21169, -0.024251, -0.069305, -0.1088, -0.045456, },
      { -0.041973, 0.075734, 0.053055, 0.036095, -0.27515, 0.11201, },
      { 0.023055, 0.031614, 0.12222, 0.010771, -0.077301, -0.018495, },
      { -0.024685, -0.036317, 0.11126, -0.18361, -0.058226, 0.10323, },
      { 0.026855, 0.031546, -0.14958, 0.043535, 0.030998, -0.014567, },
      { 0.076697, -0.05063, -0.070059, -0.08266, -0.14801, 0.059424, },
      { 0.0064736, 0.082932, -0.020663, -0.00012316, 0.083779, 0.041014, },
      { -0.029181, 0.0086983, -0.1086, -0.047549, -0.056074, -0.21895, },
      { 0.1605, -0.12342, 0.020636, 0.024317, 0.010636, 0.055859, },
      { -0.15222, -0.20971, 0.12406, 0.046877, 0.072953, -0.063542, },
      { -0.099711, 0.1271, -0.047203, -0.076077, 0.11684, 0.016995, },
      { 0.019979, 0.0060956, 0.027042, 0.045544, 0.042468, -0.079497, },
      { 0.098469, 0.0030732, -0.069297, -0.0044431, -0.098541, 0.18261, },
      { -0.038297, -0.048334, 0.08189, -0.058714, 0.10036, 0.051266, },
      { -0.0021897, -0.0028847, -0.11998, -0.0064535, -0.018853, -0.092214, },
      { -0.090678, -0.0006576, -0.0065979, 0.093025, -0.051262, 0.066034, },
      { -0.075416, -0.083371, 0.036909, 0.083222, 0.037875, 0.034832, }, };

  public SessionData() {
    double[][] alphas = LQAlphasMultipleBB;
    double[] alphaDurations = LQAlphaDurationsBB;
    switch (Globals.workload) {
    case BB:
      alphas = LQAlphasMultipleBB;
      alphaDurations = LQAlphaDurationsBB;
      break;
    case TPC_DS:
      alphas = LQAlphasMultipleTPCDS;
      alphaDurations = LQAlphaDurationsTPCDS;
      break;
    case TPC_H:
      alphas = LQAlphasMultipleTPCH;
      alphaDurations = LQAlphaDurationsTPCH;
      break;
    default:
      break;
    }

    if (Globals.runmode.equals(Globals.Runmode.MultipleBurstyQueues)) {
      sessionsArray[0] = new Session(LQ0AlphaDurations.length, alphas,
          alphaDurations, LQ0StartTime, LQ0Period);
      sessionsArray[1] = new Session(LQ1numOfJobs, alphas, alphaDurations,
          LQ1StartTime, LQ1Period);
      sessionsArray[2] = new Session(LQ2numOfJobs, alphas, alphaDurations,
          LQ2StartTime, LQ2Period);

    } else if (Globals.runmode.equals(Globals.Runmode.MultipleBatchQueueRun)) {
      sessionsArray[0] = new Session(LQnumOfJobs, alphas, alphaDurations,
          LQStartTime, LQPeriod);
    } else if (Globals.runmode.equals(Globals.Runmode.AvgTaskDuration)) {
      sessionsArray[0] = new Session(simpleLQnumOfJobs, alphas, alphaDurations,
          simpleLQStartTime, simpleLQPeriod);
    } else if (Globals.runmode.equals(Globals.Runmode.EstimationErrors)) {
      /*
       * for (int i=0; i< alphas.length; i++){ for (int j=0; j<alphas[0].length;
       * j++){ alphas[i][j] = alphas[i][j] +
       * Globals.ESTIMASION_ERRORS*(ERROR_10[i][j]/(0.1))*alphas[i][j]; } }
       */
      sessionsArray[0] = new Session(errLQnumOfJobs, alphas, alphaDurations,
          errLQStartTime, errLQPeriod);
    } else {
      sessionsArray[0] = new Session(LQnumOfJobs, alphas, alphaDurations,
          LQStartTime, LQPeriod);
    }

  }
}
