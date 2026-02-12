import org.yaml.snakeyaml.Yaml

private boolean isSandbox() {
    def locationConfig = jenkins.model.JenkinsLocationConfiguration.get()
    if (locationConfig != null && locationConfig.getUrl() != null) {
      locationConfig.getUrl().contains("sandbox")
    } else {
        System.getenv("ENVIRONMENT") == "sandbox"
    }
}

/**
 * Parses deployment-controls.yml to build a set of approved repository names.
 * Only repositories present in this allowlist will be picked up by Jenkins.
 */
Set<String> loadApprovedRepos() {
    def yaml = new Yaml()
    def parsed = yaml.load(readFileFromWorkspace('deployment-controls.yml'))
    return (parsed?.repositories?.collect { entry ->
        // Extract repo name from URL, e.g. https://github.com/hmcts/cnp-plum-frontend.git -> cnp-plum-frontend
        entry.repo.replaceAll('.*/([^/]+?)(\\.git)?$', '$1')
    } ?: []).toSet()
}

Set<String> approvedRepos = loadApprovedRepos()

List<Map> orgs = [
    [name: 'HMCTS_a_to_c', credentialsId: 'hmcts-jenkins-a-to-c', displayName: 'HMCTS - A to C', topic: 'jenkins-cft-a-c'],
    [name: 'HMCTS_d_to_i', credentialsId: 'hmcts-jenkins-d-to-i', displayName: 'HMCTS - D to I', topic: 'jenkins-cft-d-i'],
    [name: 'HMCTS_j_to_z', credentialsId: 'hmcts-jenkins-j-to-z', displayName: 'HMCTS - J to Z', topic: 'jenkins-cft-j-z']
]

orgs.each { Map org ->
    githubOrg(org, approvedRepos).call()
    org << [nightly: true]
    if (!org.nightlyDisabled) {
        githubOrg(org, approvedRepos).call()
    }
}

if (isSandbox()) {
    Map pipelineTestOrg = [
            name                           : 'Pipeline_Test',
            displayName                    : 'HMCTS - Pipeline Test',
            regex                          : 'cnp-plum-.*|cnp-rhubarb-.*|cnp-jenkins-library',
            branchesToInclude              : 'master PR*',
            jenkinsfilePath                : 'Jenkinsfile_pipeline_test',
            credentialsId                  : 'hmcts-jenkins-cft',
            suppressDefaultJenkinsfile     : true,
            disableAgedRefsBranchStrategy  : true,
    ]
    githubOrg(pipelineTestOrg, approvedRepos).call()
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
 * @param approvedRepos set of repository names approved for Jenkins inclusion via deployment-controls.yml
 */
Closure githubOrg(Map args = [:], Set<String> approvedRepos) {
    def config = [
            displayName                    : args.name,
            jenkinsfilePath                : isSandbox() ? 'Jenkinsfile_parameterized' : 'Jenkinsfile_CNP',
            suppressDefaultJenkinsfile     : false,
            enableNamedBuildBranchStrategy : false,
    ] << args
    def folderName = config.name

    String jenkinsfilePath = config.jenkinsfilePath

    def runningOnSandbox = isSandbox()
    GString orgDescription = "<br>${config.displayName} team repositories"

    def orgDisplayName = config.displayName
    
    String credId = config.credentialsId

    String folderSuffix = ''
    String wildcardBranchesToInclude = 'master demo PR-* perftest ithc preview'
    boolean suppressDefaultJenkinsfile = config.suppressDefaultJenkinsfile
    boolean enableNamedBuildBranchStrategy = config.enableNamedBuildBranchStrategy

    if (runningOnSandbox) {
        folderSuffix = '_Sandbox'
        wildcardBranchesToInclude = '*'
        // We want the labs folder to build on push but others don't need to
        enableNamedBuildBranchStrategy = config.name == 'LABS' || config.name == 'Pipeline_Test' ? false : true
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

        credId = "hmcts-jenkins-cnp"
    }

    return {
        organizationFolder(orgFolderName) {
            description(orgDescription)
            displayName(orgDisplayName)
            organizations {
                github {
                    repoOwner("HMCTS")
                    apiUri("https://api.github.com")
                    credentialsId(credId)
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

                // Build a regex from the approved repos allowlist.
                // If an org-level regex is also configured, use a lookahead to
                // require the repo matches BOTH the allowlist AND the org regex.
                if (approvedRepos) {
                    def quotedNames = approvedRepos.collect { java.util.regex.Pattern.quote(it) }
                    String allowlistRegex = quotedNames.join('|')

                    String combinedRegex
                    if (config.regex) {
                        // Must match the org regex AND be in the allowlist
                        combinedRegex = "(?=(?:${config.regex}))(?:${allowlistRegex})"
                    } else {
                        combinedRegex = allowlistRegex
                    }

                    traits << 'jenkins.scm.impl.trait.RegexSCMSourceFilterTrait' {
                        regex(combinedRegex)
                    }
                } else if (config.regex) {
                    // Fallback: no allowlist loaded, use org regex only
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
                    strategyId(2)
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
            }
        }
    }
}