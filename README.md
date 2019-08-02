# Jenkins CI

[![Build Status](https://travis-ci.org/geerlingguy/ansible-role-jenkins.svg?branch=master)](https://travis-ci.org/geerlingguy/ansible-role-jenkins)

Installs Jenkins CI on RHEL/CentOS and Debian/Ubuntu servers.

## Requirements

Requires `curl` to be installed on the server. Also, newer versions of Jenkins require Java 8+ (see the test playbooks inside the `tests/` directory for an example of how to use newer versions of Java for your OS).

## Using Vagrant to Test This Role
## Background
Vagrant and VirtualBox (or some other VM provider) can be used to quickly build or rebuild virtual servers.

This Vagrant profile installs Jenkins (running on Java) using the Ansible provisioner.

## Getting Started
This README file is inside a folder that contains a Vagrantfile (hereafter this folder shall be called the [vagrant_root]), which tells Vagrant how to set up your virtual machine in VirtualBox.

To use the vagrant file, you will need to have done the following:

Download and Install VirtualBox
Download and Install Vagrant
Install Ansible
Open a shell prompt (Terminal app on a Mac) and cd into the folder containing the Vagrantfile
Once all of that is done, you can simply type in vagrant up, and Vagrant will create a new VM, install the base box, and configure it.

Once the new VM is up and running (after vagrant up is complete and you're back at the command prompt), you can log into it via SSH if you'd like by typing in vagrant ssh. Otherwise, the next steps are below.

Setting up your hosts file
You need to modify your host machine's hosts file (Mac/Linux: /etc/hosts; Windows: %systemroot%\system32\drivers\etc\hosts), adding the line below:

192.168.33.55  jenkins
(Where jenkins) is the hostname you have configured in the Vagrantfile).

After that is configured, you could visit http://jenkins:8080/ in a browser, and you'll see the Jenkins home page.

## Role Variables

Available variables are listed below, along with default values (see `defaults/main.yml`):

    jenkins_package_state: present

The state of the `jenkins` package install. By default this role installs Jenkins but will not upgrade Jenkins (when using package-based installs). If you want to always update to the latest version, change this to `latest`.

    jenkins_hostname: localhost

The system hostname; usually `localhost` works fine. This will be used during setup to communicate with the running Jenkins instance via HTTP requests.

    jenkins_home: /var/lib/jenkins

The Jenkins home directory which, amongst others, is being used for storing artifacts, workspaces and plugins. This variable allows you to override the default `/var/lib/jenkins` location.

    jenkins_http_port: 8080

The HTTP port for Jenkins' web interface.

    jenkins_admin_username: admin
    jenkins_admin_password: admin

Default admin account credentials which will be created the first time Jenkins is installed.

    jenkins_admin_password_file: ""

Default admin password file which will be created the first time Jenkins is installed as /var/lib/jenkins/secrets/initialAdminPassword

    jenkins_admin_token: ""

A Jenkins API token (generated after installation) for [authenticated scripted clients](https://wiki.jenkins-ci.org/display/JENKINS/Authenticating+scripted+clients). You can use the admin token instead of a username and password for more convenient scripted access to Jenkins (e.g. for plugin management through this role).

    jenkins_admin_token_file: ""

A file (with full path) on the Jenkins server containing the admin token. If this variable is set in addition to the `jenkins_admin_token`, the contents of this file will overwrite the value of `jenkins_admin_token`.

    jenkins_plugins: []

Jenkins plugins to be installed automatically during provisioning.

    jenkins_plugins_state: present

Use `latest` to ensure all plugins are running the most up-to-date version.

    jenkins_plugin_updates_expiration: 86400

Number of seconds after which a new copy of the update-center.json file is downloaded. Set it to 0 if no cache file should be used.

    jenkins_plugin_timeout: 30

The server connection timeout, in seconds, when installing Jenkins plugins.

    jenkins_version: "1.644"
    jenkins_pkg_url: "http://www.example.com"

(Optional) Then Jenkins version can be pinned to any version available on `http://pkg.jenkins-ci.org/debian/` (Debian/Ubuntu) or `http://pkg.jenkins-ci.org/redhat/` (RHEL/CentOS). If the Jenkins version you need is not available in the default package URLs, you can override the URL with your own; set `jenkins_pkg_url` (_Note_: the role depends on the same naming convention that `http://pkg.jenkins-ci.org/` uses).

    jenkins_url_prefix: ""

Used for setting a URL prefix for your Jenkins installation. The option is added as `--prefix={{ jenkins_url_prefix }}` to the Jenkins initialization `java` invocation, so you can access the installation at a path like `http://www.example.com{{ jenkins_url_prefix }}`. Make sure you start the prefix with a `/` (e.g. `/jenkins`).

    jenkins_connection_delay: 5
    jenkins_connection_retries: 60

Amount of time and number of times to wait when connecting to Jenkins after initial startup, to verify that Jenkins is running. Total time to wait = `delay` * `retries`, so by default this role will wait up to 300 seconds before timing out.

    # For RedHat/CentOS (role default):
    jenkins_repo_url: http://pkg.jenkins-ci.org/redhat/jenkins.repo
    jenkins_repo_key_url: http://pkg.jenkins-ci.org/redhat/jenkins-ci.org.key
    # For Debian (role default):
    jenkins_repo_url: deb http://pkg.jenkins-ci.org/debian binary/
    jenkins_repo_key_url: http://pkg.jenkins-ci.org/debian/jenkins-ci.org.key

This role will install the latest version of Jenkins by default (using the official repositories as listed above). You can override these variables (use the correct set for your platform) to install the current LTS version instead:

    # For RedHat/CentOS LTS:
    jenkins_repo_url: http://pkg.jenkins-ci.org/redhat-stable/jenkins.repo
    jenkins_repo_key_url: http://pkg.jenkins-ci.org/redhat-stable/jenkins-ci.org.key
    # For Debian/Ubuntu LTS:
    jenkins_repo_url: deb http://pkg.jenkins-ci.org/debian-stable binary/
    jenkins_repo_key_url: http://pkg.jenkins-ci.org/debian-stable/jenkins-ci.org.key

It is also possible stop the repo file being added by setting  `jenkins_repo_url = ''`. This is useful if, for example, you sign your own packages or run internal package management (e.g. Spacewalk).

    jenkins_java_options: "-Djenkins.install.runSetupWizard=false"

Extra Java options for the Jenkins launch command configured in the init file can be set with the var `jenkins_java_options`. For example, if you want to configure the timezone Jenkins uses, add `-Dorg.apache.commons.jelly.tags.fmt.timeZone=America/New_York`. By default, the option to disable the Jenkins 2.0 setup wizard is added.

    jenkins_init_changes:
      - option: "JENKINS_ARGS"
        value: "--prefix={{ jenkins_url_prefix }}"
      - option: "JENKINS_JAVA_OPTIONS"
        value: "{{ jenkins_java_options }}"

Changes made to the Jenkins init script; the default set of changes set the configured URL prefix and add in configured Java options for Jenkins' startup. You can add other option/value pairs if you need to set other options for the Jenkins init file.

## Dependencies

  - geerlingguy.java

## Example Playbook

```yaml
- hosts: jenkins
  vars:
    jenkins_hostname: jenkins.example.com
  roles:
    - role: geerlingguy.java
    - role: geerlingguy.jenkins
      become: true
```

## Testing locally
Run `./bootstrap-secrets.sh sandbox`, which will fetch the secrets required from the sandbox key vault
Then run `docker-compose up -d`

A fully automated Jenkins image will be spun up exposed on port 8088
The credentials to login are `admin/admin` 

The config is pulled from `cac-test-local.yml`, environment variables set in `docker-compose.yml` and the `init.groovy.d` folder for plugins that don't support `configuration-as-code`
Plugins are defined in `plugins.txt`
Forked plugins are defined in the `Dockerfile`

## Testing a branch of this
This is not optimised for easy testing but in the current state you need to:
 1. Create a branch
 2. Update [casc_location](https://github.com/hmcts/cnp-jenkins-config/blob/master/playbook.yml#L326) to point to your branch, currently its hardcoded to master
 3. Depending on the environment you are pointing to update cac-sandbox or cac-prod changes are (note commented out code is only in cac-sandbox.yml, copy it to prod if you need):
     1. Uncomment Administer:authenticated
     2. Uncomment the local security realm block
     3. Comment out the saml security realm block

The end result will be a secured jenkins with a local user account and everything else identical to a regular sandbox / prod jenkins, you can then iterate by pushing changes on your branch if they are configuration as code related.

Then browsing to `<jenkins-url>/configuration-as-code` and click the reload button to apply the changes live

## License

MIT (Expat) / BSD

## Author Information

This role was created in 2014 by [Jeff Geerling](https://www.jeffgeerling.com/), author of [Ansible for DevOps](https://www.ansiblefordevops.com/).
