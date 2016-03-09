package org.internetresources.util.mongodump;

import java.io.IOException;
import java.security.InvalidParameterException;

import org.internetresources.util.mongodump.domain.BackupConfiguration;
import org.internetresources.util.mongodump.domain.MongoServerHostConfiguration;
import org.internetresources.util.mongodump.domain.RestoreConfiguration;
import org.internetresources.util.mongodump.domain.RestoreException;
import org.internetresources.util.mongodump.domain.SpyLogs;
import org.internetresources.util.mongodump.domain.StreamPrinter;

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

	public synchronized String backup(BackupConfiguration backupConf) throws RestoreException {
		String db = backupConf != null ? backupConf.getDbName() : null;
		notEmpty(db, "database name is required");
		String cmd = getHostConfig().getMongoDumpBinAbsolutePath();
		String collection = backupConf.getCollectionName();
		String finalBackupName = backupConf.getAbsoluteBackupName();
		return _backupCmd(cmd, db, collection, finalBackupName);
	}

	private String _backupCmd(String cmd, String db, String collection, String finalBackupName) throws RestoreException {
		log.info("backup cmd:{}, db:{}, collection:{}, finalZipName:{}",
				cmd, db, collection != null ? collection : "(not set)", finalBackupName);
		
		ProcessBuilder builder;
		String archiveOption = String.format("/archive:%s", finalBackupName);
		if (collection != null) {
			builder = new ProcessBuilder(cmd, archiveOption, "--gzip", "--db", db,"--collection", collection);
		} else {
			builder = new ProcessBuilder(cmd, archiveOption, "--gzip", "--db", db);
		}
		
		try {
			SpyLogs spyLogs = new SpyLogs();
			SpyLogs spyErrorLogs = new SpyLogs();
            int errorId = spyErrorLogs.addSpy("error");
			Process process = builder.start();
			log.info("please notice that mongodump reports all dump action into stderr (not only errors)");
            StreamPrinter fluxErreur = new StreamPrinter("[mongodump.exe ERR]", process.getErrorStream(), spyErrorLogs);
            StreamPrinter fluxSortie = new StreamPrinter("[mongodump.exe]",     process.getInputStream(), spyLogs);
            fluxErreur.start();
            fluxSortie.start();

			process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue == 0) {
        		log.info("BACKUP created : {}", finalBackupName);
            	return finalBackupName;
            }
            String errorMsg = null;
            if (!spyErrorLogs.hasSpy(errorId)) {
            	errorMsg = spyErrorLogs.getRecorderdSpy(errorId);
            }
            throw new RestoreException(exitValue, errorMsg);
		} catch (IOException e) {
			String errMsg = String.format("Error during the backup of '%s' : %s", db, e.getMessage());
			log.error(errMsg, e);
			throw new RestoreException(errMsg);
		} catch (InterruptedException e) {
			String errMsg = String.format("Interruption during the backup of '{}' : {}", db, e.getMessage());
			log.error(errMsg, e);
			throw new RestoreException(errMsg);
		}

	}

	public synchronized void restore(RestoreConfiguration restoreConf) throws RestoreException {
		String db = restoreConf != null ? restoreConf.getDbName() : null;
		notEmpty(db, "database name is required");
		String cmd = getHostConfig().getMongoRestoreBinAbsolutePath();
		String collection = restoreConf.getCollectionName();
		String backupFile = restoreConf.getBackupFile();
		_restoreCmd(cmd,db,collection,backupFile);
	}



	/**
	 * "C:\Program Files\MongoDB\Server\3.2\bin\mongorestore.exe" --drop --db dm_dev "C:\TMP\mongoBackup\dm_dev"
	 * @param cmd
	 * @param db
	 * @param collection
	 * @throws RestoreException 
	 */
	private void _restoreCmd(String cmd, String db, String collection, String backupFile) throws RestoreException {
		log.info("restore cmd:{}, db:{}, collection:{}, backupFile:{}",
				cmd, db, collection != null ? collection : "(not set)", backupFile);
		
		ProcessBuilder builder;
		String archiveOption = String.format("/archive:%s", backupFile);
		if (collection != null) {
			builder = new ProcessBuilder(cmd, archiveOption, "--gzip", "-v", "--drop", "--db", db,"--collection", collection);
		} else {
			builder = new ProcessBuilder(cmd, archiveOption, "--gzip", "-v", "--drop", "--db", db);
		}
		// builder.inheritIO(); 
		
		try {
			SpyLogs spyLogs = new SpyLogs();
			SpyLogs spyErrorLogs = new SpyLogs();
			int errorId = spyErrorLogs.addSpy("error");
			int failedId = spyErrorLogs.addSpy("Failed");
			Process process = builder.start();
			log.info("please notice that mongorestore reports all restore action into stderr (not only errors)");
            StreamPrinter fluxErreur = new StreamPrinter("[mongorestore.exe ERR]", process.getErrorStream(), spyErrorLogs);
            StreamPrinter fluxSortie = new StreamPrinter("[mongorestore.exe]",     process.getInputStream(), spyLogs);
            fluxErreur.start();
            fluxSortie.start();

			process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue == 0) {
        		log.info("restored with success : {}", backupFile);
            	return;
            }
            String errorMsg = null;
            if (!spyErrorLogs.hasSpy(errorId)) {
            	errorMsg = spyErrorLogs.getRecorderdSpy(errorId);
                throw new RestoreException(exitValue, errorMsg);
            }
            if (!spyErrorLogs.hasSpy(failedId)) {
            	errorMsg = spyErrorLogs.getRecorderdSpy(errorId);
                throw new RestoreException(exitValue, errorMsg);
            }
		} catch (IOException e) {
			String errMsg = String.format("Error during the restore of '%s' : %s", db, e.getMessage());
			log.error(errMsg, e);
			throw new RestoreException(errMsg);
		} catch (InterruptedException e) {
			String errMsg = String.format("Interruption during the of of '{}' : {}", db, e.getMessage());
			log.error(errMsg, e);
			throw new RestoreException(errMsg);
		}

	}
}
