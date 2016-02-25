package org.internetresources.util.mongodump.domain;

import lombok.Data;

@Data
public class BackupConfiguration {
	private static String OS = System.getProperty("os.name").toLowerCase();
    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

	String dbName = null;
	String collectionName = null;
	String backupDirectory = isWindows() ? "C:\\TMP\\mongoBackup" : "/tmp/mongoBackup";
	
}
