package org.internetresources.util.mongodump;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Locale;

import org.junit.Assume;
import org.junit.Test;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DropBoxTest {
	public static final String APP_NAME = "dropbox/Applications/MongoWrapper";
	public static final String DROPBOX_TOKEN_KEY = "DROPBOX_TOKEN";

	private String getDropBoxToken() {
		String token = System.getenv(DROPBOX_TOKEN_KEY);
	    boolean tokenIsSet = token != null && !token.isEmpty();
    	String notTokenMsg = String.format("no dropbox token defined, env:%s is not set", DROPBOX_TOKEN_KEY);
	    if (!tokenIsSet) {
			log.warn(notTokenMsg);
	    }
		Assume.assumeTrue(notTokenMsg, tokenIsSet);
		return token;
	}

	public HttpRequestor getProxy(){
		String proxyHost = System.getProperty("https.proxyHost", "");
		if (proxyHost == null || proxyHost.isEmpty()) {
			return null;
		}
		String proxyPortString = System.getProperty("https.proxyPort", "80");
		
		int proxyPort = 80;
		try { 
			proxyPort = Integer.parseInt(proxyPortString);
		} catch (NumberFormatException nfe) {
			throw new IllegalStateException(String.format("invalid proxy port '%s'", proxyPortString));
		}

		/*
	        final String authUser = "username";
	        final String authPassword = "password";

	        Authenticator.setDefault(new Authenticator() {
	            @Override
	            protected PasswordAuthentication getPasswordAuthentication() {
	                    return new PasswordAuthentication(authUser, authPassword.toCharArray());
	            }
	        });
	        */
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost,proxyPort));
        Config conf = Config.builder().withProxy(proxy).build();
		HttpRequestor req = new StandardHttpRequestor(conf);
        log.info("use proxy {}:{}", proxyHost, proxyPort);
        return req;
	}

	@Test
	public void should_list_box() throws ListFolderErrorException, DbxException {
        String localeString = Locale.getDefault().toString();
	    String token = getDropBoxToken();
        HttpRequestor requ = getProxy();

        DbxRequestConfig config;

        if(requ!=null) {
            config = new DbxRequestConfig(APP_NAME, localeString,requ);
        } else {
            config = new DbxRequestConfig(APP_NAME, localeString);
        }

        DbxClientV2 client = new DbxClientV2(config, token);


        // Get current account info
        FullAccount account = client.users().getCurrentAccount();
        System.out.println(account.getName().getDisplayName());

        // Get files and folder metadata from Dropbox root directory
        List<Metadata> entries = client.files().listFolder("").getEntries();
        for (Metadata metadata : entries) {
            System.out.println(metadata.getPathLower());
        }

        /* Upload "test.txt" to Dropbox
        try (InputStream in = new FileInputStream("test.txt")) {
            FileMetadata metadata = client.files().uploadBuilder("/test.txt")
                .uploadAndFinish(in);
        }
        */
	}

}
