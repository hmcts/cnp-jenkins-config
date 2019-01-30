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
}
Closure githubOrg(Map args = [:]) {
    def config = [
            displayName     : args.name.toUpperCase(),
            regex           : args.name + '.*',
    ] << args
    def name = config.name

    String prodJenkinsfile = 'Jenkinsfile_CNP'
    String sandboxJenkinsfile = 'Jenkinsfile_parameterized'

    return {
        organizationFolder("HMCTS_${isSandbox() ? 'Sandbox_' : ''}${name.toUpperCase()}") {
            description("${config.displayName} team repositories")
            displayName("HMCTS - ${config.displayName}")
            organizations {
                github {
                    repoOwner("hmcts")
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
                    scriptPath(isSandbox() ? sandboxJenkinsfile : prodJenkinsfile)
                }
                workflowMultiBranchProjectFactory {
                    scriptPath("Jenkinsfile")
                }
            }
            configure { node ->
                def traits = node / navigators / 'org.jenkinsci.plugins.github__branch__source.GitHubSCMNavigator' / traits
                traits << 'jenkins.scm.impl.trait.RegexSCMSourceFilterTrait' {
                    regex(config.regex)
                }
                traits << 'jenkins.scm.impl.trait.WildcardSCMHeadFilterTrait' {
                    includes('master masterv2 hmctsdemo demo cnp PR*')
                }
                traits << 'org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait' {
                    strategyId(1)
                }
                traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
                    strategyId(1)
                }
            }
        }
    }
}