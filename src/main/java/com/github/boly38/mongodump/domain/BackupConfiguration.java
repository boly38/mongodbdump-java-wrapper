package com.github.boly38.mongodump.domain;

import java.io.File;

import com.github.boly38.mongodump.services.helpers.HostInfo;

import lombok.Data;

@Data
public class BackupConfiguration {
	public static final String DEFAULT_BACKUP_DIRECTORY = 
			HostInfo.isWindows() ? "C:\\TMP\\mongoBackup"
					             : "/tmp/mongoBackup";

	String backupName = null;
	String dbName = null;
	String collectionName = null;
	String backupDirectory = DEFAULT_BACKUP_DIRECTORY;
	String backupRemoteDirectory = "/"; 
	
	private BackupConfiguration(){};
	
	public static BackupConfiguration getInstance(String dbName, String backupName) {
		BackupConfiguration conf = new BackupConfiguration();
		conf.dbName = dbName;
		conf.backupName = backupName;
		return conf;
	}

	public static BackupConfiguration getInstance(String dbName) {
		return getInstance(dbName, dbName);
	}

	public static BackupConfiguration getInstance(String dbName, String collectionName, String backupDirectory) {
		BackupConfiguration conf = BackupConfiguration.getInstance(dbName);
		conf.collectionName = collectionName;
		if (backupDirectory != null) {
			conf.backupDirectory = backupDirectory;
		}
		return conf;
	}

	public String getAbsoluteBackupName() {
		String outDir = backupDirectory != null ? backupDirectory : "./";
		String filename = backupName != null ? backupName : (dbName != null ? dbName : "backup");
		return String.format("%s%s%s.zip", outDir, File.separator, filename);
	}

	public String toString() {
		return String.format("BackupConfiguration[db:%s %s dir:%s file:%s.zip]",
				dbName, collectionName != null ? "coll:"+collectionName : "", backupDirectory, backupName);
	}
}
