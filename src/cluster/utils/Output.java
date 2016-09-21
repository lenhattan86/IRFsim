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
	
	public static void debug(String toWrite){
		if(Globals.DEBUG_ALL){
			System.out.print(toWrite);
		}
	}
	
	public static void debugln(String toWrite){
		if(Globals.DEBUG_ALL){
			System.out.println(toWrite);
		}
	}
	
	public static void debugln(boolean DEBUG, String toWrite){
		if(Globals.DEBUG_ALL || (DEBUG && Globals.DEBUG_LOCAL)){
			System.out.println(toWrite);
		}
	}
	
	public static void debugln(boolean DEBUG){
		debugln(DEBUG, "");
	}
	
	public static void debug(boolean DEBUG, String toWrite){
		if(Globals.DEBUG_ALL || (DEBUG && Globals.DEBUG_LOCAL)){
			System.out.print(toWrite);
		}
	}
	
	public static void debug(boolean DEBUG){
		debug(DEBUG, "");
	}
}
