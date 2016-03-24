package com.github.boly38.mongodump.domain;

import lombok.Data;

@Data
public class RestoreConfiguration {
	String dbName = null;
	String collectionName = null;
	String backupFile = null;

	public static RestoreConfiguration getInstance(BackupConfiguration backupConf) {
		RestoreConfiguration conf = new RestoreConfiguration();
		conf.dbName = backupConf.getDbName();
		conf.collectionName = backupConf.getCollectionName();
		conf.backupFile = backupConf.getAbsoluteBackupName();
		return conf;
	}

	public static RestoreConfiguration getInstance(String dbName, String backupFilename) {
		RestoreConfiguration conf = new RestoreConfiguration();
		conf.dbName = dbName;
		conf.backupFile = backupFilename;
		return conf;
	}

	public static RestoreConfiguration getInstance(String dbName, String collName, String backupFilename) {
		RestoreConfiguration conf = new RestoreConfiguration();
		conf.dbName = dbName;
		conf.collectionName = collName;
		conf.backupFile = backupFilename;
		return conf;
	}

	public String toString() {
		return String.format("RestoreConfiguration[db:%s %s file:%s]",
				dbName, collectionName != null ? "coll:"+collectionName : "", backupFile);
	}
}
