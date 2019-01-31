String url = jenkins.model.JenkinsLocationConfiguration.get().getUrl()
println "Running on ${url}"

private boolean isSandbox() {
    jenkins.model.JenkinsLocationConfiguration.get().getUrl().contains("sandbox")
}

List<Map> orgs = [
        [name: 'cmc'],
        [name: 'divorce', displayName: 'Divorce', regex: 'div.*'],
        [name: 'cnp']
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
            name                      : 'pipeline_test',
            displayName               : 'Pipeline Test',
            regex                     : 'cnp-rhubarb-.*|cnp-jenkins-library',
            branchesToInclude         : 'master',
            jenkinsfilePath           : 'Jenkinsfile_pipeline_test',
            suppressDefaultJenkinsfile: true
    ]
    githubOrg(pipelineTestOrg).call()
}

Closure githubOrg(Map args = [:]) {
    def config = [
            displayName               : args.name.toUpperCase(),
            regex                     : args.name + '.*',
            jenkinsfilePath           : isSandbox() ? 'Jenkinsfile_parameterized' : 'Jenkinsfile_CNP',
            suppressDefaultJenkinsfile: false
    ] << args
    def name = config.name

    String jenkinsfilePath = config.jenkinsfilePath

    String folderSandboxPrefix = isSandbox() ? 'Sandbox_' : ''
    GString orgFolderName = "HMCTS_${folderSandboxPrefix}${name.toUpperCase()}"
    wildcardBranchesToInclude = 'master masterv2 hmctsdemo demo cnp PR*'
    GString orgDescription = "<br>${config.displayName} team repositories"

    String displayNamePrefix = "HMCTS"

    if (config.branchesToInclude) {
        wildcardBranchesToInclude = config.branchesToInclude
    }

    boolean suppressDefaultJenkinsfile = config.suppressDefaultJenkinsfile

    if (config.nightly) {
        orgFolderName = "HMCTS_${folderSandboxPrefix}Nightly_${name.toUpperCase()}"
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
                    credentialsId("jenkins-github-hmcts-api-token_${name}")
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
                }
                traits << 'org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait' {
                    strategyId(1)
                }
                traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
                    strategyId(1)
                }

                // prevent builds triggering automatically from SCM push for sandbox and nightly builds
                if (isSandbox() || config.nightly) {
                    node / buildStrategies / 'jenkins.branch.buildstrategies.basic.NamedBranchBuildStrategyImpl'(plugin: 'basic-branch-build-strategies@1.1.1') {
                        filters()
                    }
                }
            }
        }
    }
}