package com.github.boly38.mongodump.services.impl;

import java.io.IOException;
import java.security.InvalidParameterException;

import com.github.boly38.mongodump.domain.BackupConfiguration;
import com.github.boly38.mongodump.domain.BackupException;
import com.github.boly38.mongodump.domain.RestoreConfiguration;
import com.github.boly38.mongodump.domain.RestoreException;
import com.github.boly38.mongodump.domain.hostconf.IMongoServerHostConfiguration;
import com.github.boly38.mongodump.domain.logger.SpyLogs;
import com.github.boly38.mongodump.domain.logger.StreamPrinter;
import com.github.boly38.mongodump.services.contract.MongodumpService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * sources used :
 * http://stackoverflow.com/questions/29627424/cannot-run-command-with-processbuilder-or-runexec-inside-eclipse
 *
 */
@Slf4j
@Data
public class MongodumpServiceImpl implements MongodumpService {
	private final IMongoServerHostConfiguration hostConfig;

	/**
	 * Constructor
	 * @param hostConfcurrent host configuration
	 */
	public MongodumpServiceImpl(IMongoServerHostConfiguration hostConf) {
		this.hostConfig = hostConf;
	}
	
	private void notEmpty(String val, String error) {
		if (val == null || val.isEmpty()) {
			throw new InvalidParameterException(error);
		}
	}

	/**
	 * mongodump mongodb according to backupConf
	 * @param backupConf backup configuration
	 * @return just created backup local file
	 * @throws BackupException
	 */
	public synchronized String backup(BackupConfiguration backupConf) throws BackupException {
		String db = backupConf != null ? backupConf.getDbName() : null;
		notEmpty(db, "database name is required");
		String cmd = getHostConfig().getMongoDumpBinAbsolutePath();
		String collection = backupConf != null ? backupConf.getCollectionName() : null;
		String finalBackupName = backupConf != null ? backupConf.getAbsoluteBackupName() : null;
		boolean openshiftMode = System.getenv("OPENSHIFT_MONGODB_DB_URL") != null;
		if (!openshiftMode) {
			return _backupCmd(cmd,  db, collection, finalBackupName);
		}
		return _openshiftBackupCmd(cmd, db, collection, finalBackupName);
	}

	// Openshift backup
	private String _openshiftBackupCmd(String cmd, String db, String collection, String finalBackupName) throws BackupException {
		String mongoHost = String.format("%s:%s", 
				System.getenv("OPENSHIFT_MONGODB_DB_HOST"), 
				System.getenv("OPENSHIFT_MONGODB_DB_PORT"));
		String mongoUser = System.getenv("OPENSHIFT_MONGODB_DB_USERNAME");
		String mongoPass = System.getenv("OPENSHIFT_MONGODB_DB_PASSWORD");
		String intermediateFile = finalBackupName != null && finalBackupName.endsWith(".zip") 
				? finalBackupName.substring(0, finalBackupName.length() - 4) 
				: finalBackupName;

		ProcessBuilder builder = null;
		if (collection != null) {
			builder = new ProcessBuilder(cmd, 
					"--db", db, "--collection", collection,
					"--host", mongoHost, "--username", mongoUser, "--password", mongoPass,
					"--out", intermediateFile);
		} else {
			builder = new ProcessBuilder(cmd,
					"--db", db, 
					"--host", mongoHost, "--username", mongoUser, "--password", mongoPass,
					"--out", intermediateFile);
		}
		int passSize = mongoPass != null ? mongoPass.length() : 0;
		log.info("openshift backup cmd:{}, db:{} ({}:###{}b@{}) tmp:{} finalZipName:{}", 
				cmd, db, mongoUser, passSize, mongoHost, intermediateFile, finalBackupName);
		try {
			SpyLogs spyLogs = new SpyLogs();
			SpyLogs spyErrorLogs = new SpyLogs();
            int errorId = spyErrorLogs.addSpy("error");
			Process process = builder.start();
			log.info("please notice that mongodump reports all dump action into stderr (not only errors)");
            StreamPrinter fluxErreur = new StreamPrinter("[mongodump ERR]", process.getErrorStream(), spyErrorLogs);
            StreamPrinter fluxSortie = new StreamPrinter("[mongodump]",     process.getInputStream(), spyLogs);
            fluxErreur.start();
            fluxSortie.start();
			process.waitFor();

			String errorMsg = null;
            int exitValue = process.exitValue();
            if (exitValue == 0) {
        		log.info("BACKUP created : {}", intermediateFile);
        		ProcessBuilder zipBuilder = new ProcessBuilder("zip","-r",finalBackupName,intermediateFile);
        		Process zipProcess = zipBuilder.start();
                StreamPrinter zipfluxErreur = new StreamPrinter("[zip ERR]", process.getErrorStream(), spyErrorLogs);
                StreamPrinter zipfluxSortie = new StreamPrinter("[zip]",     process.getInputStream(), spyLogs);
                zipfluxErreur.start();
                zipfluxSortie.start();
                zipProcess.waitFor();
                int zipExitValue = zipProcess.exitValue();
                if (zipExitValue == 0) {
                	log.info("BACKUP created : {}", finalBackupName);
                	return finalBackupName;
                }
                if (!spyErrorLogs.hasSpy(errorId)) {
                	errorMsg = spyErrorLogs.getRecorderdSpy(errorId);
                }
                throw new BackupException(exitValue, errorMsg);
            }
            if (!spyErrorLogs.hasSpy(errorId)) {
            	errorMsg = spyErrorLogs.getRecorderdSpy(errorId);
            }
            throw new BackupException(exitValue, errorMsg);
		} catch (IOException e) {
			String errMsg = String.format("Error during the backup of '%s' : %s", db, e.getMessage());
			log.error(errMsg, e);
			throw new BackupException(errMsg);
		} catch (InterruptedException e) {
			String errMsg = String.format("Interruption during the backup of '{}' : {}", db, e.getMessage());
			log.error(errMsg, e);
			throw new BackupException(errMsg);
		}
	}

	// Common case backup
	private String _backupCmd(String cmd, String db, String collection, String finalBackupName) throws BackupException {
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
            StreamPrinter fluxErreur = new StreamPrinter("[mongodump ERR]", process.getErrorStream(), spyErrorLogs);
            StreamPrinter fluxSortie = new StreamPrinter("[mongodump]",     process.getInputStream(), spyLogs);
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
            throw new BackupException(exitValue, errorMsg);
		} catch (IOException e) {
			String errMsg = String.format("Error during the backup of '%s' : %s", db, e.getMessage());
			log.error(errMsg, e);
			throw new BackupException(errMsg);
		} catch (InterruptedException e) {
			String errMsg = String.format("Interruption during the backup of '{}' : {}", db, e.getMessage());
			log.error(errMsg, e);
			throw new BackupException(errMsg);
		}
	}

	/**
	 * mongorestore a given backup according to restore configuration
	 * @param restoreConf restore coniguration
	 * @throws RestoreException
	 */
	public synchronized void restore(RestoreConfiguration restoreConf) throws RestoreException {
		String db = restoreConf != null ? restoreConf.getDbName() : null;
		notEmpty(db, "database name is required");
		String cmd = getHostConfig().getMongoRestoreBinAbsolutePath();
		String collection = restoreConf != null ? restoreConf.getCollectionName() : null;
		String backupFile = restoreConf != null ? restoreConf.getBackupFile() : null;
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
