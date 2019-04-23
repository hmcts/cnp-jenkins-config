#!groovy
import jenkins.model.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.impl.*
import hudson.plugins.sauce_ondemand.credentials.*;

def domain = Domain.global()
def store = SystemCredentialsProvider.getInstance().getStore()

def credential = new SauceCredentials(
        CredentialsScope.GLOBAL,
        "${cred_id}",
        "${cred_user}",
        "${cred_secret}",
        "https://eu-central-1.saucelabs.com/",
        "${cred_desc}"
)

def success = store.addCredentials(domain, credential)

if (success) {
    println "Created credential with id ${cred_id}"
} else {
    // Need to update the credential which requires the old one
    for (cred in store.getCredentials(domain)) {
        if (cred.getId().equals("${cred_id}")) {
            store.updateCredentials(domain, cred, credential)
            println "Updated credential with id ${cred_id}"
            break
        }
    }
}
