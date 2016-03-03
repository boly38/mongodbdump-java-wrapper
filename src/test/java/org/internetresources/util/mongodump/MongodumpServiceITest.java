package org.internetresources.util.mongodump;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;

import org.internetresources.util.mongodump.domain.BackupConfiguration;
import org.internetresources.util.mongodump.domain.MongoServerHostConfiguration;
import org.internetresources.util.mongodump.domain.RestoreConfiguration;
import org.internetresources.util.mongodump.domain.RestoreException;
import org.junit.Assume;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MongodumpServiceITest {

	final String TEST_DATABASE_NAME = "myDB";
	private MongoServerHostConfiguration hostConf = new MongoServerHostConfiguration();

    public boolean fileExists(String fileFullName) {
        File targetFile = new File(fileFullName);
        return targetFile.exists();
    }

    public void assertFile(String fileFullName) {
        boolean expectedFileExists = fileExists(fileFullName);
        assertThat(expectedFileExists).as(" file " + fileFullName + " should exists").isTrue();
    }
	
	private void assumeFileExecutable(String filePath, String assumeError) {
		File f = new File(filePath);
		boolean fIsExecutable = f.exists() && f.canExecute();
		Assume.assumeTrue(assumeError, fIsExecutable);
		
	}

	private void assumeHostIsReadyForTest() {
		String mongodumpBin = hostConf.getMongoDumpBinAbsolutePath();
		String mongorestoreBin = hostConf.getMongoRestoreBinAbsolutePath();
		assumeFileExecutable(mongodumpBin, 
				String.format("mongodump binary '%s' is not available, skip this test", mongodumpBin));
		assumeFileExecutable(mongorestoreBin, 
				String.format("mongorestore binary '%s' is not available, skip this test", mongorestoreBin));
	}

	@Test
	public void should_backup_database() throws RestoreException {
		// GIVEN 
		log.debug("TODO : create dedicated mongo database + content");
		assumeHostIsReadyForTest();
		MongodumpService mService = MongodumpService.getInstance(hostConf);
		BackupConfiguration backupConf = BackupConfiguration.getInstance(TEST_DATABASE_NAME);
		
		// WHEN 
		String backupFile = mService.backup(backupConf);

		// THEN
		log.info("assert zip file is created : {}", backupFile);
		assertFile(backupFile);
	}


	@Test
	public void should_restore_database() throws RestoreException {
		// GIVEN 
		assumeHostIsReadyForTest();
		String backupFilename = String.format("%s%s%s%s",
				BackupConfiguration.DEFAULT_BACKUP_DIRECTORY,
				File.separator,
				TEST_DATABASE_NAME,
				".zip"); 
		MongodumpService mService = MongodumpService.getInstance(hostConf);
		RestoreConfiguration restoreConf = RestoreConfiguration.getInstance(TEST_DATABASE_NAME, backupFilename);
		log.info("restore from : {}", backupFilename);		
		
		// WHEN
		mService.restore(restoreConf);

		// THEN
		log.debug("TODO : check mongo content");
	}

}
