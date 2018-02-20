import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.microsoft.azure.util.*


def subscription_id = "{{ jenkins_subscription_id }}"
def client_id = "{{ jenkins_client_id }}"
def client_secret = "{{ jenkins_client_secret }}"
def token_id = "{{ jenkins_tenant_id }}"

AzureCredentials azureCred = (AzureCredentials) new AzureCredentials(
    scope= CredentialsScope.GLOBAL,
    id= "jenkinsServicePrincipal",
    description= "Jenkins Service Principal - only has access to infra key vault",
    subscriptionId= "${subscription_id}",
    clientId= "${client_id}",
    clientSecret= "${client_secret}",
    oauth2TokenEndpoint= "https://login.microsoftonline.com/${token_id}/oauth2/token",
    serviceManagementURL= "https://management.core.windows.net/",
    authenticationEndpoint= "https://login.microsoftonline.com/",
    resourceManagerEndpoint= "https://management.azure.com/",
    graphEndpoint= "https://graph.windows.net/")
SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), azureCred)
