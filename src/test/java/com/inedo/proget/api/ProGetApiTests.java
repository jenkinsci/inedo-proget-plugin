package com.inedo.proget.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.inedo.proget.api.ProGetApi;
import com.inedo.proget.api.ProGetPackager.ZipItem;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.ProGetPackage;
import com.inedo.proget.domain.Version;
import com.inedo.proget.jenkins.GlobalConfig;
import com.inedo.proget.jenkins.JenkinsHelper;
import com.inedo.proget.jenkins.UploadPackageBuilder;
import com.inedo.proget.jenkins.DownloadPackageBuilder.DownloadFormat;
import com.inedo.utils.MockServer;
import com.inedo.utils.TestConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the ProGet API class
 * 
 * TODO: There is timing issue when running all tests against a live server as tests randomly fail.  Running one at a time works fine.   
 * 
 * @author Andrew Sumner
 */
public class ProGetApiTests {
	private static MockServer mockServer = null;
	private ProGetApi proget;
		
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	@BeforeClass
    public static void beforeClass() throws IOException {
		ProGetConfig config;

		if (TestConfig.useMockServer()) {
			mockServer = new MockServer();
			config = mockServer.getProGetConfig();
		} else {
			config = TestConfig.getProGetConfig();
		}

		GlobalConfig.injectConfiguration(config);
	}

	@Before
	public void before() {
		proget = new ProGetApi();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		if (mockServer != null) {
			mockServer.stop();
		}

		GlobalConfig.injectConfiguration(null);
	}

	@Test
	public void checkConnection() throws IOException {
		// An exception will be thrown if fails
		proget.canConnect();
	}
	
	@Test(expected=UnknownHostException.class)
	public void getWithIncorrectHost() throws IOException {
		ProGetConfig config = GlobalConfig.getProGetConfig();
		String origUrl = config.url; 
		config.url = "http://rubbish_host";

		try {
			new ProGetApi(config).getFeeds();
		} finally {
			config.url = origUrl;
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
		
		ProGetPackage[] packages = proget.getPackages(feed.Feed_Id);
    	
        assertThat("Expect more than one package", packages.length, is(greaterThan(0)));
	}
	
	@Test
	public void getPackageVersions() throws IOException  {
		Feed feed = proget.getFeed("Example");
		ProGetPackage pkg = proget.getPackages(feed.Feed_Id)[0];
    	Version[] versions = proget.getPackageVersions(feed.Feed_Id, pkg.Group_Name, pkg.Package_Name);
    	
        assertThat("Expect at least one version", versions.length, is(greaterThan(0)));
	}
	
	@Test
	public void downloadPackage() throws IOException  {
		Feed feed = proget.getFeed("Example");
		
		ProGetPackage pkg = proget.getPackages(feed.Feed_Id)[0];
		
		File downloaded = proget.downloadPackage(feed.Feed_Name, pkg.Group_Name, pkg.Package_Name, pkg.LatestVersion_Text, folder.getRoot().getAbsolutePath(), DownloadFormat.PACKAGE);
//		File downloaded = proget.downloadPackage("Example", "andrew/sumner/example", "examplepackage", "0.0.1", folder.getRoot().getAbsolutePath());
    	
        assertThat("File has content", downloaded.length(), is(greaterThan((long)400)));
	}
	
	@Test
	public void downloadPackageLatestVersion() throws IOException  {
		Feed feed = proget.getFeed("Example");
		
		ProGetPackage pkg = proget.getPackages(feed.Feed_Id)[0];
		
		File downloaded = proget.downloadPackage(feed.Feed_Name, pkg.Group_Name, pkg.Package_Name, "", folder.getRoot().getAbsolutePath(), DownloadFormat.PACKAGE);
    	
        assertThat("File has content", downloaded.length(), is(greaterThan((long)1000)));
	}
	
	@Test
	public void uploadPackage() throws IOException {
		preparePackageFiles();
		
		JenkinsHelper helper = new JenkinsHelper();
		ProGetPackager packageUtils = new ProGetPackager();
		UploadPackageBuilder builder = getExampleBuilder("**/*.*", "");
		List<ZipItem> files = packageUtils.getFileList(folder.getRoot(), builder);
		
		File pkg = packageUtils.createPackage(folder.getRoot(), files, builder.buildMetadata(helper));
		
		proget.uploadPackage("Example", pkg);
		
		// Success is fact that no exception thrown...
	}

	private void preparePackageFiles() throws IOException {
		createFile(new File(folder.getRoot(), "sample.data"), "This is a sample data file");
		createFile(new File(folder.getRoot(), "sample.txt"), "This is a sample text file");
		createFile(new File(folder.getRoot(), "more/sample.data"), "This is a another sample data file");
		createFile(new File(folder.getRoot(), "logs/sample.log"), "This is a sample log file");
		createFile(new File(folder.getRoot(), "logs/sample.log.bak"), "This is a sample log file");
		
		InputStream in = this.getClass().getResourceAsStream("1UP.ico");
		OutputStream out = new FileOutputStream(new File(folder.getRoot(), "1UP.ico"));
		IOUtils.copy(in, out);
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
	
	private void createFile(File file, String content) throws IOException {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdir();
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write("This is a sample file");
		}
	}
}
