
private boolean isSandbox() {
    def locationConfig = jenkins.model.JenkinsLocationConfiguration.get()
    if (locationConfig != null && locationConfig.getUrl() != null) {
      locationConfig.getUrl().contains("sandbox")
    } else {
        System.getenv("ENVIRONMENT") == "sandbox"
    }
}

if (isSandbox()) {
    Map pipelineTestOrg = [
            name                           : 'CNP_test',
            displayName                    : 'CNP_test',
            regex                          : 'cnp-plum-.*',
            branchesToInclude              : 'master PR*',
            jenkinsfilePath                : 'Jenkinsfile_parameterized',
            suppressDefaultJenkinsfile     : false,
            disableNamedBuildBranchStrategy: false,
            credentialId                   : 'jenkins-github-hmcts-api-token_cnp'
    ]
    githubOrg(pipelineTestOrg).call()
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
            jenkinsfilePath                : isSandbox() ? 'Jenkinsfile_parameterized' : '',
            suppressDefaultJenkinsfile     : false,
            disableNamedBuildBranchStrategy: false,
            credentialId                   : "jenkins-github-hmcts-api-token_" + args.name.toLowerCase()
    ] << args
    def name = config.name

    String jenkinsfilePath = config.jenkinsfilePath

    String folderSandboxPrefix = isSandbox() ? 'Sandbox_' : ''
    GString orgFolderName = "HMCTS_${folderSandboxPrefix}${name}"
    String wildcardBranchesToInclude = isSandbox() ? '' : ''
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

        jenkinsfilePath = isSandbox() ? 'Jenkinsfile_nightly_sandbox' : ''
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