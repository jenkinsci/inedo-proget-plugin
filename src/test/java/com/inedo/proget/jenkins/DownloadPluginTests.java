////------------------------------------------------------------------------
//// TODO This is a nearly there, just need to get it passing
////------------------------------------------------------------------------
//
//package com.inedo.proget.jenkins;
//
//import com.inedo.TestConfig;
//import com.inedo.proget.MockServer;
//import com.inedo.proget.api.ProGetConfig;
//import hudson.EnvVars;
//import hudson.Launcher;
//import hudson.model.*;
//import hudson.slaves.EnvironmentVariablesNodeProperty;
//import org.apache.commons.io.FileUtils;
//import org.junit.*;
//import org.junit.rules.TemporaryFolder;
//import org.jvnet.hudson.test.JenkinsRule;
//import org.jvnet.hudson.test.TestBuilder;
//
//import java.io.IOException;
//
//import static org.hamcrest.Matchers.*;
//import static org.junit.Assert.assertThat;
//
///**
// * Tests for the TriggerBuildHelper class
// *
// * @author Andrew Sumner
// */
//public class DownloadPluginTests {
//	private MockServer mockServer = null;
//
//	@Rule public TemporaryFolder folder = new TemporaryFolder();
//    @ClassRule public static JenkinsRule j = new JenkinsRule();
//
//	@Before
//	public void before() throws IOException, InterruptedException {
//        ProGetConfig config;
//
//        if (TestConfig.useMockServer()) {
//            mockServer = new MockServer();
//            config = mockServer.getProGetConfig();
//        } else {
//            config = TestConfig.getProGetConfig();
//        }
//
//        // TODO Look at using Mockito to get global configuration rather than injecting it
//        ProGetHelper.injectConfiguration(config);
//	}
//
//	@After
//	public void tearDown() throws Exception {
//        if (mockServer != null) {
//            mockServer.stop();
//        }
//
//        ProGetHelper.injectConfiguration(null);
//	}
//
//	@Test
//	public void performDownload() throws Exception {
//        // TODO doesn't point at valid file
//        String feedName = "Example";
//        String groupName = "andrew/sumner/example";
//        String packageName = "examplepackage";
//        String version = "0.0.1";
//        DownloadPackageBuilder.DownloadFormat downloadFormat = DownloadPackageBuilder.DownloadFormat.PACKAGE;
//        String downloadFolder = folder.getRoot().getAbsolutePath();
//
//        FreeStyleProject project = j.createFreeStyleProject();
//
//        // TODO This belongs in upload rather than download
//        project.getBuildersList().add(new TestBuilder() {
//            @Override
//            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
//                build.getWorkspace().child("abc.txt").write("hello", "UTF-8");
//                return true;
//            }
//        });
//
//        project.getBuildersList().add(new DownloadPackageBuilder(feedName, groupName, packageName, version, downloadFormat.getFormat(), downloadFolder));
//
//        FreeStyleBuild build = project.scheduleBuild2(0).get();
//
//        assertThat("Result should be successful", build.getResult() , is(Result.SUCCESS));
//
//        String log = FileUtils.readFileToString(build.getLogFile());
//        assertThat(log, containsString("+ echo hello"));
//
//        EnvironmentVariablesNodeProperty prop = j.jenkins.getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
//        EnvVars envVars = prop.getEnvVars();
//        String file = envVars.get("PROGET_FILE");
//        assertThat(file, is(notNullValue()));
//	}
//}