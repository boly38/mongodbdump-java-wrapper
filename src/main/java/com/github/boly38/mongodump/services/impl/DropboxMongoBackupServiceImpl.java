package com.github.boly38.mongodump.services.impl;

import java.io.File;
import java.util.List;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.github.boly38.mongodump.domain.BackupConfiguration;
import com.github.boly38.mongodump.domain.BackupException;
import com.github.boly38.mongodump.domain.DeleteException;
import com.github.boly38.mongodump.domain.RestoreConfiguration;
import com.github.boly38.mongodump.domain.RestoreException;
import com.github.boly38.mongodump.domain.hostconf.IMongoServerHostConfiguration;
import com.github.boly38.mongodump.services.contract.DropboxMongoBackupService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class DropboxMongoBackupServiceImpl implements DropboxMongoBackupService {
	private MongodumpServiceImpl mongoDumpService;
	private DropboxServiceImpl dropboxService;
	
	public DropboxMongoBackupServiceImpl(IMongoServerHostConfiguration hostConf) {
		MongodumpServiceImpl mongoDumpSvc = new MongodumpServiceImpl(hostConf);
		this.mongoDumpService = mongoDumpSvc;
		this.dropboxService = new DropboxServiceImpl();
	}

	@Override
	public List<Metadata> listFolder(String folderName) throws ListFolderErrorException, DbxException {
		return dropboxService.listFolder(folderName);
	}
	
	@Override
	public FileMetadata backup(BackupConfiguration backupConf) throws BackupException {
		dropboxService.assumeAvailable();
		String localFileBackup = null;
		try {	
			localFileBackup = mongoDumpService.backup(backupConf);
			FileMetadata dbFile;
			String dropTarget = getDropboxFilename(backupConf);
			try {
				dbFile = dropboxService.uploadFile(localFileBackup, dropTarget);
				log.debug("backup uploaded '{}'", dbFile.getPathLower());
			} catch (Throwable e) {
				String exMsg = String.format("Unable to upload backup '%s' to dropbox : %s", dropTarget, e.getMessage());
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

	@Override
	public String getDropboxFilename(BackupConfiguration backupConf) {
		String remoteDir = backupConf.getBackupRemoteDirectory();
		String backupName = backupConf.getBackupName();
		if (remoteDir == null) {
			throw new IllegalStateException("unable to determine dropbox filename without backup remote directory");
		}
		if (backupName == null) {
			throw new IllegalStateException("unable to determine dropbox filename without backup name");
		}
		if (!remoteDir.endsWith("/")) {
			remoteDir = String.format("%s/", remoteDir);
		}
		if (!backupName.endsWith(".zip")) {
			return String.format("%s%s.zip", remoteDir, backupName);
		}
		return String.format("%s%s", remoteDir, backupName);
	}

	@Override
	public void restore(BackupConfiguration backupConf) throws RestoreException {
		String dropBackup = getDropboxFilename(backupConf);
		String dbName = backupConf.getDbName();
		String collName = backupConf.getCollectionName();
		_restore(dbName, collName, dropBackup);
	}

	private void _restore(String dbName, String collName, String backupFullName) throws RestoreException {
		dropboxService.assumeAvailable();
		String localFileBackup = null;
		try {
			try {
				localFileBackup = dropboxService.downloadFile(backupFullName);
			} catch (Throwable e) {
				String errMsg = String.format("Unable to download backup '%s' : %s", backupFullName, e.getMessage());
				throw new RestoreException(errMsg, e);
			}
			if (localFileBackup == null) {
				String errMsg = String.format("Backup '%s' not found", backupFullName);
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

	@Override
	public void removeBackup(BackupConfiguration backupConf) throws DeleteException {
		String dropTarget = getDropboxFilename(backupConf);
		try {
			log.info("removing dropbox file {}", dropTarget);
			dropboxService.removeFile(dropTarget);
		} catch (Throwable t) {
			String errMsg = String.format("Unable to remove backup '%s' : %s", backupConf, t.getMessage());
			throw new DeleteException(errMsg, t);
		}
	}
	
}
