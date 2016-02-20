package com.inedo.proget.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.inedo.proget.MockServer;
import com.inedo.proget.api.ProGet;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.ProGetPackage;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;

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
public class ProGetTests {
	private final boolean MOCK_REQUESTS = false;	// Set this value to false to run against a live BuildMaster installation 
	private MockServer mockServer;
	private ProGet proget;
	
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
    public void before() throws IOException {
		mockServer = new MockServer(MOCK_REQUESTS, System.out);
		proget = new ProGet(mockServer.getProGetConfig());
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
		String origUrl = mockServer.getProGetConfig().url; 
		mockServer.getProGetConfig().url = "http://buildmaster1";
				
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
	public void getPackages() throws IOException  {
		Feed feed = proget.getFeed("Example");
		
		ProGetPackage[] packages = proget.getPackages(feed.Feed_Id);
    	
        assertThat("Expect more than one package", packages.length, is(greaterThan(0)));
	}
	
	@Test
	public void downloadPackage() throws IOException  {
		Feed feed = proget.getFeed("Example");
		
		ProGetPackage proGetPackage = proget.getPackages(feed.Feed_Id)[0];
		
		File downloaded = proget.downloadPackage("Example", proGetPackage, folder.getRoot().getAbsolutePath());
    	
        assertThat("File has content", downloaded.length(), is(greaterThan((long)1000)));
	}
	
}
