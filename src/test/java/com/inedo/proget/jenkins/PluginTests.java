package com.inedo.proget.jenkins;
//------------------------------------------------------------------------
// TODO Haven't done the upload plugin yet
//------------------------------------------------------------------------

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import com.inedo.proget.api.ProGetConfig;
import com.inedo.utils.MockServer;
import com.inedo.utils.TestConfig;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;

/**
 * Tests for the TriggerBuildHelper class
 *
 * @author Andrew Sumner
 */
public class PluginTests {
    private MockServer mockServer = null;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void before() throws IOException, InterruptedException {
        ProGetConfig config;

        if (TestConfig.useMockServer()) {
            mockServer = new MockServer();
            config = mockServer.getProGetConfig();
        } else {
            config = TestConfig.getProGetConfig();
        }

        // TODO Look at using Mockito to get global configuration rather than injecting it
        GlobalConfig.injectConfiguration(config);
    }

    @After
    public void tearDown() throws Exception {
        if (mockServer != null) {
            mockServer.stop();
        }

        GlobalConfig.injectConfiguration(null);
    }

    @Test
    public void performDownload() throws Exception {
        // TODO doesn't point at valid file
        String feedName = "Example";
        String groupName = "andrew/sumner/proget";
        String packageName = "ExamplePackage";
        String version = "0.0.3";
        DownloadFormat downloadFormat = DownloadFormat.PACKAGE;
        String downloadFolder = folder.getRoot().getAbsolutePath();

        FreeStyleProject project = jenkins.createFreeStyleProject();

        // TODO This belongs in upload rather than download
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("abc.txt").write("hello", "UTF-8");
                return true;
            }
        });

        project.getBuildersList().add(new DownloadPackageBuilder(feedName, groupName, packageName, version, downloadFormat.getFormat(), downloadFolder));

        final HoldFileName progetFile = new HoldFileName();
        
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                progetFile.fileName = build.getEnvironment(listener).expand("${PROGET_FILE}");
                return true;
            }
        });

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertThat("Result is successful", build.getResult() , is(Result.SUCCESS));

        String log = FileUtils.readFileToString(build.getLogFile());
        assertThat("Has logged ProGet actions", log, containsString("[ProGet]"));

        assertThat("PROGET_FILE variable has been set", progetFile.fileName, is(notNullValue()));
        assertThat("PROGET_FILE variable points to existing file", new File(downloadFolder, progetFile.fileName).exists(), is(true));
    }


    @Test
    public void performUpload() throws Exception {
        String feedName = "Example";
        String groupName = "andrew/sumner/proget";
        String packageName = "ExamplePackage";
        String version = "0.0.${BUILD_NUMBER}";
        String artifact = "XX.${BUILD_NUMBER}.TXT";
        
        FreeStyleProject project = jenkins.createFreeStyleProject();

        // TODO This belongs in upload rather than download
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

                build.getWorkspace().child("XX." + build.number + ".TXT").write("hello", "UTF-8");
                return true;
            }
        });

        project.getBuildersList().add(new UploadPackageBuilder(feedName, groupName, packageName, version, artifact));

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertThat("Result is successful", build.getResult() , is(Result.SUCCESS));

        String log = FileUtils.readFileToString(build.getLogFile());
        assertThat("Has logged ProGet actions", log, containsString("[ProGet]"));
    }

    public void setEnvironmentVariables() throws IOException {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("sampleEnvVarKey", "sampleEnvVarValue");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }

    public class HoldFileName {
        public String fileName;
    }
}