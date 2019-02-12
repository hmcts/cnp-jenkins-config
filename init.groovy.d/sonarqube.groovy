import hudson.plugins.sonar.SonarGlobalConfiguration
import hudson.plugins.sonar.SonarInstallation

def String getNonEmptySecret(String secretName) {
    def secretsLocation = System.getenv("SECRETS") ?: "/run/secrets"
    def variable = new File("${secretsLocation}/${secretName}").getText('UTF-8')
    if (variable == null || variable.isEmpty()) {
        println secretName + " was not found, exiting"
        System.exit(1)
    }
    variable
}

def sonarApiKey = getNonEmptySecret("sonarcloud-api-token")

def sonarGlobalConfig = SonarGlobalConfiguration.get()
def installation = new SonarInstallation("SonarQube",
        "https://sonarcloud.io",
        sonarApiKey,
        null,
        null,
        null,
        "sonar.organization=hmcts"
)

sonarGlobalConfig.setInstallations(installation)
sonarGlobalConfig.save()
