package org.internetresources.util.mongodump;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

import org.internetresources.util.mongodump.domain.BackupConfiguration;
import org.internetresources.util.mongodump.domain.MongoServerHostConfiguration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * sources used :
 * http://stackoverflow.com/questions/29627424/cannot-run-command-with-processbuilder-or-runexec-inside-eclipse
 *
 */
@Slf4j
@Data
public class MongodumpService {
	private MongoServerHostConfiguration hostConfig;

	private MongodumpService(){}
	
	public static MongodumpService getInstance(MongoServerHostConfiguration hostConf) {
		MongodumpService svc = new MongodumpService();
		svc.setHostConfig(hostConf);
		return svc;
	}
	
	private void notEmpty(String val, String error) {
		if (val == null || val.isEmpty()) {
			throw new InvalidParameterException(error);
		}
	}

	public synchronized void backup(BackupConfiguration backupConf) {
		String db = backupConf != null ? backupConf.getDbName() : null;
		notEmpty(db, "database name is required");
		String cmd = getHostConfig().getMongoDumpBinAbsolutePath();
		String collection = backupConf.getCollectionName();
		String outDir = backupConf.getBackupDirectory();
		_backupCmd(cmd,db,collection,outDir);
	}

	private void _backupCmd(String cmd, String db, String collection, String outDir) {
		log.info("backup cmd:{}, db:{}, collection:{}, out:{}",
				cmd, db, collection != null ? collection : "(not set)", outDir);
		
		
		ProcessBuilder builder;
		if (collection != null) {
			builder = new ProcessBuilder(cmd, "--db", db,"--collection", collection, "--out" ,outDir);
		} else {
			builder = new ProcessBuilder(cmd, "--db", db, "--out" ,outDir);
		}
		builder.inheritIO(); 
		
		try {
			// final Process process = 
			builder.start();
		} catch (IOException e) {
			log.error("Error during the backup of '{}' : {}", db, e.getMessage(), e);
		}

	}

	public synchronized void restore(BackupConfiguration backupConf) {
		String db = backupConf != null ? backupConf.getDbName() : null;
		notEmpty(db, "database name is required");
		String cmd = getHostConfig().getMongoRestoreBinAbsolutePath();
		String collection = backupConf.getCollectionName();
		String backupDir = backupConf.getBackupDirectory();
		_restoreCmd(cmd,db,collection,backupDir);
	}



	/**
	 * "C:\Program Files\MongoDB\Server\3.2\bin\mongorestore.exe" --drop --db dm_dev "C:\TMP\mongoBackup\dm_dev"
	 * @param cmd
	 * @param db
	 * @param collection
	 * @param inputDir
	 */
	private void _restoreCmd(String cmd, String db, String collection, String inputDir) {
		String inputBackupDir = String.format("%s%s%s", inputDir, File.separator, db);
		log.info("restore cmd:{}, db:{}, collection:{}, inputBackupDirectory:{}",
				cmd, db, collection != null ? collection : "(not set)", inputBackupDir);
		
		
		ProcessBuilder builder;
		if (collection != null) {
			builder = new ProcessBuilder(cmd, "--drop", "--db", db,"--collection", collection, inputBackupDir);
		} else {
			builder = new ProcessBuilder(cmd, "--drop", "--db", db, inputBackupDir);
		}
		builder.inheritIO(); 
		
		try {
			// final Process process = 
			builder.start();
		} catch (IOException e) {
			log.error("Error during the restore of '{}' : {}", db, e.getMessage(), e);
		}

	}
}
