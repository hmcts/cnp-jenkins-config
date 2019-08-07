# Jenkins CI

[![Build Status](https://travis-ci.org/geerlingguy/ansible-role-jenkins.svg?branch=master)](https://travis-ci.org/geerlingguy/ansible-role-jenkins)

Configuration for jenkins at HMCTS

Mostly uses the [configuration-as-code-plugin](https://plugins.jenkins.io/configuration-as-code) for configuring

## Requirements

## Testing locally
See https://github.com/hmcts/cnp-jenkins-docker

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
