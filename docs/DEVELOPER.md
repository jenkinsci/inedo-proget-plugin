Developer notes
===============


## Building The Plugin

The plugin is built using <a href="http://www.gradle.org/">Gradle</a> and the <a href="https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin">Gradle Jenkins JPI Plugin</a>.  The code base includes the Gradle Wrapper, which will automatically download the correct version of Gradle. 

Gradle can be run from the command line or from your IDE:

### Command line

From the command line, `cd` to the folder containing a copy of this project, and run 

  `./gradlew clean jpi` on Unix-based systems, or 
  
  `gradlew clean jpi` on Windows.
  
  `gradlew -Dhttp.proxyHost=yourProxy -Dhttp.proxyPort=yourPort -Dhttp.proxyUser=yourUsername -Dhttp.proxyPassword=yourPassword -Dhttps.proxyHost=yourProxy -Dhttps.proxyPort=yourPort -Dhttps.proxyUser=yourUsername -Dhttps.proxyPassword=yourPassword clean jpi` from behind a proxy. It is vital that any tasks come after the proxy configuration. 

This will download the required dependencies, clean the existing project, recompile all source code and build the jpi file required by jenkins.
 

### IDE

For Eclipse and NetBeans, you will need to install a Gradle plugin to your IDE before importing the project. See [Gradle tooling](https://www.gradle.org/tooling) for details.

On importing the project to your IDE, the required dependencies will be downloaded.


## Testing The Plugin

### Manual

To spin up a Jenkins instance with this plugin installed for manual testing, run `gradlew clean server` (see "building the plugin" above). The Jenkins instance will be available at http://localhost:8080. You may need to specify a path to your JDK, if so use `gradlew clean server -Dorg.gradle.java.home=/JDK_PATH`

To login the username will be admin and the password can be found in <project root>/work/secrets/initialAdminPassword

### Prerequisites
* ProGet:
    * Installed and configured with an API key
    * Add a Feed called Example using the universal package format
    * Add a package to the feed - use the ProGetApiTests.uploadPackage() method as this will put an appropriate sized file there that will allow the tests to pass
* Jenkins:
    * System Configuration page updated with BuildMaster server details and the Test Connection button returning success
    * test-freestyle job added to create a package, upload it, and download it
    * test-pipleline job with this pipeline script definition:
    
```
node {
    ws {
        bat '''DEL *.TXT /Q
        		DEL *.upack /Q
            ECHO Build Tag: %BUILD_TAG% > Example.txt'''
        uploadProgetPackage artifacts: 'Example.txt', feedName: 'Example', groupName: 'jenkins/pipleline', packageName: 'JenkinsPackage', version: "1.0.${BUILD_NUMBER}"
        downloadProgetPackage downloadFolder: "${WORKSPACE}", downloadFormat: 'pkg', feedName: 'Example', groupName: 'jenkins/freestyle', packageName: 'JenkinsPackage', version: "1.0.${BUILD_NUMBER}"
    }
}
```

See the [Wiki page](https://github.com/jenkinsci/inedo-proget-plugin) for more details.

## Automated

Update <project root>/test.properties with the required details and run the tests.  If useMockServer is false then the tests will be run against the installed application, if true it will run against a mock server.  While the mock server is useful for unit testing, the real service is required to test the plugin against application upgrades.

The tests mainly verify the ProGet APIs are still functioning as expected, although there are a couple of tests that attempt to use the plugin from a mocked Jenkins job.  
