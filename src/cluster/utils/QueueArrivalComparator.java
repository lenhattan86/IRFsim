package cluster.utils;

import java.util.Comparator;

import cluster.datastructures.JobQueue;

public class QueueArrivalComparator implements Comparator<JobQueue> {
  
  @Override
  public int compare(JobQueue queue1, JobQueue queue2) {
    int res = 0; 
    if (queue1.getCurrSessionStartTime() > queue2.getCurrSessionStartTime()){
      res = 1;
    }else if (queue1.getCurrSessionStartTime() < queue2.getCurrSessionStartTime()){
      res = -1;
    }
    
    if (res != 0)
      return res;
    
    if (queue1.isLQ && !queue2.isLQ){
      res = 1;
    }else if (!queue1.isLQ && queue2.isLQ){
      res = -1;
    }
    
    if (res!=0)
    	return res;
    return queue1.getQueueName().compareTo(queue2.getQueueName());
  }

}
