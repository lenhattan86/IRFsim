package cluster.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobQueueList;
import cluster.datastructures.MLJob;
import cluster.datastructures.SubGraph;
import cluster.simulator.Simulator;

public class ProfiledJob {
	String jobName = "";
	String cpuCmd = "";
	String gpuCmd = "";
	String cpuDemand = "";
	String gpuDemand = "";
	int cpuCmpl = 0;
	int gpuCmpl = 0;
	int numBatches = 0;
	
	public static List<ProfiledJob> readJobInfo(String filePath){
		List<ProfiledJob> jobs = new LinkedList<ProfiledJob>();
		File file = new File(filePath);
		int lineIter = 0;
		assert (file.exists() && !file.isDirectory());
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#")) {
					// do nothing
					continue;
				} else {
						ProfiledJob job =  new ProfiledJob();
						job.cpuDemand = line;
						job.gpuDemand = br.readLine();
						
						String temp = br.readLine();
						String[] args = temp.split("=");
						job.numBatches = Integer.parseInt(args[args.length-1]);
						int index = temp.indexOf("--num_batches");
						int len = temp.length();
						job.cpuCmd = temp.substring(0, index);
						
						temp = br.readLine();						
						job.gpuCmd = temp.substring(0, temp.indexOf("--num_batches"));		
						jobs.add(job);
				}
			}
			br.close();
		} catch (Exception e) {
			System.err.println("Catch exception: " + e);
			e.printStackTrace();
		}
		return jobs;
	}
}
