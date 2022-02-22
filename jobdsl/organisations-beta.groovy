private boolean isSandbox() {
    def locationConfig = jenkins.model.JenkinsLocationConfiguration.get()
    if (locationConfig != null && locationConfig.getUrl() != null) {
      locationConfig.getUrl().contains("sandbox")
    } else {
        System.getenv("ENVIRONMENT") == "sandbox"
    }
}

List<Map> orgs = [
        [name: 'Adoption'],
        [name: 'AM'],
        [name: 'BSP', regex: '(send-letter-client|send-letter-service|send-letter-performance-tests|send-letter-service-container-.*|bulk-scan-.*|blob-router-service|reform-scan-.*)'],
        [name: 'CDM', regex: '(ccd.*|aac.*|cpo.*|hmc.*)'],
        [name: 'CMC'],
        [name: 'CNP'],
        [name: 'CTSC'],
        [name: 'DIV', displayName: "Divorce"],
        [name: 'ECM', displayName: "ECM", regex: '(ethos.*|ecm.*)', credentialId: 'hmcts-jenkins-ethos'],
        [name: 'EM',displayName: 'Evidence Management', regex: '(document-management-store-app|dm-shared-infrastructure|em-.*|dg-.*)'],
        [name: 'ET', displayName: "Employment Tribunals", regex: 'et-.*'],
        [name: 'FeePay', displayName: 'Fees and Pay', regex: '(ccfr.*|ccpay.*|bar.*)'],
        [name: 'FinRem', displayName: "Financial Remedy"],
        [name: 'FPL'],
        [name: 'FPRL', displayName: 'Family Private Law'],
        [name: 'IAC', regex: 'ia.*'],
        [name: 'IDAM', regex: '(idam-.*|cnp-idam-.*)'],
        [name: 'MI', displayName: 'Management Information'],
        [name: 'HMI', regex: 'hmi-(?!case-hq-emulator).*'],
        [name: 'PCQ'],
        [name: 'Platform',credentialId: "hmcts-jenkins-rpe", regex: '(rpe-.*|draft-store.*|cmc-pdf-service|service-auth-provider-.*|spring-boot-template|data-extractor|data-generator|camunda-.*)'],
        [name: 'Probate'],
        [name: 'RD', displayName: 'Ref Data'],
        [name: 'SSCS'],
        [name: 'XUI', regex: 'rpx-.*'],
        [name: 'RSE', displayName: 'Reform Software Engineering'],
        [name: 'Civil', regex: 'civil-(?!damages).*'],
        [name: 'WA'],
        [name: 'FACT'],
        [name: 'NFDIV', displayName: "No Fault Divorce"],
        [name: 'LAU', displayName: "Logs and Audit"],
        [name: 'PRL', displayName: 'Private Law'],
        [name: 'LABS', displayName: 'Labs'],
        [name: 'DS', displayName: 'Document Submissions'],
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
            enableNamedBuildBranchStrategy : false,
            credentialId                   : "hmcts-jenkins-cft"
    ] << args
    def folderName = config.name

    String jenkinsfilePath = config.jenkinsfilePath

    def runningOnSandbox = isSandbox()
    GString orgDescription = "<br>${config.displayName} team repositories"

    String displayNamePrefix = "HMCTS"

    String folderPrefix = ''
    String wildcardBranchesToInclude = 'master demo PR-* perftest ithc preview ethosldata'
    boolean suppressDefaultJenkinsfile = config.suppressDefaultJenkinsfile
    boolean enableNamedBuildBranchStrategy = config.enableNamedBuildBranchStrategy

    if (runningOnSandbox) {
        folderPrefix = 'Sandbox_'
        wildcardBranchesToInclude = '*'
        // We want the labs folder to build on push but others don't need to
        enableNamedBuildBranchStrategy = config.name == 'LABS' ? false : true
    }
    GString orgFolderName = "HMCTS_${folderPrefix}${folderName}"

    if (config.branchesToInclude) {
        wildcardBranchesToInclude = config.branchesToInclude
    }

    if (config.nightly) {
        orgFolderName = "HMCTS_${folderPrefix}Nightly_${folderName}"
        //noinspection GroovyAssignabilityCheck
        orgDescription = "<br>Nightly tests for ${config.displayName}  will be scheduled using this organisation on the AAT Version of the application"

        displayNamePrefix += " Nightly Tests"
        wildcardBranchesToInclude = "master nightly-dev"

        jenkinsfilePath = runningOnSandbox ? 'Jenkinsfile_nightly_sandbox' : 'Jenkinsfile_nightly'
        suppressDefaultJenkinsfile = true
        enableNamedBuildBranchStrategy = true
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

                if (!config.nightly && !config.disableAgedRefsBranchStrategy) {
                    traits << 'org.jenkinsci.plugins.scm_filter.GitHubAgedRefsTrait' {
                        retentionDays(30)
                    }
                }

                // prevent builds triggering automatically from SCM push for sandbox and nightly builds
                if (enableNamedBuildBranchStrategy) {
                    node / buildStrategies / 'jenkins.branch.buildstrategies.basic.NamedBranchBuildStrategyImpl'(plugin: 'basic-branch-build-strategies@1.1.1') {
                        filters()
                    }
                }
            }
        }
    }
}
