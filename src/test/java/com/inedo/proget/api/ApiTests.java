package com.inedo.proget.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.inedo.proget.MockServer;
import com.inedo.proget.api.ProGet;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.PackageMetadata;
import com.inedo.proget.domain.ProGetPackage;
import com.inedo.proget.jenkins.ProGetHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the ProGet API class
 * 
 * TODO: There is timing issue when running all tests against a live server as tests randomly fail.  Running one at a time works fine.   
 * 
 * @author Andrew Sumner
 */
public class ApiTests {
	private final boolean MOCK_REQUESTS = false;	// Set this value to false to run against a live BuildMaster installation 
	private MockServer mockServer;
	private ProGet proget;
		
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
    public void before() throws IOException {
		mockServer = new MockServer(MOCK_REQUESTS);
		
		proget = new ProGet(new ProGetHelper(mockServer.getProGetConfig()));
	}
	
	@After
	public void tearDown() throws Exception {
		mockServer.stop();
	}

	@Test
	public void checkConnection() throws IOException {
		// An exception will be thrown if fails
		proget.checkConnection();
	}
	
	@Test(expected=UnknownHostException.class)
	public void getWithIncorrectHost() throws IOException {
		ProGetConfig config = mockServer.getProGetConfig();
		String origUrl = config.url; 
		config.url = "http://buildmaster1";
		
		proget = new ProGet(new ProGetHelper(config));
		
		try {
			proget.getFeeds();
		} finally {
			mockServer.getProGetConfig().url = origUrl;
		}
	}
	
	@Test
	public void getFeeds() throws IOException  {
    	Feed[] feeds = proget.getFeeds();
    	
        assertThat("Expect to have a feed", feeds.length, is(greaterThan(0)));
	}
	
	@Test
	public void getPackageList() throws IOException  {
		Feed feed = proget.getFeed("Example");
		
		ProGetPackage[] packages = proget.getPackageList(feed.Feed_Id);
    	
        assertThat("Expect more than one package", packages.length, is(greaterThan(0)));
	}
	
	@Test
	public void downloadPackage() throws IOException  {
		Feed feed = proget.getFeed("Example");
		
		ProGetPackage pkg = proget.getPackageList(feed.Feed_Id)[0];
		
		File downloaded = proget.downloadPackage(feed.Feed_Name, pkg.Group_Name, pkg.Package_Name, pkg.LatestVersion_Text, folder.getRoot().getAbsolutePath());
//		File downloaded = proget.downloadPackage("Example", "andrew/sumner/example", "examplepackage", "0.0.1", folder.getRoot().getAbsolutePath());
    	
        assertThat("File has content", downloaded.length(), is(greaterThan((long)1000)));
	}
	
	@Test
	public void downloadPackageLatestVersion() throws IOException  {
		Feed feed = proget.getFeed("Example");
		
		ProGetPackage pkg = proget.getPackageList(feed.Feed_Id)[0];
		
		File downloaded = proget.downloadPackage(feed.Feed_Name, pkg.Group_Name, pkg.Package_Name, "", folder.getRoot().getAbsolutePath());
    	
        assertThat("File has content", downloaded.length(), is(greaterThan((long)1000)));
	}
	
	@Test
	public void uploadPackage() throws IOException {
		PackageMetadata metadata = preparePackageFiles();
		
		File pkg = new ProGetPackageUtils().createPackage(folder.getRoot(), metadata);
		
		proget.uploadPackage("Example", pkg);
		
		// Success is fact that no exception thrown...
	}

	private PackageMetadata preparePackageFiles() throws IOException {
		createFile(new File(folder.getRoot(), "sample.data"), "This is a sample data file");
		createFile(new File(folder.getRoot(), "sample.txt"), "This is a sample text file");
		createFile(new File(folder.getRoot(), "more/sample.data"), "This is a another sample data file");
		createFile(new File(folder.getRoot(), "logs/sample.log"), "This is a sample log file");
		createFile(new File(folder.getRoot(), "logs/sample.log.bak"), "This is a sample log file");
				
		PackageMetadata metadata = new PackageMetadata();
		metadata.group = "com/inedo/proget";
		metadata.name = "ExamplePackage";
		metadata.version = "0.0.3";
		metadata.title = "Example Package";
		metadata.description = "Example package for testing";
		return metadata;
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
