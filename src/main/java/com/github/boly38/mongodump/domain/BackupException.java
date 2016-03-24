package com.github.boly38.mongodump.domain;

public class BackupException extends Exception {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 4113027926941514546L;

	public BackupException(String errMsg) {
		super(errMsg);
	}

	public BackupException(String exMsg, Throwable e) {
		super(exMsg, e);
	}

	private static String exceptionMessage(int existStatus, String errorMessage) {
		String errorDetails = "";
		if (errorMessage != null) {
			errorDetails = String.format("message : %s", errorMessage);
		}
		return String.format("Backup exception exitStatus:%d %s", existStatus, errorDetails);
	}

	public BackupException(int existStatus, String errorMessage) {
		super(exceptionMessage(existStatus, errorMessage));
	}



}
