import jenkins.*
import jenkins.model.*
import hudson.model.*
import hudson.tasks.Shell
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry
import hudson.model.Node.Mode

Jenkins jenkins = Jenkins.getInstance()

Map propertiesConfig = ['ENVIRONMENT_VARIABLES':[['NAME':'NONPROD_SUBSCRIPTION_NAME', 'VALUE':'sandbox'],
                                                 ['NAME':'NONPROD_ENVIRONMENT_NAME', 'VALUE':'saat'],
                                                 ['NAME': 'PROD_SUBSCRIPTION_NAME', 'VALUE': 'sandbox'],
                                                 ['NAME': 'PROD_ENVIRONMENT_NAME', 'VALUE': 'sprod']]]

List<Entry> envVarList = new ArrayList<Entry>()
propertiesConfig.ENVIRONMENT_VARIABLES.each { envVar ->
    try {
        envVarList.add(new Entry(envVar.NAME, envVar.VALUE))
    } catch (MissingMethodException e) {
        jenkins.doSafeExit(null)
        System.exit(1)
    }
}

jenkins.getGlobalNodeProperties().add(
        Collections.singleton(
                new EnvironmentVariablesNodeProperty(envVarList)
        )
)

jenkins.save()
