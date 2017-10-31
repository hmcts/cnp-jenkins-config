#!groovy
import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.impl.*
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl


def id = args[0]
def type = args[1]
def description = args[2]
def user = args[3]
def secret = args[4]


domain = Domain.global()
store = SystemCredentialsProvider.getInstance().getStore()

credential = getCredential(type, id, description, user, secret)

def success = store.addCredentials(domain, credential)
if (success) {
  println "Created credential with id " + id
} else {
  // Need to update the credential which requires the old one
  for (cred in store.getCredentials(domain)) {
    if (cred.getId().equals(id)) {
      store.updateCredentials(domain, cred, credential)
      println "Updated credential with id " + id
      break
    }
  }
}

private BaseStandardCredentials getCredential(credentialType, id, desc, user, secret) {
  if (credentialType == "username-password") {
    return new UsernamePasswordCredentialsImpl(
      CredentialsScope.GLOBAL,
      id,
      desc,
      user,
      secret
    )
  } else if (credentialType == "string") {
    return  new StringCredentialsImpl(
      CredentialsScope.GLOBAL,
      id,
      desc,
      new Secret(secret)
    )
  } else if (credentialType == "VaultTokenFileCredential") {
    return new com.datapipe.jenkins.vault.credentials.VaultTokenFileCredential(
      CredentialsScope.GLOBAL,
      id,
      desc,
      secret
    )
  } else {
    throw new IllegalArgumentException("Unknown credential type: ${credentialType}")
  }
}
