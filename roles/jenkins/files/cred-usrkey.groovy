#!groovy
import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*

domain = Domain.global()
store = SystemCredentialsProvider.getInstance().getStore()

credential = new BasicSSHUserPrivateKey(
        CredentialsScope.GLOBAL,
        "${cred_id}",
        "${cred_user}",
        new BasicSSHUserPrivateKey.UsersPrivateKeySource(),
        "${cred_secret}",
        "${cred_desc}"
)

def success = store.addCredentials(domain, credential)

if (success) {
    println "Created credential with id ${cred_id}"
} else {
    // Need to update the credential which requires the old one
    for (cred in store.getCredentials(domain)) {
        if (cred.getId().equals(${cred_id})) {
            store.updateCredentials(domain, cred, credential)
            println "Updated credential with id ${cred_id}"
            break
        }
    }
}

