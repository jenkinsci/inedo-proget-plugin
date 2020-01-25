Inedo ProGet Plugin
========================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/inedo-proget.svg)](https://plugins.jenkins.io/inedo-proget)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/inedo-proget-plugin.svg?label=changelog)](https://github.com/jenkinsci/inedo-proget-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/inedo-proget.svg?color=blue)](https://plugins.jenkins.io/inedo-proget)

## About this plugin
This plugin integrates [Inedo ProGet](http://inedo.com/proget) with Jenkins allowing Jenkins jobs to create and upload, or download and extract, universal packages.

## Usage
The plugin requires a minimum ProGet version of 4.0.12 or higher to work correctly.

### Installing and configuring the plugin

This plugin can be installed from any Jenkins installation connected to the Internet using the **Plugin Manager** screen.

To configure the plugin:

**First** you need to ensure that an api key as been configured in ProGet at ProGet > Administration > API Keys & Access Logs 
Without this the plugin will still work to a certain point but will have reduced functionality in the job configuration - i.e. you will need to fill in certain details rather than select values from a drop down list, of feeds, groups and packages.

![ProGet API Key](/docs/images/proget_api_key.png)

**Next**, you need to go to Jenkins' system config screen to tell Jenkins where's your ProGet installation resides and the username and password of a user with permission to upload files to ProGet.

![ProGet Configuration](/docs/images/proget_configuration.png)

**Finally**, you need to add either an "ProGet Upload Package" or "ProGet Download Package" build step to your Jenkins job.

#### Upload Package
In it basic form, this simply require specifying the files in your work space that you'd like to package, supplying some metadata that that describes the package and the job is done.
Please consult the help text in the plugin configuration screen for more information on each setting.

![ProGet Upload](/docs/images/proget_upload.png)

There are some more advanced options that allow you to tweak the files that will be included in the package and the supply additional metadata.

![ProGet Upload Advanced](/docs/images/proget_download.png)

#### Download Package
Downloads a universal ProGet package in the requested format (package, zip, or tgz) to specified folder and will optionally unpack it for you.
The environment variable PROGET_FILE will be populated with the name of the downloaded file
Please consult the help text in the plugin configuration screen for more information on each setting.

![ProGet Download](/docs/images/proget_download.png)


#### Pipeline Script
Script can be generated using the pipeline syntax snippet generator.

<table style="border: 1px solid grey; border-collapse: collapse; width: 100%;">
<tr style="text-align: left; background-color: lightgrey">
    <th>Scripted Pipeline Example</th>
</tr>
<tr>
<td><pre>
node {
    bat '''
        DEL *.TXT /Q
        DEL *.upack /Q
        ECHO Build Tag: %BUILD_TAG% > Example.txt
    '''
    uploadProgetPackage artifacts: 'Example.txt', feedName: 'Example', groupName: 'jenkins/pipleline', packageName: 'JenkinsPackage', version: "1.0.${BUILD_NUMBER}"
    downloadProgetPackage downloadFolder: "${WORKSPACE}", downloadFormat: 'pkg', feedName: 'Example', groupName: 'jenkins/pipleline', packageName: 'JenkinsPackage', version: "1.0.${BUILD_NUMBER}"
}
</pre></td>
</tr>
</table>

<table style="border: 1px solid grey; border-collapse: collapse; width: 100%;">
<tr style="text-align: left; background-color: lightgrey">
    <th>Declarative Pipeline Example</th>
</tr>
<tr>
<td><pre>
pipeline {
  agent any
 
  stages {
    stage('Main') {
      steps {
        bat '''
            DEL *.TXT /Q
            DEL *.upack /Q
            ECHO Build Tag: %BUILD_TAG% > Example.txt
        '''
        uploadProgetPackage artifacts: 'Example.txt', feedName: 'Example', groupName: 'jenkins/pipleline', packageName: 'JenkinsPackage', version: "1.0.${BUILD_NUMBER}"
        downloadProgetPackage downloadFolder: "${WORKSPACE}", downloadFormat: 'pkg', feedName: 'Example', groupName: 'jenkins/pipleline', packageName: 'JenkinsPackage', version: "1.0.${BUILD_NUMBER}"
      }
    }
  }
}
</pre></td>
</tr>
</table>

## Reporting an Issue
Select Create Issue on the [JIRA home page](https://issues.jenkins-ci.org/secure/Dashboard.jspa) and ensure that the component is set to inedo-proget-plugin.

For more information see the Jenkins guide on [how to report an issue](https://wiki.jenkins.io/display/JENKINS/How+to+report+an+issue).

## More information

* [Changelog](https://github.com/jenkinsci/inedo-proget-plugin/releases)
* [Developer documentation](./docs/DEVELOPER.md)