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
import com.inedo.proget.jenkins.ProGetHelper;
import com.inedo.proget.jenkins.UploadPackageBuilder;

public class ProGetPackageUtilsTests {
	private ProGetHelper helper;
	
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
	public void prepareTestFiles() throws IOException {
		helper = new ProGetHelper(null, new MockTaskListener());
		
		
		preparePackageFiles();
	}
	
	@Test
	public void antStyleIncludes() throws IOException {
		checkFilter("Everything included", 5, "**/*.*", "");
		checkFilter("Log file excluded", 4, "**/*.*", "**/*.log");
		checkFilter("Log file excluded", 4, "**/*.*", "logs/sample.log");
		checkFilter("Only backup file included", 1, "**/*.bak", "");
		checkFilter("Only data file included", 1, "*.data", "");
		checkFilter("Multiple filters", 3, "**/*.data, *.txt", "");
		checkFilter("Multiple exlcudes", 2, "**/*.*", "**/*.txt, **/*.bak, **/*.log");
		checkFilter("Exlcude Folder", 2, "**/*.*", "more/, logs/");
	}
	
	public UploadPackageBuilder getSettings(String include, String exclude) {
		UploadPackageBuilder settings = new UploadPackageBuilder("Example", "andrew/sumner/proget", "ExamplePackage", "0.0.3", include);
		settings.setCaseSensitive(false);
		settings.setDefaultExcludes(false);
		settings.setExcludes(exclude);
		
		settings.setTitle("");
		settings.setDescription("");
		settings.setIcon("");
		settings.setMetadata("custom=yes\rreally=of course");
		settings.setDependencies("");
		
		return settings;
	}
	
	private void checkFilter(String assertMessage, int expectedFileCount, String include, String exclude) throws IOException {
		ProGetPackageUtils utils = new ProGetPackageUtils();
		
		List<String> files = utils.getFileList(folder.getRoot(), getSettings(include, exclude));
		
		assertThat("Package is created", files, is(not(empty())));
		assertThat(assertMessage, files.size(), is(equalTo(expectedFileCount)));
	}
	
	@Test
	public void createPackageFromSourceFolder() throws IOException {
		File pkg = new ProGetPackageUtils().createPackage(folder.getRoot(), helper.getMetadata(getSettings("", "")));
		
		verifyPackage(pkg, 6);
	}

	@Test
	public void createPackageFromAntIncludes() throws IOException {
		UploadPackageBuilder settings = new UploadPackageBuilder("Example", "andrew/sumner/proget", "ExamplePackage", "0.0.3", "**/*.*");
		settings.setCaseSensitive(false);
		settings.setDefaultExcludes(false);
		settings.setExcludes("logs/");
		
		settings.setTitle("");
		settings.setDescription("");
		settings.setIcon("");
		settings.setMetadata("");
		settings.setDependencies("");

		ProGetPackageUtils utils = new ProGetPackageUtils();
		
		List<String> files = utils.getFileList(folder.getRoot(), settings);
		File pkg = new ProGetPackageUtils().createPackage(folder.getRoot(), files, helper.getMetadata(getSettings("", "")));
		
		verifyPackage(pkg, 4);
	}
	
	@Test
	public void unpackContent() throws ZipException, IOException {
		File pkg = new ProGetPackageUtils().createPackage(folder.getRoot(), helper.getMetadata(getSettings("", "")));
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
	            
	            assertThat("Metadata contains custom attribute", json, containsString("custom"));
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
}
