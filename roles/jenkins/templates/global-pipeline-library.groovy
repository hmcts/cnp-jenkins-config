import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import jenkins.plugins.git.GitSCMSource;

def globalLibsDesc = Jenkins.getInstance().getDescriptor("org.jenkinsci.plugins.workflow.libs.GlobalLibraries")
SCMSourceRetriever retriever =
    new SCMSourceRetriever(
        new GitSCMSource(
            id="someId",
            remote="git@github.com:contino/moj-jenkins-library.git",
            credentialsId="",
            includes="*",
            excludes="",
            ignoreOnPushNotifications=false))

LibraryConfiguration pipeline =
    new LibraryConfiguration("Infrastructure", retriever)
        .setDefaultVersion("master")
        .setImplicit(true)

globalLibsDesc.get().setLibraries([pipeline])
