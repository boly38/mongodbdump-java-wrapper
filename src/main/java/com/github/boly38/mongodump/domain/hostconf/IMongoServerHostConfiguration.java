package com.github.boly38.mongodump.domain.hostconf;

public interface IMongoServerHostConfiguration {
	String getMongoDumpBinAbsolutePath();
	String getMongoRestoreBinAbsolutePath();
}
