package com.github.boly38.mongodump.services.contract;

import java.util.List;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.github.boly38.mongodump.domain.BackupConfiguration;
import com.github.boly38.mongodump.domain.BackupException;
import com.github.boly38.mongodump.domain.DeleteException;
import com.github.boly38.mongodump.domain.RestoreException;

public interface DropboxMongoBackupService {
	List<Metadata> listFolder(String folderName) throws ListFolderErrorException, DbxException;
	FileMetadata backup(BackupConfiguration backupConf) throws BackupException;

	String getDropboxFilename(BackupConfiguration backupConf);
	
	void restore(BackupConfiguration backupConf) throws RestoreException;
	
	void removeBackup(BackupConfiguration backupConf) throws DeleteException;
}
