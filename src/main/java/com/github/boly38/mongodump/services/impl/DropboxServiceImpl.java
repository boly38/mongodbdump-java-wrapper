package com.github.boly38.mongodump.services.impl;

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

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeleteErrorException;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.users.FullAccount;
import com.github.boly38.mongodump.services.contract.DropboxService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DropboxServiceImpl implements DropboxService {
	public static final String DEFAULT_APP_NAME = "dropbox/Applications/MongoWrapper";
	
	private DbxClientV2 dboxClient = null;

	public DropboxServiceImpl() {
        _initClient();
	}

	private void warnUsage() {
    	String notTokenMsg = String.format("no dropbox token defined, env:%s is not set", DROPBOX_TOKEN_KEY);
		log.warn(notTokenMsg);
	}
	
	private String getDropBoxToken() {
		String token = System.getenv(DROPBOX_TOKEN_KEY);
	    boolean tokenIsSet = token != null && !token.isEmpty();
	    if (!tokenIsSet) {
	    	return null;
	    }
		return token;
	}

	private String getDropBoxAppName() {
		String appName = System.getenv(DROPBOX_APPLICATION_KEY);
	    boolean appIsSet = appName != null && !appName.isEmpty();
	    if (!appIsSet) {
	    	return DEFAULT_APP_NAME;
	    }
		return appName;
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
		String appName = getDropBoxAppName();
	    String token = getDropBoxToken();
	    if (token == null || token.isEmpty()) {
	    	return;
	    }
        HttpRequestor requ = getProxyRequestor();

        DbxRequestConfig config;

        if(requ!=null) {
            config = new DbxRequestConfig(appName, localeString,requ);
        } else {
            config = new DbxRequestConfig(appName, localeString);
        }

        dboxClient = new DbxClientV2(config, token);
        log.debug("connected to dropbox application '{}'", appName);
	}

	public void assumeAvailable() {
		if (!isAvailable()) {
			warnUsage();
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

	/**
	 * list dropbox folder
	 * @param folderName
	 * @return target folder list of meta data
	 * @throws ListFolderErrorException
	 * @throws DbxException
	 */
	public List<Metadata> listFolder(String folderName) throws ListFolderErrorException, DbxException {
		assumeAvailable();
		List<Metadata> entries = dboxClient.files().listFolder(folderName).getEntries();
		return entries;
	}

	/**
	 * upload local file to dropbox folder
	 * @param localFilename
	 * @param dboxFilename
	 * @return uploaded file meta data
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws UploadErrorException
	 * @throws DbxException
	 */
	public FileMetadata uploadFile(String localFilename, String dboxFilename) throws FileNotFoundException, IOException, UploadErrorException, DbxException {
		assumeAvailable();
        try (InputStream in = new FileInputStream(localFilename)) {
            FileMetadata metadata = dboxClient.files().uploadBuilder(dboxFilename)
                .uploadAndFinish(in);
            return metadata;
        }
	}

	/**
	 * download dropbox file locally
	 * @param dboxFilename
	 * @return downloaded local temp file
	 * @throws IOException
	 * @throws DownloadErrorException
	 * @throws DbxException
	 */
	public String downloadFile(String dboxFilename) throws IOException, DownloadErrorException, DbxException {
		File tmpFile = File.createTempFile("dropbox-downloaded-file", ".tmp"); 
		DbxDownloader<FileMetadata> download = dboxClient.files().download(dboxFilename);
		OutputStream fOut = new FileOutputStream(tmpFile);
		download.download(fOut );
		String filePath = tmpFile.getAbsolutePath();
		fOut.close();
		return filePath;
	}

	public void removeFile(String dropTarget) throws DeleteErrorException, DbxException {
		dboxClient.files().delete(dropTarget);
	}
}
