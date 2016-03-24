package com.github.boly38.mongodump.domain;

import java.io.File;

import lombok.Data;

@Data
public class MongoServerHostConfiguration {
	String mongodumpBin = "mongodump.exe";
	String mongorestoreBin = "mongorestore.exe";
	String mongoHome = "C:\\Program Files\\MongoDB\\Server\\3.2";

	private String getMongoCommandAbsolutePath(String command) {
		return String.format("%s%s%s%s%s",mongoHome, File.separator, "bin", File.separator, command);
	}
	public String getMongoDumpBinAbsolutePath() {
		return getMongoCommandAbsolutePath(mongodumpBin);
	}
	public String getMongoRestoreBinAbsolutePath() {
		return getMongoCommandAbsolutePath(mongorestoreBin);
	}
}
