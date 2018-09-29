package cluster.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.datastructures.BaseJob;
import cluster.datastructures.MLJob;

public class ConvertToExperimentInput {
	public static void main(String[] args) {
		int numberOfUsers = 4;
		int numberOfProfiledFiles = 3;
		String traceFile = "/ssd/projects/IRFevaluation/awscloudlab/eurosys3.0/automation_prof/sample_experiment.txt";
		String profileFolder = "/ssd/projects/IRFimpl/experiments/traces/profiling";
		String outputFolder = "/ssd/projects/IRFimpl/experiments/traces/large/";
		String profileDataCsv = "/ssd/projects/IRFevaluation/awscloudlab/eurosys3.0/automation_prof/fullJobs.csv";
		Queue<BaseJob> tracedJobs = MLJob.readDags(traceFile);
		for (int i = 0; i<numberOfUsers ; i++){
			String toWrite = "# Generated workload for user"+(i+1);
			Output.writeln(toWrite, false, outputFolder + "/user"+(i+1)+".txt");
		}
		
		List<List<ProfiledJob>>  profiledFiles = new LinkedList<List<ProfiledJob>>();
		for (int i=0; i<numberOfProfiledFiles; i++){
		  List<ProfiledJob> profileFile = new LinkedList<ProfiledJob>();
		  profileFile = ProfiledJob.readJobInfo(profileFolder + "/user"+(i+1)+".txt");
			profiledFiles.add(profileFile);
		}
		
	  List<ProfiledJob> profileData = new LinkedList<ProfiledJob>();
	  File file = new File(profileDataCsv);
		assert (file.exists() && !file.isDirectory());
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				String[] temp = line.trim().split(",");
				ProfiledJob profiledItem =  new ProfiledJob();
				profiledItem.jobName  = temp[1];
				profiledItem.cpuCmpl = (int)(Float.parseFloat(temp[5]));
				profiledItem.gpuCmpl = (int)(Float.parseFloat(temp[6]));
				profileData.add(profiledItem);
			}
			br.close();
		} catch (Exception e) {
			System.err.println("Catch exception: " + e);
			e.printStackTrace();
		}
		
		
		for (BaseJob job:tracedJobs){
			int fileId = Integer.parseInt(job.profileJobName.substring(4, 5)); 
			int jobId = Integer.parseInt(job.profileJobName.split("-")[1]);			
			ProfiledJob profile = profiledFiles.get(fileId).get(jobId);			
			int userId = Integer.parseInt(job.getQueueName().substring(5,6))+1;
			String fileToWrite = outputFolder + "user"+userId+ ".txt";
			
			ProfiledJob jData = getJob(profileData, fileId, jobId);
			
			double scale1 = job.getDemand().cpuCompl/jData.cpuCmpl;
			
			if (Math.abs((scale1 - job.profileJobScale)/scale1) > 0.1)
				System.err.println("Wrong cpu compl. scale at job " + job.dagId);
			double scale2 = job.getDemand().gpuCompl/jData.gpuCmpl;
			if (Math.abs((scale2 - job.profileJobScale)/scale2) > 0.1)
				System.err.println("Wrong cpu compl. scale at job " + job.dagId);
			if (Math.abs((scale2 - scale1)/scale2) > 0.1)
				System.err.println("Wrong cpu compl. scale at job " + job.dagId);
			double scale = (scale1+scale2)/2;
			String[] temp = profile.cpuDemand.split(" "); 
			profile.cpuDemand = temp[0]+" " + temp[1]+" " + temp[2]+ " " + (int)(jData.cpuCmpl*scale); 
			Output.writeln(profile.cpuDemand, true, fileToWrite);
			temp = profile.gpuDemand.split(" ");
			profile.gpuDemand = temp[0]+" " + temp[1]+" " + temp[2]+ " " + (int)(jData.gpuCmpl*scale);
			Output.writeln(profile.gpuDemand, true, fileToWrite);
			
			int numBatches =  -1;
			
			if (job.profileJobScale > 0)
				numBatches = (int) (profile.numBatches*job.profileJobScale);
			else 
				numBatches = (int) (profile.numBatches*scale);
			
			Output.writeln(profile.cpuCmd+"--num_batches="+numBatches, true, fileToWrite);
			Output.writeln(profile.gpuCmd+"--num_batches="+numBatches, true, fileToWrite);
		}
	}
	
	public static ProfiledJob getJob(List<ProfiledJob> profileData, int userId, int jobId){
		for (ProfiledJob data:profileData){
			String jobName = "user"+userId + "-"+ jobId;
			if (data.jobName.equals(jobName)){
				return data;
			}
		}
		return null;
	}
}
