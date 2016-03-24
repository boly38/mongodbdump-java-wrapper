package com.github.boly38.mongodump.services;

import java.io.File;

import com.dropbox.core.v2.files.FileMetadata;
import com.github.boly38.mongodump.domain.BackupConfiguration;
import com.github.boly38.mongodump.domain.BackupException;
import com.github.boly38.mongodump.domain.MongoServerHostConfiguration;
import com.github.boly38.mongodump.domain.RestoreConfiguration;
import com.github.boly38.mongodump.domain.RestoreException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class DropboxMongoBackupService {
	private MongodumpService mongoDumpService;
	private DropboxService dropboxService;
	
	public static DropboxMongoBackupService getInstance(MongoServerHostConfiguration hostConf) {
		DropboxMongoBackupService svc = new DropboxMongoBackupService();
		MongodumpService mongoDumpSvc = MongodumpService.getInstance(hostConf);
		svc.setMongoDumpService(mongoDumpSvc);
		svc.setDropboxService(new DropboxService());
		return svc;
	}

	public FileMetadata backup(BackupConfiguration backupConf) throws BackupException {
		dropboxService.assumeAvailable();
		String localFileBackup = null;
		try {	
			localFileBackup = mongoDumpService.backup(backupConf);
			FileMetadata dbFile;
			try {
				String dropTarget = String.format("/%s.zip", backupConf.getBackupName());
				dbFile = dropboxService.uploadFile(localFileBackup, dropTarget);
				log.debug("backup uploaded '{}'", dbFile.getPathLower());
			} catch (Throwable e) {
				String exMsg = String.format("Unable to upload backup to dropbox : %s", e.getMessage());
				throw new BackupException(exMsg, e);
			}
			return dbFile;
		} catch (BackupException be) {
			throw be;
		} finally {
			if (localFileBackup != null) {
				new File(localFileBackup).delete();
			}
		}
	}

	public void restore(String dbName, String collName, String backupName) throws RestoreException {
		dropboxService.assumeAvailable();
		String localFileBackup = null;
		try {
			String dropSource = String.format("/%s.zip", backupName);
			try {
				localFileBackup = dropboxService.downloadFile(dropSource);
			} catch (Throwable e) {
				String errMsg = String.format("Unable to download backup '%s' : %s", backupName, e.getMessage());
				throw new RestoreException(errMsg, e);
			}
			if (localFileBackup == null) {
				String errMsg = String.format("Backup '%s' not found", backupName);
				throw new RestoreException(errMsg);
			}
			RestoreConfiguration restoreConf = RestoreConfiguration.getInstance(dbName, collName, localFileBackup);
			mongoDumpService.restore(restoreConf);
			log.debug("restore done : {}", restoreConf);
		} catch (RestoreException re) {
			throw re;
		} finally {
			if (localFileBackup != null) {
				new File(localFileBackup).delete();
			}
		}
	}
}
