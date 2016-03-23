package org.internetresources.util.mongodump;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;

import org.internetresources.util.mongodump.domain.BackupConfiguration;
import org.internetresources.util.mongodump.domain.BackupException;
import org.internetresources.util.mongodump.domain.MongoServerHostConfiguration;
import org.internetresources.util.mongodump.domain.RestoreConfiguration;
import org.internetresources.util.mongodump.domain.RestoreException;
import org.junit.Assume;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Please take a snapshot of your data before using this tests.
 *
 */
@Slf4j
public class MongodumpServiceITest {

	final String TEST_DATABASE_NAME = "myDB";
	final String TEST_BACKUP_NAME   = "myBackup";

	private MongoServerHostConfiguration hostConf = new MongoServerHostConfiguration();

	// THEN
    public boolean fileExists(String fileFullName) {
        File targetFile = new File(fileFullName);
        return targetFile.exists();
    }

    // THEN
    public void assertFile(String fileFullName) {
        boolean expectedFileExists = fileExists(fileFullName);
        assertThat(expectedFileExists).as(" file " + fileFullName + " should exists").isTrue();
    }
	
    // GIVEN
	private void assumeFileExecutable(String filePath, String assumeError) {
		File f = new File(filePath);
		boolean fIsExecutable = f.exists() && f.canExecute();
		Assume.assumeTrue(assumeError, fIsExecutable);
		
	}

	// GIVEN
	private void assumeHostIsReadyForTest() {
		String mongodumpBin = hostConf.getMongoDumpBinAbsolutePath();
		String mongorestoreBin = hostConf.getMongoRestoreBinAbsolutePath();
		assumeFileExecutable(mongodumpBin, 
				String.format("mongodump binary '%s' is not available, skip this test", mongodumpBin));
		assumeFileExecutable(mongorestoreBin, 
				String.format("mongorestore binary '%s' is not available, skip this test", mongorestoreBin));
	}
	
	private BackupConfiguration getBackupConfiguration() {
		return BackupConfiguration.getInstance(TEST_DATABASE_NAME, TEST_BACKUP_NAME);
	}

	/**
	 * backup TEST_DATABASE_NAME database into zip file located in tmp directory
	 * @throws BackupException 
	 */
	@Test
	public void should_backup_database() throws BackupException {
		// GIVEN 
		// IMPROVEMENT : create dedicated mongo database + content");
		assumeHostIsReadyForTest();
		MongodumpService mService = MongodumpService.getInstance(hostConf);
		BackupConfiguration backupConf = getBackupConfiguration();
		
		// WHEN 
		String backupFile = mService.backup(backupConf);

		// THEN
		log.info("backup file created : {}", backupFile);
		assertFile(backupFile);
	}

	/**
	 * restore TEST_DATABASE_NAME database from zip file located in tmp directory
	 * WARNING: restore action will first drop TEST_DATABASE_NAME database before importing data !
	 */
	@Test
	public void should_restore_database() throws RestoreException {
		// GIVEN 
		assumeHostIsReadyForTest();
		BackupConfiguration backupConf = getBackupConfiguration();
		MongodumpService mService = MongodumpService.getInstance(hostConf);
		RestoreConfiguration restoreConf = RestoreConfiguration.getInstance(backupConf);
		log.info("restore from : {}", restoreConf.getBackupFile());		
		
		// WHEN
		mService.restore(restoreConf);

		// THEN
		// IMPROVEMENT : check mongo content
	}

	
	/**
	 * backup TEST_DATABASE_NAME database into zip file located in a dropbox directory
	 * require DROPBOX_TOKEN environment variable set
	 *
	@Test
	public void should_backup_database_on_dropbox() throws RestoreException {
		// GIVEN 
		// IMPROVEMENT : create dedicated mongo database + content");
		assumeHostIsReadyForTest();
		MongodumpService mService = MongodumpService.getInstance(hostConf);
		BackupConfiguration backupConf = getBackupConfiguration();
		
		// WHEN 
		String backupFile = mService.backup(backupConf);

		// THEN
		log.info("backup file created : {}", backupFile);
		assertFile(backupFile);
	}
	*/


}
