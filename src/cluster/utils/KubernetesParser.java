package cluster.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class KubernetesParser {
	public static void main(String[] args) {
		String[] INPUT_FOLDER ={"/home/tanle/projects/IRFsim/src/cluster/utils/"};
		String inputFile = "scheduler.log";
		String[] OUTPUT_FOLDER = {"/home/tanle/projects/IRFsim/src/cluster/utils/"};
		String outputFile = "fairness.csv";
		int i = 0;
		for (String input : INPUT_FOLDER){
			parseSchedulerLog(INPUT_FOLDER[i] + inputFile, OUTPUT_FOLDER[i] + outputFile);
			i++;
		}
	}
	
	public static void parseSchedulerLog(String filepath, String output){
		File file = new File(filepath);
		assert (file.exists() && !file.isDirectory());
		int jobNum = 0;
		String toWrite = "";
		Output.write(toWrite, false, output);
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			int iStep = 0;
			while ((line = br.readLine()) != null) {
				if (line.contains("FairScoreMap:")){
					line = line.trim();
					String strTime = line.split(" ")[1];
					String subString = line.substring(line.indexOf("[user"));
					subString = subString.substring(1, subString.length()-1);
					String[] args = subString.split(" ");
					int numOfUser = args.length;
					toWrite = strTime;
					iStep++;
					for (int i=0; i< numOfUser; i++) {
						String stringUser = "user"+(i+1);
						for(int j=0; j<numOfUser; j++){
							if(args[j].contains(stringUser)){
								toWrite = strTime;
								toWrite += "," + iStep;
								toWrite += "," + args[j].split(":")[0];
								toWrite += "," + args[j].split(":")[1];
								Output.writeln(toWrite, true, output);
							}
						}
					}
				}
			}
			br.close();
		} catch (Exception e) {
			System.err.println("Catch exception: " + e);
			e.printStackTrace();
		}
	}
}
