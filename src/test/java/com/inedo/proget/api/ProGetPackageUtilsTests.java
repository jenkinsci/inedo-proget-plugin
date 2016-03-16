package com.inedo.proget.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.PackageMetadata;
import com.inedo.proget.domain.ProGetPackage;

public class ProGetPackageUtilsTests {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
	public void prepareTestFiles() throws IOException {
		preparePackageFiles();
	}
	
	@Test
	public void antStyleIncludes() throws IOException {
		checkFilter("Everything included", 5, "**/*.*", "", false);
		checkFilter("Log file excluded", 4, "**/*.*", "**/*.log", false);
		checkFilter("Log file excluded", 4, "**/*.*", "logs/sample.log", false);
		checkFilter("Only backup file included", 1, "**/*.bak", "", false);
		checkFilter("Only data file included", 1, "*.data", "", false);
		checkFilter("Multiple filters", 3, "**/*.data, *.txt", "", false);
		checkFilter("Multiple exlcudes", 2, "**/*.*", "**/*.txt, **/*.bak, **/*.log", false);
		checkFilter("Exlcude Folder", 2, "**/*.*", "more/, logs/", false);
	}
	
	private void checkFilter(String assertMessage, int expectedFileCount, String include, String exclude, boolean caseSensitive) throws IOException {
		List<File> files = ProGetPackageUtils.getFileList(folder.getRoot(), include, exclude, caseSensitive);
		
		assertThat("Package is created", files, is(not(empty())));
		assertThat(assertMessage, files.size(), is(equalTo(expectedFileCount)));
	}
	
	@Test
	public void createPackageFromSourceFolder() throws IOException {
		File pkg = new ProGetPackageUtils().createPackage(folder.getRoot(), getMetadata());
		
		verifyPackage(pkg, 6);
	}

	@Test
	public void createPackageFromAntIncludes() throws IOException {
		List<File> files = ProGetPackageUtils.getFileList(folder.getRoot(), "**/*.*", "logs/", false);
		File pkg = new ProGetPackageUtils().createPackage(folder.getRoot(), files, getMetadata());
		
		verifyPackage(pkg, 3);
	}
	
	@Test
	public void unpackContent() throws ZipException, IOException {
		File pkg = new ProGetPackageUtils().createPackage(folder.getRoot(), getMetadata());
		File temp = new File(folder.getRoot(), "temp");
		temp.mkdir();
		
		temp = new File(temp, pkg.getName());
		Files.move(pkg, temp);
		pkg = temp;
		
		int fileCount = pkg.getParentFile().listFiles().length;
		
		ProGetPackageUtils.unpackContent(pkg);
		
		assertThat("File have been unpacked", pkg.getParentFile().listFiles().length, is(greaterThan(fileCount)));
	}
	
	private void verifyPackage(File pkg, int expectedFileCount) throws IOException, UnsupportedEncodingException, ZipException {
		try (ZipFile zip = new ZipFile(pkg)) {
	        assertThat("Package file contains 2 entries", zip.size(), is(equalTo(expectedFileCount)));
	        
	        ZipEntry ze = zip.getEntry("upack.json");
	        assertThat("Package file contains upack.json", ze, is(notNullValue()));
	        
	        try (InputStream zin = zip.getInputStream(ze)) {
		        byte[] bytes= new byte[(int)ze.getSize()];
		        zin.read(bytes, 0, bytes.length);
	            String json = new String( bytes, "UTF-8" );
	            
	            // Will throw JsonParseException if not valid json
	            new JsonParser().parse(json);
	        }  catch (JsonParseException e) {
	        	fail("unpack.json is not a valid json file");
	        }
	        
	        ze = zip.getEntry("unpack/sample.data");
	        assertThat("Package file contains unpack folder", ze, is(notNullValue()));
		}
	}
	
	private void preparePackageFiles() throws IOException {
		createFile(new File(folder.getRoot(), "sample.data"), "This is a sample data file");
		createFile(new File(folder.getRoot(), "sample.txt"), "This is a sample text file");
		createFile(new File(folder.getRoot(), "more/sample.data"), "This is a another sample data file");
		createFile(new File(folder.getRoot(), "logs/sample.log"), "This is a sample log file");
		createFile(new File(folder.getRoot(), "logs/sample.log.bak"), "This is a sample log file");
	}
	
	private void createFile(File file, String content) throws IOException {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdir();
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write("This is a sample file");
		}
	}
	
	private PackageMetadata getMetadata() {
		PackageMetadata metadata = new PackageMetadata();
		metadata.group = "com/inedo/proget";
		metadata.name = "ExamplePackage";
		metadata.version = "0.0.3";
		metadata.title = "Example Package";
		metadata.description = "Example package for testing";
		return metadata;
	}
	
	
}
