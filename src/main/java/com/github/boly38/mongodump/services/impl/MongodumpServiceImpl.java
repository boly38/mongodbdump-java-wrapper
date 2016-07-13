package com.github.boly38.mongodump.services.impl;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;

import com.github.boly38.mongodump.domain.BackupConfiguration;
import com.github.boly38.mongodump.domain.BackupException;
import com.github.boly38.mongodump.domain.RestoreConfiguration;
import com.github.boly38.mongodump.domain.RestoreException;
import com.github.boly38.mongodump.domain.hostconf.IMongoServerHostConfiguration;
import com.github.boly38.mongodump.domain.hostconf.OpenshiftHost;
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
		String dbName = backupConf != null ? backupConf.getDbName() : null;
		notEmpty(dbName, "database name is required");
		String mongodumpCmd = getHostConfig().getMongoDumpBinAbsolutePath();
		String collection = backupConf != null ? backupConf.getCollectionName() : null;
		String finalBackupName = backupConf != null ? backupConf.getAbsoluteBackupName() : null;
		if (OpenshiftHost.isOpenshiftHost()) {
			return _openshiftBackupCmd(mongodumpCmd, dbName, collection, finalBackupName);
		}
		return _backupCmd(mongodumpCmd,  dbName, collection, finalBackupName);
	}

	/**
	 * Openshift backup
	 *   - first make a dump into a directory using openshift mongodump binary (mongodump version 2.4.9)
	 *   - second, use zip command to zip this directory to a single file.
	 *   
	 * src: http://stackoverflow.com/questions/27160237/how-to-mongodump-from-openshift-and-mongorestore-locally-on-mongodb-2-4-9
	 * 
	 * @param mongodumpCmd
	 * @param dbName
	 * @param collectionName (optional) if null than all database will be dump
	 * @param finalBackupName
	 * @return
	 * @throws BackupException
	 */
	private String _openshiftBackupCmd(String mongodumpCmd, String dbName, String collectionName, String finalBackupName) throws BackupException {
		String mongoHost = OpenshiftHost.getMongoHostPort();
		String mongoUser = OpenshiftHost.getMongoUsername();
		String mongoPass = OpenshiftHost.getMongoPassword();
		String intermediateFile = finalBackupName != null && finalBackupName.endsWith(".zip") 
				? finalBackupName.substring(0, finalBackupName.length() - 4) 
				: finalBackupName;

		List<String> cmdArgs;
		if (collectionName != null) {
			cmdArgs = Arrays.asList(mongodumpCmd, 
					"--db", dbName, "--collection", collectionName,
					"--host", mongoHost, "--username", mongoUser, "--password", mongoPass,
					"--out", intermediateFile);
		} else {
			cmdArgs = Arrays.asList(mongodumpCmd,
					"--db", dbName, 
					"--host", mongoHost, "--username", mongoUser, "--password", mongoPass,
					"--out", intermediateFile);
		}
		int passSize = mongoPass != null ? mongoPass.length() : 0;
		log.info("openshift backup cmd:{}, db:{} ({}:###{}b@{}) tmp:{} finalZipName:{}", 
				mongodumpCmd, dbName, mongoUser, passSize, mongoHost, intermediateFile, finalBackupName);
		String action = String.format("backup '%s' to '%s'", dbName, intermediateFile);
		try {
			intermediateFile = _hostBackupProcessCommand("mongodump", cmdArgs, intermediateFile);

    		action = String.format("zip backup '%s' to '%s'", intermediateFile, finalBackupName);
    		List<String> zipcmdArgs = null;
    		zipcmdArgs = Arrays.asList("zip","-rj",finalBackupName,intermediateFile);
    		finalBackupName = _hostBackupProcessCommand("zip", zipcmdArgs, finalBackupName);
			return finalBackupName;
		}  catch (Throwable t) {
			String errMsg = String.format("Error during %s : %s", action, t.getMessage());
			log.error(errMsg, t);
			throw new BackupException(errMsg);
		}
	}

	/**
	 * make a dump into a zip file using mongodump binary
	 * @param mongodumpCmd
	 * @param dbName
	 * @param collectionName (optional) if null than all database will be dump
	 * @param finalBackupName
	 * @return
	 * @throws BackupException
	 */
	private String _backupCmd(String mongodumpCmd, String dbName, String collectionName, String finalBackupName) throws BackupException {
		log.info("backup cmd:{}, db:{}, collection:{}, finalZipName:{}",
				mongodumpCmd, dbName, collectionName != null ? collectionName : "(not set)", finalBackupName);
		List<String> cmdArgs;

		String archiveOption = String.format("/archive:%s", finalBackupName);
		if (collectionName != null) {
			cmdArgs = Arrays.asList(mongodumpCmd, archiveOption, "--gzip", "--db", dbName,"--collection", collectionName);
		} else {
			cmdArgs = Arrays.asList(mongodumpCmd, archiveOption, "--gzip", "--db", dbName);
		}
		try {
			return _hostBackupProcessCommand("mongodump", cmdArgs, finalBackupName);
		} catch (Throwable t) {
			String errMsg = String.format("Error during the backup of '%s' : %s", dbName, t.getMessage());
			log.error(errMsg, t);
			throw new BackupException(errMsg);
		}
	}
	
	public String stringArrayToString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String s : list)
		{
			sb.append(s);
		    sb.append(" ");
		}

		return sb.toString();
	}

	private String _hostBackupProcessCommand(String processName, List<String> cmdArgs, String outFileName)
			throws IOException, InterruptedException, BackupException {
		ProcessBuilder builder = new ProcessBuilder(cmdArgs);
		SpyLogs spyLogs = new SpyLogs();
		SpyLogs spyErrorLogs = new SpyLogs();
		int errorId = spyErrorLogs.addSpy("error");
		log.info("{} : {}", processName, stringArrayToString(cmdArgs));
		Process process = builder.start();
		if ("mongodump".equals(processName)) {
			log.info("please notice that mongodump reports all dump action into stderr (not only errors)");
		}
		StreamPrinter fluxErreur = new StreamPrinter(String.format("[%s ERR]", processName), process.getErrorStream(), spyErrorLogs);
		StreamPrinter fluxSortie = new StreamPrinter(String.format("[%s]", processName),     process.getInputStream(), spyLogs);
		fluxErreur.start();
		fluxSortie.start();

		process.waitFor();
		int exitValue = process.exitValue();
		if (exitValue == 0) {
			log.info("created : {}", outFileName);
			return outFileName;
		}
		String errorMsg = null;
		if (!spyErrorLogs.hasSpy(errorId)) {
			errorMsg = spyErrorLogs.getRecorderdSpy(errorId);
		}
		throw new BackupException(exitValue, errorMsg);
	}

	/**
	 * mongorestore a given backup according to restore configuration
	 * @param restoreConf restore configuration
	 * @throws RestoreException
	 */
	public synchronized void restore(RestoreConfiguration restoreConf) throws RestoreException {
		String dbName = restoreConf != null ? restoreConf.getDbName() : null;
		notEmpty(dbName, "database name is required");
		String mongoRestoreCmd = getHostConfig().getMongoRestoreBinAbsolutePath();
		String collection = restoreConf != null ? restoreConf.getCollectionName() : null;
		String backupFile = restoreConf != null ? restoreConf.getBackupFile() : null;
		if (OpenshiftHost.isOpenshiftHost()) {
			_openshiftRestoreCmd(mongoRestoreCmd, dbName, collection, backupFile);
			return;
		}
		_restoreCmd(mongoRestoreCmd,dbName,collection,backupFile);
	}



	/**
	 * mongorestore a given file on openshift gear
	 * @param mongoRestoreCmd
	 * @param dbName
	 * @param collection
	 * @param backupFile
	 * @throws RestoreException 
	 */
	private void _openshiftRestoreCmd(String mongoRestoreCmd, String dbName, String collectionName, String backupFile) throws RestoreException {
		String mongoHost = OpenshiftHost.getMongoHostPort();
		String mongoUser = OpenshiftHost.getMongoUsername();
		String mongoPass = OpenshiftHost.getMongoPassword();
		String openshiftTmpDir = OpenshiftHost.getTmpDir();
		notEmpty(openshiftTmpDir, "OpenShift temp directory is required");
		
		int passSize = mongoPass != null ? mongoPass.length() : 0;

		log.info("openshift restore cmd:{}, db:{} ({}:###{}b@{}) backupFile:{}", 
				mongoRestoreCmd, dbName, mongoUser, passSize, mongoHost, backupFile);
		
		String intermediateDir = backupFile != null 
				&& (backupFile.endsWith(".zip") || backupFile.endsWith(".tmp")) 
				? backupFile.substring(0, backupFile.length() - 4) 
				: String.format("%s/dump", openshiftTmpDir);
		
		String action = String.format("unzip '%s' to '%s'", backupFile, intermediateDir);
		log.info(action);
		try {
    		List<String> unzipcmdArgs = null;
    		unzipcmdArgs = Arrays.asList("unzip", backupFile,"-d", intermediateDir);
    		_hostRestoreProcessCommand("unzip", unzipcmdArgs, intermediateDir);
    		log.info("restore dir created : {}", intermediateDir);
/*
 mongorestore -h $OPENSHIFT_MONGODB_DB_HOST:$OPENSHIFT_MONGODB_DB_PORT -u $OPENSHIFT_MONGODB_DB_USERNAME -p $OPENSHIFT_MONGODB_DB_PASSWORD DM_BACKUP_2016_5_3 -d madem
 */
    		
    		action = String.format("mongorestore '%s'", intermediateDir);
    		List<String> cmdArgs;
    		if (collectionName != null) {
    			cmdArgs = Arrays.asList(mongoRestoreCmd,
    					"--drop", "--db", dbName, "--collection", collectionName,
    					"--host", mongoHost, "--username", mongoUser, "--password", mongoPass,
    					intermediateDir);
    		} else {
    			cmdArgs = Arrays.asList(mongoRestoreCmd,
    					"--db", dbName, 
    					"--host", mongoHost, "--username", mongoUser, "--password", mongoPass,
    					intermediateDir);
    			_hostRestoreProcessCommand("mongorestore", cmdArgs, intermediateDir);
        		log.info("restore directory OK : {}", intermediateDir);
    		}
		}  catch (Throwable t) {
			String errMsg = String.format("Error during %s : %s", action, t.getMessage());
			log.error(errMsg, t);
			throw new RestoreException(errMsg);
		}

	}

	/**
	 * restore from a zip file using mongorestore binary
	 * @param mongoRestoreCmd
	 * @param dbName
	 * @param collection
	 * @param backupFile
	 * @throws RestoreException
	 */
	private void _restoreCmd(String mongoRestoreCmd, String dbName, String collection, String backupFile) throws RestoreException {
		log.info("restore cmd:{}, db:{}, collection:{}, backupFile:{}",
				mongoRestoreCmd, dbName, collection != null ? collection : "(not set)", backupFile);
		
		String archiveOption = String.format("/archive:%s", backupFile);
		List<String> cmdArgs;
		if (collection != null) {
			cmdArgs = Arrays.asList(mongoRestoreCmd, archiveOption, "--gzip", "-v", "--drop", "--db", dbName,"--collection", collection);
		} else {
			cmdArgs = Arrays.asList(mongoRestoreCmd, archiveOption, "--gzip", "-v", "--drop", "--db", dbName);
		}
		try {
			_hostRestoreProcessCommand("mongorestore", cmdArgs,  backupFile);
		} catch (Throwable t) {
			String errMsg = String.format("Error during the restore of '%s' : %s", dbName, t.getMessage());
			log.error(errMsg, t);
			throw new RestoreException(errMsg);
		}
	}

	private void _hostRestoreProcessCommand(String processName, List<String> cmdArgs, String backupFile)
			throws RestoreException, IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(cmdArgs);
		SpyLogs spyLogs = new SpyLogs();
		SpyLogs spyErrorLogs = new SpyLogs();
		int errorId = spyErrorLogs.addSpy("error");
		int failedId = spyErrorLogs.addSpy("Failed");
		log.info("{} : {}", processName, stringArrayToString(cmdArgs));
		Process process = builder.start();
		if ("mongorestore".equals(processName)) {
			log.info("please notice that mongorestore reports all restore action into stderr (not only errors)");
		}
		StreamPrinter fluxErreur = new StreamPrinter(String.format("[%s ERR]", processName), process.getErrorStream(), spyErrorLogs);
		StreamPrinter fluxSortie = new StreamPrinter(String.format("[%s]", processName),     process.getInputStream(), spyLogs);
		fluxErreur.start();
		fluxSortie.start();

		process.waitFor();
		int exitValue = process.exitValue();
		if (exitValue == 0) {
			log.info("{} with success : {}", processName, backupFile);
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
		throw new RestoreException(exitValue, errorMsg);
	}
}
