package com.github.boly38.mongodump.domain.hostconf;

public class OpenshiftHost {
	public static boolean isOpenshiftHost() {
		return System.getenv("OPENSHIFT_MONGODB_DB_URL") != null;
	}

	public static String getMongoHostPort() {
		return String.format("%s:%s", 
				System.getenv("OPENSHIFT_MONGODB_DB_HOST"), 
				System.getenv("OPENSHIFT_MONGODB_DB_PORT"));
	}

	public static String getMongoUsername() {
		return System.getenv("OPENSHIFT_MONGODB_DB_USERNAME");
	}

	public static String getMongoPassword() {
		return System.getenv("OPENSHIFT_MONGODB_DB_PASSWORD");
	}

	public static String getTmpDir() {
		return System.getenv("OPENSHIFT_TMP_DIR");
	}
}
