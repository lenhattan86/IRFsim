package cluster.utils;

import java.io.FileWriter;
import java.io.IOException;

import cluster.simulator.Main.Globals;

public class Output {
	public static void write(String toWrite, boolean append){
		toWrite=toWrite + "\n";
		try {
			FileWriter file = new FileWriter(Globals.FileOutput, append);
			file.write(toWrite.toCharArray(), 0, toWrite.length());
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeln(String toWrite){
		toWrite=toWrite + "\n";
		try {
			FileWriter file = new FileWriter(Globals.FileOutput, true);
			file.write(toWrite.toCharArray(), 0, toWrite.length());
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void write(String toWrite){
		try {
			FileWriter file = new FileWriter(Globals.FileOutput, true);
			file.write(toWrite.toCharArray(), 0, toWrite.length());
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void debugln(String toWrite){
		if(Globals.DEBUG){
			System.out.println(toWrite);
			writeln(toWrite);
		}
	}
	
	public static void debugln(boolean debug, String toWrite){
		if(debug){
			System.out.println(toWrite);
			writeln(toWrite);
		}
	}
	
	public static void debug(String toWrite){
		if(Globals.DEBUG){
			System.out.print(toWrite);
			write(toWrite);
		}
	}
	
	public static void debug(boolean debug, String toWrite){
		if(debug){
			System.out.print(toWrite);
			write(toWrite);
		}
	}
}
