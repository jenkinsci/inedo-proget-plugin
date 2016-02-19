[![Build Status](https://jenkins.ci.cloudbees.com/job/plugins/job/inedo-buildmaster-plugin/badge/icon)](https://jenkins.ci.cloudbees.com/job/plugins/job/inedo-buildmaster-plugin/)

This plugin allows Jenkins to request version information and trigger a build on an application in [Inedo BuildMaster](http://inedo.com/buildmaster) as part of a Jenkins build process.

See the [Wiki page](http://wiki.jenkins-ci.org/display/JENKINS/Inedo+BuildMaster+Plugin) for more details.

## Building The Plugin
-------------------

The plugin is built using <a href="http://www.gradle.org/">Gradle</a> and the <a href="https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin">Gradle Jenkins JPI Plugin</a>.  The code base includes the Gradle Wrapper, which will automatically download the correct version of Gradle. 

Gradle can be run from the command line or from your IDE:

Command line
============
From the command line, `cd` to the folder containing a copy of this project, and run 

  `./gradlew clean jpi` on Unix-based systems, or 
  
  `gradlew clean jpi` on Windows.
  
  `gradlew -Dhttp.proxyHost=yourProxy -Dhttp.proxyPort=yourPort -Dhttp.proxyUser=yourUsername -Dhttp.proxyPassword=yourPassword -Dhttps.proxyHost=yourProxy -Dhttps.proxyPort=yourPort -Dhttps.proxyUser=yourUsername -Dhttps.proxyPassword=yourPassword clean jpi` from behind a proxy.It is vital that any tasks come after the proxy configuration. 

This will download the required dependencies, clean the existing project, recompile all source code and build the jpi file required by jenkins. 

IDE
===
For Eclipse and NetBeans, you will need to install a Gradle plugin to your IDE before importing the project. See [Gradle tooling](https://www.gradle.org/tooling) for details.

On importing the project to your IDE, the required dependencies will be downloaded.

## Developing The Plugin
-------------------

To spin up a Jenkins instance with this plugin installed for manual testing, run `gradlew clean server` (see "building the plugin" above). The Jenkins instance will be available on port 8080 on your localhost.
