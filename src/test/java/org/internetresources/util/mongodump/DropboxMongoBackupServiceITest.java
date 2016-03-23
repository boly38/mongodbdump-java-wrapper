package org.internetresources.util.mongodump;

import org.fest.assertions.api.Assertions;
import org.internetresources.util.mongodump.domain.BackupConfiguration;
import org.internetresources.util.mongodump.domain.BackupException;
import org.internetresources.util.mongodump.domain.MongoServerHostConfiguration;
import org.internetresources.util.mongodump.domain.RestoreException;
import org.junit.Test;

import com.dropbox.core.v2.files.FileMetadata;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DropboxMongoBackupServiceITest {
	final String TEST_DATABASE_NAME = "myDB";
	final String TEST_BACKUP_NAME   = "myBackup";

	private MongoServerHostConfiguration hostConf = new MongoServerHostConfiguration();
	
	private BackupConfiguration getBackupConfiguration() {
		return BackupConfiguration.getInstance(TEST_DATABASE_NAME, TEST_BACKUP_NAME);
	}

	@Test
	public void should_backup() throws BackupException {
		// GIVEN
		DropboxMongoBackupService dbmSvc = DropboxMongoBackupService.getInstance(hostConf);
		BackupConfiguration backupConf = getBackupConfiguration();
		// WHEN
		FileMetadata backupFile = dbmSvc.backup(backupConf);
		// THEN
		Assertions.assertThat(backupFile).isNotNull();
		log.info("backup file created : {}", backupFile.getPathLower());
	}


	@Test
	public void should_restore() throws RestoreException {
		// GIVEN
		DropboxMongoBackupService dbmSvc = DropboxMongoBackupService.getInstance(hostConf);
		log.info("restore from : {}", TEST_BACKUP_NAME);		
		// WHEN
		dbmSvc.restore(TEST_DATABASE_NAME, null, TEST_BACKUP_NAME);
		// THEN
		// THEN
		// IMPROVEMENT : check mongo content
	}
}
