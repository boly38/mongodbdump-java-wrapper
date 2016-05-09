package com.github.boly38.mongodump.domain;

public class DeleteException extends Exception {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 3212376071282127434L;

	public DeleteException(String errMsg) {
		super(errMsg);
	}

	public DeleteException(String exMsg, Throwable e) {
		super(exMsg, e);
	}

}
