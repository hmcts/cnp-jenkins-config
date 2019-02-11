#!/bin/bash
set -e

subscription=${1}

function usage() {
    echo "./bootstrap-secrets.sh <subscription>"
}

if [ -z ${subscription} ]; then
  usage
  exit 1
fi


function readSecret () {
    local secret_name="${1}"
    rm -f secrets/${secret_name}
    az keyvault secret download --vault-name infra-vault-${subscription} --name ${secret_name} --query value -o tsv -f secrets/${secret_name}
}

readSecret slack-token

readSecret hmcts-github-apikey-cmc
readSecret hmcts-github-apikey-div
readSecret hmcts-github-apikey-finrem
readSecret hmcts-github-apikey-cdm
readSecret hmcts-github-apikey-iac
readSecret hmcts-github-apikey-platform
readSecret hmcts-github-apikey-rpa
readSecret hmcts-github-apikey-sscs
readSecret hmcts-github-apikey-probate
readSecret hmcts-github-apikey-feepay
readSecret hmcts-github-apikey-sl
readSecret hmcts-github-apikey-cnp
readSecret hmcts-github-apikey-idam
readSecret hmcts-github-apikey-cet
readSecret hmcts-github-apikey-fpl
readSecret hmcts-github-apikey-am
readSecret hmcts-github-apikey-ethos
readSecret hmcts-github-apikey-ctsc

readSecret hmcts-github-apikey

readSecret mgmt-bastion-creds-password

readSecret sonarcloud-api-token
readSecret pipelinemetrics-cosmosdb-key
readSecret OWASPDb-Password
readSecret jenkins-build-logs-key

readSecret sauce-access-key

readSecret bfa-user-password

readSecret jenkins-sp-subscription-id
readSecret jenkins-sp-client-id
readSecret jenkins-sp-client-secret
readSecret jenkins-sp-tenant-id
