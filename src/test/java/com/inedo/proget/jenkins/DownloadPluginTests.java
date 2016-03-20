package com.inedo.proget.jenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import com.inedo.proget.MockServer;
import com.inedo.proget.api.ProGetConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import hudson.EnvVars;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

/**
 * Tests for the TriggerBuildHelper class
 * 
 * @author Andrew Sumner
 */
public class DownloadPluginTests {
	public MockServer mockServer;
		
	@SuppressWarnings("rawtypes")
	public AbstractBuild build;
	@SuppressWarnings("rawtypes")	
	public AbstractProject project;
	//public Launcher launcher;
	public BuildListener listener;
	public EnvVars env;
	public ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	public PrintStream logger = new PrintStream(outContent);
	
	public String releaseNumber;
	public String buildNumber;
		
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	@Rule public JenkinsRule j = new JenkinsRule();
	
	@Before
	public void before() throws IOException, InterruptedException {
		mockServer = new MockServer(false);
		
		build = mock(AbstractBuild.class);
		//launcher = mock(Launcher.class);
		listener = mock(BuildListener.class);
		env = mock(EnvVars.class);
		project = mock(AbstractProject.class);
		
		when(build.getProject()).thenReturn(project);
		when(build.getEnvironment(listener)).thenReturn(env);
		when(env.expand(anyString())).then(returnsFirstArg());
		when(listener.getLogger()).thenReturn(logger);
	
	}
	
	@After
	public void tearDown() throws Exception {
		mockServer.stop();
	}
	
	@Test
	public void perform() throws InterruptedException, ExecutionException, IOException, SAXException {
		
		//TriggerableData data = new TriggerableData(MockServer.APPLICATION_ID, releaseNumber, buildNumber);
		String feedName = "Example";
		String groupName = "andrew/sumner/example";
		String packageName = "examplepackage";
		String version = "0.0.1";
		String downloadFolder = folder.getRoot().getAbsolutePath();
		boolean unpack = false;
		
		restLog();
		
		DownloadPackageBuilder download = new DownloadPackageBuilder(feedName, groupName, packageName, version, downloadFolder, unpack);
		
		ProGetConfig config = mockServer.getProGetConfig();
		
		ProGetHelper.injectConfiguration(config);
		
		
		
//		JenkinsRule.WebClient webClient = j.createWebClient();
//		HtmlPage globalConfigPage = webClient.goTo("/configure");
//		
////		 HtmlPage p = j.createWebClient().goTo("/configure");        
//	        HtmlForm form = globalConfigPage.getFormByName("configure");        
//	        
//	        HtmlInput url = form.getInputByName("_.url");
//	        url.setValueAttribute(config.url);
//
//	        HtmlInput user = form.getInputByName("_.user");
//	        user.setValueAttribute(config.user);
//	        
//	        HtmlInput password = form.getInputByName("_.password");
//	        password.setValueAttribute(config.password);
//	        
//	        form.submit();
	        
		FreeStyleProject project = j.createFreeStyleProject();
		project.getBuildersList().add(download);
		FreeStyleBuild build = project.scheduleBuild2(0).get();
				
		assertThat("Result should be successful", build.getResult() , is(Result.SUCCESS));
		
		String log[] = extractLogLinesRemovingApiCall();
		assertThat("Create Build step should be the last actioned performed.", log[log.length - 1], containsString("Create BuildMaster build with BuildNumber="));
		
		String log2 = extractLog();
		assertThat("Create Build step should be the last actioned performed.", log2, containsString("Create BuildMaster build with BuildNumber="));
	}
		
	// Mocking of Server
	private void restLog() {
		logger.flush();
		outContent.reset();
	}
	
	private String extractLog() throws UnsupportedEncodingException {
		return URLDecoder.decode(outContent.toString(), "UTF-8");
	}
	
	private String[] extractLogLines() {
		return outContent.toString().split("[\\r\\n]+");
	}
	
	private String[] extractLogLinesRemovingApiCall() {
		ArrayList<String> out = new ArrayList<String>(Arrays.asList(extractLogLines()));
		
		for (Iterator<String> iterator = out.iterator(); iterator.hasNext();) {
		    String string = iterator.next();
		    if (string.contains("Executing request")) {
		        iterator.remove();
		    }
		}
		
		return out.toArray(new String[0]);
	}
}