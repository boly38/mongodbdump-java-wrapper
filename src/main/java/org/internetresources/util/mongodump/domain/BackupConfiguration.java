package org.internetresources.util.mongodump.domain;

import lombok.Data;

@Data
public class BackupConfiguration {
	private static String OS = System.getProperty("os.name").toLowerCase();
	public static final String DEFAULT_BACKUP_DIRECTORY = isWindows() ? "C:\\TMP\\mongoBackup" : "/tmp/mongoBackup";
    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

	String dbName = null;
	String collectionName = null;
	String backupDirectory = DEFAULT_BACKUP_DIRECTORY;
	
	private BackupConfiguration(){};
	
	public static BackupConfiguration getInstance(String dbName) {
		BackupConfiguration conf = new BackupConfiguration();
		conf.dbName = dbName;
		return conf;
	}

	public static BackupConfiguration getInstance(String dbName, String collectionName, String backupDirectory) {
		BackupConfiguration conf = BackupConfiguration.getInstance(dbName);
		conf.collectionName = collectionName;
		if (backupDirectory != null) {
			conf.backupDirectory = backupDirectory;
		}
		return conf;
	}
}
