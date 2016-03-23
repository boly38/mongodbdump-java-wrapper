package org.internetresources.util.mongodump.domain;

import java.io.File;

import lombok.Data;

@Data
public class BackupConfiguration {
	private static String OS = System.getProperty("os.name").toLowerCase();
	public static final String DEFAULT_BACKUP_DIRECTORY = isWindows() ? "C:\\TMP\\mongoBackup" : "/tmp/mongoBackup";
    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

	String backupName = null;
	String dbName = null;
	String collectionName = null;
	String backupDirectory = DEFAULT_BACKUP_DIRECTORY;
	
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
