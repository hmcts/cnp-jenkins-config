#!groovy
import jenkins.*
import jenkins.model.*
import hudson.model.*
import hudson.tasks.Shell
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry
import hudson.model.Node.Mode

def populateEnvVarList(envVarList, propertyConfigKey){
  propertiesConfig[propertyConfigKey].each{ envVar ->
  	try {
    	envVarList.add(new Entry(envVar.NAME, envVar.VALUE))
  	} catch (MissingMethodException e) {
    	jenkins.doSafeExit(null)
    	System.exit(1)
  	}
  }
}

Jenkins jenkins = Jenkins.getInstance()

propertiesConfig = ['sandbox': [['NAME': 'NONPROD_SUBSCRIPTION_NAME', 'VALUE': 'sandbox'],
                                ['NAME': 'NONPROD_ENVIRONMENT_NAME', 'VALUE': 'saat'],
                                ['NAME': 'PROD_SUBSCRIPTION_NAME', 'VALUE': 'sandbox'],
                                ['NAME': 'PROD_ENVIRONMENT_NAME', 'VALUE': 'sprod'],
                                ['NAME': 'INFRA_VAULT_NAME', 'VALUE': 'infra-vault-sandbox']],
                    'prod'   : [['NAME': 'INFRA_VAULT_NAME', 'VALUE': 'infra-vault']],
                    'common' : [['NAME': 'JAVA_OPTS', 'VALUE': '-Xmx2g -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8'],
                                ['NAME': 'GRADLE_OPTS', 'VALUE': '-Dorg.gradle.jvmargs="-Xmx2048m -XX:+HeapDumpOnOutOfMemoryError"']]]

List<Entry> envVarList = new ArrayList<Entry>()
populateEnvVarList(envVarList, "${jenkins_env}")
populateEnvVarList(envVarList, "common")

jenkins.getGlobalNodeProperties().replaceBy(
    Collections.singleton(
        new EnvironmentVariablesNodeProperty(envVarList)
    )
)

jenkins.save()
