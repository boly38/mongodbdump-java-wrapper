package com.github.boly38.mongodump.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.fest.assertions.api.Assertions;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.github.boly38.mongodump.services.impl.DropboxServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Ignore("only manual tests : dropbox env required")
public class DropboxServiceITest {

	private DropboxServiceImpl assumeDroptboxRequirement() {
		DropboxServiceImpl dboxSvc = new DropboxServiceImpl();
		Assume.assumeTrue(dboxSvc.isAvailable());
		return dboxSvc;
	}

	@Test
	public void should_list_directory_files() throws ListFolderErrorException, DbxException {
		should_list_directory("", 2); // app root directory
		should_list_directory("/essai", 1);
	}

	private void should_list_directory(String dirName, int dirFileMinCount) throws ListFolderErrorException, DbxException {
		// GIVEN
		DropboxServiceImpl dboxSvc = assumeDroptboxRequirement();
		// WHEN
		List<Metadata> listFolder = dboxSvc.listFolder(dirName);
		// THEN
		Assertions.assertThat(listFolder).isNotNull();
		for (Metadata m : listFolder) {
			log.info(m.getPathLower());
		}
		Assertions.assertThat(listFolder.size()).isGreaterThanOrEqualTo(dirFileMinCount);
	}


	private String getTestImagePath() throws URISyntaxException {
		URL resource = getClass().getClassLoader().getResource("image.jpg");
		if (resource ==  null) {
			return null;
		}
		String imagePath = new File(resource.toURI()).getAbsolutePath();
		return imagePath;
	}

	@Test
	public void should_upload_then_download_file() throws ListFolderErrorException, FileNotFoundException, DbxException, IOException, URISyntaxException {
		should_upload_file();		
		should_download_file();		
	}
	
	public void should_upload_file() throws ListFolderErrorException, DbxException, FileNotFoundException, IOException, URISyntaxException {
		// GIVEN
		DropboxServiceImpl dboxSvc = assumeDroptboxRequirement();
		String imagePath = getTestImagePath();
		
		// WHEN
		FileMetadata uploadFile = dboxSvc.uploadFile(imagePath, "/essaiUpload/image.jpg");
		// THEN
		Assertions.assertThat(uploadFile).isNotNull();
		String uploadedFilePath = uploadFile.getPathLower();
		log.info("{} uploaded", uploadedFilePath);
	}


	public void should_download_file() throws ListFolderErrorException, DbxException, FileNotFoundException, IOException, URISyntaxException {
		// GIVEN
		DropboxServiceImpl dboxSvc = assumeDroptboxRequirement();
		String imageOriginPath = getTestImagePath();
		File imageOrigin = new File(imageOriginPath);
	
		// WHEN
		String downloadTmpFile = dboxSvc.downloadFile("/essaiUpload/image.jpg");
		// THEN
		Assertions.assertThat(downloadTmpFile).isNotNull();
		File tmpFile = new File(downloadTmpFile);
		try {
			boolean contentEquals = FileUtils.contentEquals(tmpFile, imageOrigin);
			Assertions.assertThat(contentEquals).isTrue();
			log.info("{} and (uploaded) tmp file {} are identicals", imageOriginPath, downloadTmpFile);
		} finally {
			tmpFile.setWritable(true);
			tmpFile.delete();
		}
	}
}
