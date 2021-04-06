private boolean isSandbox() {
    def locationConfig = jenkins.model.JenkinsLocationConfiguration.get()
    if (locationConfig != null && locationConfig.getUrl() != null) {
      locationConfig.getUrl().contains("sandbox")
    } else {
        System.getenv("ENVIRONMENT") == "sandbox"
    }
}

List<Map> orgs = [
    [name: 'CNP'],
]

orgs.each { Map org ->
    githubOrg(org).call()
    org << [nightly: true]
    if (!org.nightlyDisabled) {
        githubOrg(org).call()
    }
}

Map pipelineTestOrg = [
        name                           : 'Pipeline_Test',
        displayName                    : 'Pipeline Test',
        regex                          : 'cnp-plum-.*|cnp-rhubarb-.*|cnp-jenkins-library',
        branchesToInclude              : 'master PR*',
        jenkinsfilePath                : 'Jenkinsfile_pipeline_test',
        suppressDefaultJenkinsfile     : true,
        disableNamedBuildBranchStrategy: true,
        credentialId                   : 'hmcts-jenkins-cnp'
]
githubOrg(pipelineTestOrg).call()

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
            credentialId                   : "hmcts-jenkins-" + args.name.toLowerCase()
    ] << args
    def folderName = config.name

    String jenkinsfilePath = config.jenkinsfilePath

    def runningOnSandbox = isSandbox()
    String folderSandboxPrefix = runningOnSandbox ? 'Sandbox_' : ''
    GString orgFolderName = "HMCTS_${folderSandboxPrefix}${folderName}"
    String wildcardBranchesToInclude = runningOnSandbox ? '*' : 'master demo PR-* perftest ithc preview ethosldata'
    GString orgDescription = "<br>${config.displayName} team repositories"

    String displayNamePrefix = "HMCTS"

    if (config.branchesToInclude) {
        wildcardBranchesToInclude = config.branchesToInclude
    }

    boolean suppressDefaultJenkinsfile = config.suppressDefaultJenkinsfile

    if (config.nightly) {
        orgFolderName = "HMCTS_${folderSandboxPrefix}Nightly_${folderName}"
        //noinspection GroovyAssignabilityCheck
        orgDescription = "<br>Nightly tests for ${config.displayName}  will be scheduled using this organisation on the AAT Version of the application"

        displayNamePrefix += " Nightly Tests"
        wildcardBranchesToInclude = "master nightly-dev"

        jenkinsfilePath = runningOnSandbox ? 'Jenkinsfile_nightly_sandbox' : 'Jenkinsfile_nightly'
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
                traits << 'org.jenkinsci.plugins.github__branch__source.ExcludeArchivedRepositoriesTrait' {
                }

                traits << 'io.jenkins.plugins.checks.github.status.GitHubSCMSourceStatusChecksTrait' {
                    if (config.nightly) {
                        // TODO enable skip globally at some point so we don't have 2 job statuses
                        // not doing right now as tons of people will have it in their required commit statuses
                        skipNotifications(true)
                        def label = runningOnSandbox ? "Jenkins - sandbox nightly" : "Jenkins - nightly"
                        name(label)
                    }
                    skipProgressUpdates(true)
                }

                // prevent builds triggering automatically from SCM push for sandbox and nightly builds
                if ((runningOnSandbox || config.nightly) && !config.disableNamedBuildBranchStrategy) {
                    node / buildStrategies / 'jenkins.branch.buildstrategies.basic.NamedBranchBuildStrategyImpl'(plugin: 'basic-branch-build-strategies@1.1.1') {
                        filters()
                    }
                }
            }
        }
    }
}
