private boolean isSandbox() {
    def locationConfig = jenkins.model.JenkinsLocationConfiguration.get()
    if (locationConfig != null && locationConfig.getUrl() != null) {
      locationConfig.getUrl().contains("sandbox")
    } else {
        System.getenv("ENVIRONMENT") == "sandbox"
    }
}

List<Map> orgs = [
        [name: 'CMC'],
        [name: 'DIV', displayName: "Divorce"],
        [name: 'CNP'],
        [name: 'FinRem', displayName: 'Financial Remedy', regex: 'finrem.*'],
        [name: 'CDM', regex: '\\b(?:document-management-store-app|dm-shared-infrastructure|ccd.*)\\b'],
        [name: 'IAC', regex: 'ia.*'],
        [name: 'BSP', regex: '(send-letter-client|send-letter-service|send-letter-performance-tests|bulk-scan-.*|blob-router-service|reform-scan-.*)'],
        [name: 'RPA', regex: '(rpa-.*|prd-.*|rpx-.*)'],
        [name: 'SSCS'],
        [name: 'Probate'],
        [name: 'FeePay', displayName: 'Fees and Pay', regex: '(ccfr.*|ccpay.*|bar.*)'],
        [name: 'SL', regex: 'snl.*'],
        [name: 'DevOps', nightlyDisabled: true],
        [name: 'IDAM', regex: '(idam-.*|cnp-idam-.*)'],
        [name: 'CET'],
        [name: 'FPL'],
        [name: 'AM'],
        [name: 'ETHOS', displayName: "Ethos replacement"],
        [name: 'CTSC'],
        [name: 'RD', displayName: 'Ref Data'],
        [name: 'MI', displayName: 'Management Information']
]
orgs.each { Map org ->
    githubOrg(org).call()
    org << [nightly: true]
    if (!org.nightlyDisabled) {
        githubOrg(org).call()
    }
}

/**
 * Creates a github organisation
 * @param args map of arguments
 *  - name: the name of the organisation
 *  - displayName (optional, name will be used by default): display name, will be prefixed by HMCTS -
 *  - regex (optional, name.* will be used by default): regex to use for finding repos owned by this team
 *  - jenkinsfilePath (advanced use only): custom jenkinsfile path
 *  - suppressDefaultJenkinsfile: don't use the default Jenkinsfile
 *  - nightly: whether this is nightly org automatically set by the dsl
 */
Closure githubOrg(Map args = [:]) {
    def config = [
            displayName                    : args.name,
            regex                          : args.name.toLowerCase() + '.*',
            jenkinsfilePath                : isSandbox() ? 'Jenkinsfile_parameterized' : 'Jenkinsfile_CNP',
            suppressDefaultJenkinsfile     : false,
            disableNamedBuildBranchStrategy: false,
            credentialId                   : "jenkins-github-hmcts-api-token_" + args.name.toLowerCase()
    ] << args
    def name = config.name

    String jenkinsfilePath = config.jenkinsfilePath

    String folderSandboxPrefix = isSandbox() ? 'Sandbox_' : ''
    GString orgFolderName = "HMCTS_${folderSandboxPrefix}${name}"
    String wildcardBranchesToInclude = isSandbox() ? '*' : 'master hmctsdemo demo PR* perftest ithc preview ethosldata'
    GString orgDescription = "<br>${config.displayName} team repositories"

    String displayNamePrefix = "HMCTS"

    if (config.branchesToInclude) {
        wildcardBranchesToInclude = config.branchesToInclude
    }

    boolean suppressDefaultJenkinsfile = config.suppressDefaultJenkinsfile

    if (config.nightly) {
        orgFolderName = "HMCTS_${folderSandboxPrefix}Nightly_${name}"
        //noinspection GroovyAssignabilityCheck
        orgDescription = "<br>Nightly tests for ${config.displayName}  will be scheduled using this organisation on the AAT Version of the application"

        displayNamePrefix += " Nightly Tests"
        wildcardBranchesToInclude = "master"

        jenkinsfilePath = isSandbox() ? 'Jenkinsfile_nightly_sandbox' : 'Jenkinsfile_nightly'
        suppressDefaultJenkinsfile = true
    }

    return {
        organizationFolder(orgFolderName) {
            description(orgDescription)
            displayName("${displayNamePrefix} - ${config.displayName}")
            organizations {
                github {
                    repoOwner("HMCTS")
                    apiUri("https://api.github.com")
                    credentialsId(config.credentialId)
                }
            }

            orphanedItemStrategy {
                discardOldItems {
                    daysToKeep(7)
                    numToKeep(10)
                }
            }

            projectFactories {
                workflowMultiBranchProjectFactory {
                    scriptPath(jenkinsfilePath)
                }
                if (!suppressDefaultJenkinsfile) {
                    workflowMultiBranchProjectFactory {
                        scriptPath("Jenkinsfile")
                    }
                }
            }
            configure { node ->
                def traits = node / navigators / 'org.jenkinsci.plugins.github__branch__source.GitHubSCMNavigator' / traits
                traits << 'jenkins.scm.impl.trait.RegexSCMSourceFilterTrait' {
                    regex(config.regex)
                }
                traits << 'jenkins.scm.impl.trait.WildcardSCMHeadFilterTrait' {
                    includes(wildcardBranchesToInclude)
                    excludes()
                }
                traits << 'org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait' {
                    strategyId(1)
                }
                traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
                    strategyId(1)
                }

                if (isSandbox() || config.nightly) {
                    def label = isSandbox() && !config.nightly ? "Jenkins - sandbox" : null
                    if (label == null) {
                        label = isSandbox() ? "Jenkins - sandbox nightly" : "Jenkins - nightly"
                    }
                    traits << 'org.jenkinsci.plugins.githubScmTraitNotificationContext.NotificationContextTrait' {
                        contextLabel(label)
                        typeSuffix(false)
                    }
                }

                // prevent builds triggering automatically from SCM push for sandbox and nightly builds
                if ((isSandbox() || config.nightly) && !config.disableNamedBuildBranchStrategy) {
                    node / buildStrategies / 'jenkins.branch.buildstrategies.basic.NamedBranchBuildStrategyImpl'(plugin: 'basic-branch-build-strategies@1.1.1') {
                        filters()
                    }
                }
            }
        }
    }
}
