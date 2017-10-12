#!/usr/bin/env groovy
import jenkins.model.*
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView
import groovy.json.JsonSlurper
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.model.TriggersConfig
import static hudson.plugins.sonar.utils.SQServerVersions.SQ_5_3_OR_HIGHER

java.util.logging.Logger.getLogger("LABS").info( 'running configure-jenkins.groovy' )

def owner = null;

// delete default OpenShift job
Jenkins.instance.items.findAll {
  job -> job.name == 'OpenShift Sample'
}.each {
  job -> job.delete()
}

// create a default build monitor view that includes all jobs
// https://wiki.jenkins-ci.org/display/JENKINS/Build+Monitor+Plugin
if ( Jenkins.instance.views.findAll{ view -> view instanceof com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView }.size == 0){
  view = new BuildMonitorView('Build Monitor','Build Monitor')
  view.setIncludeRegex('.*')
  Jenkins.instance.addView(view)
}

def sonarConfig = Jenkins.instance.getDescriptor('hudson.plugins.sonar.SonarGlobalConfiguration')

def tokenName = 'Jenkins'

def sonarHost = "http://sonarqube:9000"

def revokeToken = new URL("${sonarHost}/api/user_tokens/revoke").openConnection()
def message = "name=Jenkins&login=admin"
revokeToken.setRequestMethod("POST")
revokeToken.setDoOutput(false)
revokeToken.setRequestProperty("Accept", "application/json")
def authString = "admin:admin".bytes.encodeBase64().toString()
def rc = revokeToken.getResponseCode()

def generateToken = new URL("${sonarHost}/api/user_tokens/generate").openConnection()
message = "name=${tokenName}&login=admin"
generateToken.setRequestMethod("POST")
generateToken.setDoOutput(true)
generateToken.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
generateToken.setRequestProperty("Authorization", "Basic ${authString}")
generateToken.getOutputStream().write(message.getBytes("UTF-8"))
rc = generateToken.getResponseCode()

def token = null

if (rc == 200) {
    def jsonBody = generateToken.getInputStream().getText()
    def jsonParser = new JsonSlurper()
    def data = jsonParser.parseText(jsonBody)
    token = data.token
    def jenkins = new SonarInstallation(
        "Sonar", sonarHost, SQ_5_3_OR_HIGHER, token, "", "", "", "", "", new TriggersConfig(), "", ""
    )
    sonarConfig.setInstallations(jenkins)
    sonarConfig.save()
} else {
    println("Request failed: ${rc}")
    println(generateToken.getErrorStream().getText())
}

// support custom CSS for htmlreports
// https://stackoverflow.com/questions/35783964/jenkins-html-publisher-plugin-no-css-is-displayed-when-report-is-viewed-in-j
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")

// This is a helper to delete views in the Jenkins script console if needed
// Jenkins.instance.views.findAll{ view -> view instanceof com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView }.each{ view -> Jenkins.instance.deleteView( view ) }