package com.github.boly38.mongodump.services.contract;

import com.github.boly38.mongodump.domain.BackupConfiguration;
import com.github.boly38.mongodump.domain.BackupException;
import com.github.boly38.mongodump.domain.RestoreConfiguration;
import com.github.boly38.mongodump.domain.RestoreException;

public interface MongodumpService {
	String backup(BackupConfiguration backupConf) throws BackupException;
	void restore(RestoreConfiguration restoreConf) throws RestoreException;
}
