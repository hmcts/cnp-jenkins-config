# Jenkins CI

Installs Jenkins CI on RHEL/CentOS and Debian/Ubuntu servers.


## Getting Started

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
