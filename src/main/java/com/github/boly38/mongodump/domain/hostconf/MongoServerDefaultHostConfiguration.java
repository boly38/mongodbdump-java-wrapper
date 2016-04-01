package com.github.boly38.mongodump.domain.hostconf;

import java.io.File;

import com.github.boly38.mongodump.services.helpers.HostInfo;

import lombok.Data;

@Data
public class MongoServerDefaultHostConfiguration implements IMongoServerHostConfiguration {
	public static final String WIN_DEFAULT_MONGO_HOME = "C:\\Program Files\\MongoDB\\Server\\3.2";
	public static final String NIX_DEFAULT_MONGO_HOME = " /var/lib/mongodb";

	String mongoHome = HostInfo.isWindows() ? WIN_DEFAULT_MONGO_HOME 
											: NIX_DEFAULT_MONGO_HOME;
	String mongodumpBin = "mongodump";
	String mongorestoreBin = "mongorestore";
	private boolean forceMongoHome = false;

	/**
	 * constructor using default value
	 */
	public MongoServerDefaultHostConfiguration() {
	}

	/**
	 * constructor using custom mongo home directory
	 * @param mongoHome custom mongo home directory
	 * @param forceMongoHome : set to true when installing mongodb from tarball on Unix/Linux systems
	 */
	public MongoServerDefaultHostConfiguration(String mongoHome, boolean forceMongoHome) {
		this.mongoHome = mongoHome;
		this.forceMongoHome  = forceMongoHome;
	}
	
	private String getMongoCommandAbsolutePath(String command) {
		if (HostInfo.isWindows()) {
			return String.format("%s%s%s%s%s.exe",mongoHome, File.separator, "bin", File.separator, command);
		}
		if (forceMongoHome) { // tarball custom install directory use case
			return String.format("%s%s%s%s%s",mongoHome, File.separator, "bin", File.separator, command);
		}
		// suppose binary are in the path of the host
		return command;
	}
	public String getMongoDumpBinAbsolutePath() {
		return getMongoCommandAbsolutePath(mongodumpBin);
	}
	public String getMongoRestoreBinAbsolutePath() {
		return getMongoCommandAbsolutePath(mongorestoreBin);
	}
}
