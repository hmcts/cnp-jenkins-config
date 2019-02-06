import jenkins.model.JenkinsLocationConfiguration

def location = JenkinsLocationConfiguration.get()
location.setUrl("${EXTERNAL_URL}")
location.setAdminAddress("jenkins-reform@hmcts.net")
location.save()
