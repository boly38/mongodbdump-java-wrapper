package com.github.boly38.mongodump.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.fest.assertions.api.Assertions;
import org.junit.Assume;
import org.junit.Test;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.github.boly38.mongodump.services.DropboxService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DropboxServiceITest {

	private DropboxService assumeDroptboxRequirement() {
		DropboxService dboxSvc = new DropboxService();
		Assume.assumeTrue(dboxSvc.isAvailable());
		return dboxSvc;
	}

	@Test
	public void should_list_directory_files() throws ListFolderErrorException, DbxException {
		should_list_directory("", 2); // app root directory
		should_list_directory("/essai", 1);
	}

	private void should_list_directory(String dirName, int dirFileCount) throws ListFolderErrorException, DbxException {
		// GIVEN
		DropboxService dboxSvc = assumeDroptboxRequirement();
		// WHEN
		List<Metadata> listFolder = dboxSvc.listFolder(dirName);
		// THEN
		Assertions.assertThat(listFolder).isNotNull();
		for (Metadata m : listFolder) {
			log.info(m.getPathLower());
		}
		Assertions.assertThat(listFolder.size()).isEqualTo(dirFileCount);
	}


	private String getTestImagePath() throws URISyntaxException {
		URL resource = DropboxServiceITest.class.getResource("../../../../image.jpg");
		String imagePath = (Paths.get(resource.toURI()).toFile()).getAbsolutePath();
		return imagePath;
	}

	@Test
	public void should_upload_then_download_file() throws ListFolderErrorException, FileNotFoundException, DbxException, IOException, URISyntaxException {
		should_upload_file();		
		should_download_file();		
	}
	
	public void should_upload_file() throws ListFolderErrorException, DbxException, FileNotFoundException, IOException, URISyntaxException {
		// GIVEN
		DropboxService dboxSvc = assumeDroptboxRequirement();
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
		DropboxService dboxSvc = assumeDroptboxRequirement();
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
