private boolean isSandbox() {
    def locationConfig = jenkins.model.JenkinsLocationConfiguration.get()
    if (locationConfig != null && locationConfig.getUrl() != null) {
      locationConfig.getUrl().contains("sandbox")
    } else {
        System.getenv("ENVIRONMENT") == "sandbox"
    }
}

List<Map> orgs = [
    [name: 'HMCTS', topic: 'jenkins-cft'],
]

orgs.each { Map org ->
    githubOrg(org).call()
    org << [nightly: true]
    if (!org.nightlyDisabled) {
        githubOrg(org).call()
    }
}

if (isSandbox()) {
    Map pipelineTestOrg = [
            name                           : 'Pipeline_Test',
            displayName                    : 'Pipeline Test',
            regex                          : 'cnp-plum-.*|cnp-rhubarb-.*|cnp-jenkins-library',
            branchesToInclude              : 'master PR*',
            jenkinsfilePath                : 'Jenkinsfile_pipeline_test',
            suppressDefaultJenkinsfile     : true,
            disableAgedRefsBranchStrategy  : true,
            credentialId                   : 'hmcts-jenkins-cft'
    ]
    githubOrg(pipelineTestOrg).call()
}

/**
 * Creates a github organisation
 * @param args map of arguments
 *  - name: the name of the organisation
 *  - displayName (optional, name will be used by default): display name, will be prefixed by HMCTS -
 *  - regex (optional, name.* will be used by default): regex to use for finding repos owned by this team
 *  - topic (optional): GitHub topic to use to find repos owned by the team
 *  - jenkinsfilePath (advanced use only): custom jenkinsfile path
 *  - suppressDefaultJenkinsfile: don't use the default Jenkinsfile
 *  - nightly: whether this is nightly org automatically set by the dsl
 */
Closure githubOrg(Map args = [:]) {
    def config = [
            displayName                    : args.name,
            jenkinsfilePath                : isSandbox() ? 'Jenkinsfile_parameterized' : 'Jenkinsfile_CNP',
            suppressDefaultJenkinsfile     : false,
            enableNamedBuildBranchStrategy : false,
            credentialId                   : "hmcts-jenkins-cft"
    ] << args
    def folderName = config.name

    String jenkinsfilePath = config.jenkinsfilePath

    def runningOnSandbox = isSandbox()
    GString orgDescription = "<br>${config.displayName} team repositories"

    def orgDisplayName = config.displayName

    String folderSuffix = ''
    String wildcardBranchesToInclude = 'master demo PR-* perftest ithc preview ethosldata'
    boolean suppressDefaultJenkinsfile = config.suppressDefaultJenkinsfile
    boolean enableNamedBuildBranchStrategy = config.enableNamedBuildBranchStrategy

    if (runningOnSandbox) {
        folderSuffix = '_Sandbox'
        wildcardBranchesToInclude = '*'
        // We want the labs folder to build on push but others don't need to
        enableNamedBuildBranchStrategy = config.name == 'LABS' ? false : true
    }
    GString orgFolderName = "${folderName}${folderSuffix}"

    if (config.branchesToInclude) {
        wildcardBranchesToInclude = config.branchesToInclude
    }

    if (config.nightly) {
        orgFolderName = "${folderName}_Nightly${folderSuffix}"
        //noinspection GroovyAssignabilityCheck
        orgDescription = "<br>Nightly tests for ${orgDisplayName}  will be scheduled using this organisation on the AAT Version of the application"

        orgDisplayName += " Nightly Tests"
        wildcardBranchesToInclude = "master nightly-dev"

        jenkinsfilePath = runningOnSandbox ? 'Jenkinsfile_nightly_sandbox' : 'Jenkinsfile_nightly'
        suppressDefaultJenkinsfile = true
        enableNamedBuildBranchStrategy = true
    }

    return {
        organizationFolder(orgFolderName) {
            description(orgDescription)
            displayName(orgDisplayName)
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

                if (config.regex) {
                    traits << 'jenkins.scm.impl.trait.RegexSCMSourceFilterTrait' {
                        regex(config.regex)
                    }
                }

                if (config.topic) {
                    traits << 'org.jenkinsci.plugins.github__branch__source.TopicsTrait' {
                        topicList(config.topic)
                    }
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

                if (config.nightly) {
                    traits << 'io.jenkins.plugins.checks.github.status.GitHubSCMSourceStatusChecksTrait' {
                        // TODO enable skip globally at some point so we don't have 2 job statuses
                        // not doing right now as tons of people will have it in their required commit statuses
                        skipNotifications(true)
                        def label = runningOnSandbox ? "Jenkins - sandbox nightly" : "Jenkins - nightly"
                        name(label)
                    }
                }

                if (!config.nightly && !config.disableAgedRefsBranchStrategy) {
                    traits << 'org.jenkinsci.plugins.scm_filter.GitHubAgedRefsTrait' {
                        retentionDays(30)
                    }
                }

                // prevent builds triggering automatically from SCM push for sandbox and nightly builds
                if (enableNamedBuildBranchStrategy) {
                    node / buildStrategies / 'jenkins.branch.buildstrategies.basic.NamedBranchBuildStrategyImpl'(plugin: 'basic-branch-build-strategies@1.3.2') {
                        filters()
                    }
                }

                node / buildStrategies / 'jenkins.branch.buildstrategies.basic.SkipInitialBuildOnFirstBranchIndexing'(plugin: 'basic-branch-build-strategies@1.3.2') {
                }
            }
        }
    }
}
