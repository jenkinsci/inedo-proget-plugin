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
import com.inedo.proget.api.ProGetPackager.ZipItem;
import com.inedo.proget.jenkins.JenkinsHelper;
import com.inedo.proget.jenkins.UploadPackageBuilder;

public class ProGetPackagerTests {
	private JenkinsHelper helper;
	private ProGetPackager packageUtils = new ProGetPackager();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
	public void prepareTestFiles() throws IOException {
		helper = new JenkinsHelper();
		
		preparePackageFiles();
	}
	
	@Test
	public void antStyleIncludes() throws IOException {
		checkFilter("Everything included", 5, "**/*.*", "");
		checkFilter("Log file excluded", 4, "**/*.*", "**/*.log");
		checkFilter("Log file excluded", 4, "**/*.*", "bin/logs/sample.log");
		checkFilter("Only backup file included", 1, "**/*.bak", "");
		checkFilter("Only data file included", 1, "bin/*.data", "");
		checkFilter("Multiple filters", 3, "**/*.data, bin/*.txt", "");
		checkFilter("Multiple exlcudes", 2, "**/*.*", "**/*.txt, **/*.bak, **/*.log");
		checkFilter("Exlcude Folder", 2, "**/*.*", "bin/more/, bin/logs/");
	}

	@Test
	public void consolidateFolders() {
		UploadPackageBuilder builder = getExampleBuilder("[bin]/logs/*.log", "");
		List<ZipItem> files = packageUtils.getFileList(folder.getRoot(), builder);
		
		assertThat("Expected file was found", files.size(), is(equalTo(1)));
		assertThat("Expected file was found", files.get(0).getSourceFile(), is(equalTo("bin/logs/sample.log".replace("/", File.separator))));
		assertThat("Desintation file has removed bin folder", files.get(0).getDestinationFile(), is(equalTo("logs/sample.log".replace("/", File.separator))));
	}
	
	@Test
	public void createPackageFromAntIncludes() throws IOException {
		UploadPackageBuilder builder = getExampleBuilder("[bin]/**/*.*", "bin/logs/");
		List<ZipItem> files = packageUtils.getFileList(folder.getRoot(), builder);
		
		File pkg = packageUtils.createPackage(folder.getRoot(), files, builder.buildMetadata(helper));
		
		verifyPackage(pkg, 4);
	}
	
	@Test
	public void unpackContent() throws ZipException, IOException {
		UploadPackageBuilder builder = getExampleBuilder("bin/**/*.*", "logs/");
		List<ZipItem> files = packageUtils.getFileList(folder.getRoot(), builder);
		
		File pkg = packageUtils.createPackage(folder.getRoot(), files, builder.buildMetadata(helper));
		
		File temp = new File(folder.getRoot(), "temp");
		temp.mkdir();
		
		temp = new File(temp, pkg.getName());
		Files.move(pkg, temp);
		pkg = temp;
		
		int fileCount = pkg.getParentFile().listFiles().length;
		
		ProGetPackager.unpackContent(pkg);
		
		assertThat("File have been unpacked", pkg.getParentFile().listFiles().length, is(greaterThan(fileCount)));
	}
	
	private void verifyPackage(File pkg, int expectedFileCount) throws IOException, UnsupportedEncodingException, ZipException {
		try (ZipFile zip = new ZipFile(pkg)) {
	        assertThat("Package file contains " + expectedFileCount + " entries", zip.size(), is(equalTo(expectedFileCount)));
	        
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
	        
	        ze = zip.getEntry("unpack" + File.separator + "sample.data");
	        assertThat("Package file contains unpack folder", ze, is(notNullValue()));
		}
	}
	
	private UploadPackageBuilder getExampleBuilder(String include, String exclude) {
		UploadPackageBuilder settings = new UploadPackageBuilder("Example", "andrew/sumner/proget", "ExamplePackage", "0.0.3", include);
		settings.setCaseSensitive(false);
		settings.setDefaultExcludes(false);
		settings.setExcludes(exclude);
		
		settings.setTitle("Example Title");
		settings.setDescription("This contains [example](http://example.net) files specific to test suite");
		settings.setIcon("package://1UP.ico");
		settings.setMetadata("custom=yes\rreally=of course");
		settings.setDependencies("my.dependency:one\rmy.dependency:two");
		
		return settings;
	}
	
	private void checkFilter(String assertMessage, int expectedFileCount, String include, String exclude) throws IOException {
		UploadPackageBuilder builder = getExampleBuilder(include, exclude);
		
		List<ZipItem> files = packageUtils.getFileList(folder.getRoot(), builder);
		
		assertThat("Package is created", files, is(not(empty())));
		assertThat(assertMessage, files.size(), is(equalTo(expectedFileCount)));
	}
	
	private void preparePackageFiles() throws IOException {
		createFile(new File(folder.getRoot(), "bin/sample.data"), "This is a sample data file");
		createFile(new File(folder.getRoot(), "bin/sample.txt"), "This is a sample text file");
		createFile(new File(folder.getRoot(), "bin/more/sample.data"), "This is a another sample data file");
		createFile(new File(folder.getRoot(), "bin/logs/sample.log"), "This is a sample log file");
		createFile(new File(folder.getRoot(), "bin/logs/sample.log.bak"), "This is a sample log file");
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
