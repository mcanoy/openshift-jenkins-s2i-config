#!/usr/bin/env groovy
import jenkins.model.*
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView
import groovy.json.JsonSlurper
import hudson.plugins.sonar.model.TriggersConfig
import hudson.plugins.sonar.*
import hudson.tools.*
import java.util.logging.Level
import java.util.logging.Logger
import static hudson.plugins.sonar.utils.SQServerVersions.SQ_5_3_OR_HIGHER

final def LOG = Logger.getLogger("LABS")

LOG.log(Level.INFO, 'Get Simple SonarQube config')
def sonarConfig = Jenkins.instance.getDescriptor('hudson.plugins.sonar.SonarGlobalConfiguration')

def sonarHost = System.getenv("OPENSHIFT_SIMPLE_SONARQUBE")
LOG.log(Level.INFO, "Sonar Host: " + sonarHost)
if (sonarHost != null) {
    
    // Add the SonarQube server config to Jenkins
    SonarInstallation sonarInst = new SonarInstallation(
        "rh-labs-sonar", sonarHost, SQ_5_3_OR_HIGHER, null, "", "", "", "", "", new TriggersConfig(), "", "", ""
        )

    sonarConfig.setInstallations(sonarInst)
    sonarConfig.setBuildWrapperEnabled(true)
    sonarConfig.save()
    LOG.log(Level.INFO, 'SonarQube plugin configuration saved')

    // Sonar Runner
    // Source: http://pghalliday.com/jenkins/groovy/sonar/chef/configuration/management/2014/09/21/some-useful-jenkins-groovy-scripts.html

    def inst = Jenkins.getInstance()

    def sonarRunner = inst.getDescriptor("hudson.plugins.sonar.SonarRunnerInstallation")

    def installer = new SonarRunnerInstaller("3.0.3.778")
    def prop = new InstallSourceProperty([installer])
    def sinst = new SonarRunnerInstallation("sonar-scanner-tool", "", [prop])
    sonarRunner.setInstallations(sinst)

    sonarRunner.save()
}