package org.internetresources.util.mongodump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Locale;

import org.junit.Assume;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.users.FullAccount;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DropboxService {
	public static final String APP_NAME = "dropbox/Applications/MongoWrapper";
	public static final String DROPBOX_TOKEN_KEY = "DROPBOX_TOKEN";
	
	private DbxClientV2 dboxClient = null;

	public DropboxService() {
        _initClient();
	}

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

	private HttpRequestor getProxyRequestor(){
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
	private void _initClient() {
		String localeString = Locale.getDefault().toString();
	    String token = getDropBoxToken();
	    if (token == null || token.isEmpty()) {
	    	return;
	    }
        HttpRequestor requ = getProxyRequestor();

        DbxRequestConfig config;

        if(requ!=null) {
            config = new DbxRequestConfig(APP_NAME, localeString,requ);
        } else {
            config = new DbxRequestConfig(APP_NAME, localeString);
        }

        dboxClient = new DbxClientV2(config, token);
	}

	private void assumeAvailable() {
		if (!isAvailable()) {
			throw new IllegalStateException("DropBox client is not available");
		}
	}

	public boolean isAvailable() {
		return (dboxClient != null);
	}
	
	public String getAccount() throws DbxException {
		assumeAvailable();
	    // Get current account info
        FullAccount account = dboxClient.users().getCurrentAccount();
        return account.getName().getDisplayName();
	}

	public List<Metadata> listFolder(String folderName) throws ListFolderErrorException, DbxException {
		assumeAvailable();
		return dboxClient.files().listFolder(folderName).getEntries();
	}

	public FileMetadata uploadFile(String localFilename, String dboxFilename) throws FileNotFoundException, IOException, UploadErrorException, DbxException {
		assumeAvailable();
        try (InputStream in = new FileInputStream(localFilename)) {
            FileMetadata metadata = dboxClient.files().uploadBuilder(dboxFilename)
                .uploadAndFinish(in);
            return metadata;
        }
	}

	public String downloadFile(String dboxFilename) throws IOException, DownloadErrorException, DbxException {
		File tmpFile = File.createTempFile("dropbox-downloaded-file", ".tmp"); 
		DbxDownloader<FileMetadata> download = dboxClient.files().download(dboxFilename);
		OutputStream fOut = new FileOutputStream(tmpFile);
		download.download(fOut );
		String filePath = tmpFile.getAbsolutePath();
		fOut.close();
		return filePath;
	}
}
