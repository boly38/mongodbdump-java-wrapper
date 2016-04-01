package com.github.boly38.mongodump.services.helpers;

public class HostInfo {

	private static String OS = System.getProperty("os.name").toLowerCase();
    
	public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

}
