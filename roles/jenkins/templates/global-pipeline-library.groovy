import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import jenkins.plugins.git.GitSCMSource
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries

def GlobalLibraries globalLibsConfigurator = Jenkins.getInstance().getDescriptor("org.jenkinsci.plugins.workflow.libs.GlobalLibraries")

def nameToAdd = "{{ global_lib.name }}"

LibraryConfiguration libToAdd =
    new LibraryConfiguration(nameToAdd,
        new SCMSourceRetriever(
            new GitSCMSource(
                null,
                remote = "git@github.com:hmcts/moj-jenkins-library.git",
                credentialsId = "git_access_key",
                includes = "*",
                excludes = "",
                ignoreOnPushNotifications = {{ global_lib.ignore_on_push_notifications }})))
libToAdd.setDefaultVersion("{{ global_lib.default_version }}")
libToAdd.setImplicit({{ global_lib.set_implicit }})
libToAdd.setAllowVersionOverride({{ global_lib.allow_version_override }});
libToAdd.setIncludeInChangesets(false);

def preConfiguredLibs = globalLibsConfigurator.get().getLibraries()
if (preConfiguredLibs != null) {
  println "Libs alredy configured: '${preConfiguredLibs}'"
  if (!libAlreadyExists(preConfiguredLibs, nameToAdd))
    globalLibsConfigurator.setLibraries(preConfiguredLibs + [libToAdd])
  else
    println "Library with '${nameToAdd}' name already exists! No action taken"
    //throw new IllegalArgumentException("Library with '${nameToAdd}' name already exists! Note: case insensitive name search executed")
}
else
  globalLibsConfigurator.setLibraries([libToAdd])

private boolean libAlreadyExists(list, name) {
  return list.any { return it.getName().equals(name) }
}
