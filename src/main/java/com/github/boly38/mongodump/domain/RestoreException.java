package com.github.boly38.mongodump.domain;

public class RestoreException extends Exception {
	/**
	 * serialVersionUID 
	 */
	private static final long serialVersionUID = 4660979883427092009L;

	public RestoreException(String errMsg) {
		super(errMsg);
	}

	public RestoreException(String errMsg, Throwable e) {
		super(errMsg, e);
	}

	private static String exceptionMessage(int existStatus, String errorMessage) {
		String errorDetails = "";
		if (errorMessage != null) {
			errorDetails = String.format("message : %s", errorMessage);
		}
		return String.format("Restore exception exitStatus:%d %s", existStatus, errorDetails);
	}

	public RestoreException(int existStatus, String errorMessage) {
		super(exceptionMessage(existStatus, errorMessage));
	}

}
