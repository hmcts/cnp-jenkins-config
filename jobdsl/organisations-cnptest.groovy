
// --dv
import com.microsoft.azure.vmagent.builders.*;
import jenkins.model.Jenkins;
import hudson.model.Node;
// --dv

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


// --dv
def myCloud = new com.microsoft.azure.vmagent.builders.AzureVMCloudBuilderr()
.withCloudName("test")
.withAzureCredentialsId("jenkinsServicePrincipal")
.withExistingResourceGroupName("mgmt-vmimg-store-cnptest")
.withMaxVirtualMachinesLimit("2")
.withDeploymentTimeout("1200")
.addNewTemplate()
    .withName("test")
    .withLabels("test")
    .withDescription("Jenkins build agents for HMCTS")
    .withWorkspace("test")
    .withLocation("UK South")
    .withUsageMode("Only build jobs with label expressions matching this node") 
    .withVirtualMachineSize("Standard_D4_v3")
    .withExistingStorageAccount("mgmtvmimgstorecnptest")
    .withStorageAccountType("Standard_LRS")
    .addNewAdvancedImage()
        .withCustomManagedImage("/subscriptions/bf308a5c-0624-4334-8ff8-8dca9fd43783/resourceGroups/cnp-vmimages-sandbox/providers/Microsoft.Compute/images/moj-centos-agent74-20181113164555")
        .withOsType("Linux")
        .withLaunchMethod("SSH")
        .withInitScript("usermod -a -G docker {{ jenkins_agent_user }}\numount /mnt/resource\nmkdir -pv {{ jenkins_home }}\nmount /dev/sdc1 {{ jenkins_home }}\nchown -R {{ jenkins_agent_user }}:{{ jenkins_agent_user }} {{ jenkins_home }}\nmv /tmp/jenkinsssh_id_rsa /home/{{ jenkins_agent_user }}/.ssh/id_rsa\nchown {{ jenkins_agent_user }}:{{ jenkins_agent_user }} /home/{{ jenkins_agent_user }}/.ssh/id_rsa\nchmod 0600 /home/{{ jenkins_agent_user }}/.ssh/id_rsa\nmv /tmp/vault-token /home/{{ jenkins_agent_user }}/.vault-token\nchown -R {{ jenkins_agent_user }}:{{ jenkins_agent_user }} /home/{{ jenkins_agent_user }}/.vault-token\nchmod 0600 /home/{{ jenkins_agent_user }}/.vault-token\nmkdir {{ jenkins_home }}/.gradle && echo 'org.gradle.daemon=false' >{{ jenkins_home }}/.gradle/gradle.properties\ncat > /etc/dnsmasq.d/10-internal-forwarding<<EOF\nserver=/#/{{ ansible_eth0.ipv4.address }}\nEOF\nsystemctl restart dnsmasq\ncat >/etc/security/limits.d/30-jenkins.conf<<EOF\n{{ jenkins_agent_user }} soft nofile {{ jenkins_nofile_soft }}\n{{ jenkins_agent_user }} hard nofile {{ jenkins_nofile_hard }}\n{{ jenkins_agent_user }} soft nproc {{ jenkins_nproc_soft }}\n{{ jenkins_agent_user }} hard nproc {{ jenkins_nproc_hard }}\nEOF\nssh-keyscan github.com github.com >> /home/{{ jenkins_agent_user }}/.ssh/known_hosts\nssh-keygen -F github.com -f /home/{{ jenkins_agent_user }}/.ssh/known_hosts # verifies key is correctly installed")
        .withRunScriptAsRoot(true)
        .withDoNotUseMachineIfInitFails(true)
        .withVirtualNetworkName("vnet1")
        .withVirtualNetworkResourceGroupName("vnet1-RG")
        .withSubnetName("jenkins-subnet")
        .withUsePrivateIP(true)
        .withNumberOfExecutors("1")
        .withJvmOptions("-Xms4G -Xmx4G -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+AlwaysPreTouch -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:+UnlockDiagnosticVMOptions -XX:G1SummarizeRSetStatsPeriod=1")
        .withEnableUAMI(true)
        .withGetUamiID("/subscriptions/bf308a5c-0624-4334-8ff8-8dca9fd43783/resourcegroups/rgcnptestsandbox/providers/Microsoft.ManagedIdentity/userAssignedIdentities/cnpmitestinga")
    .endAdvancedImage()
    .withAdminCredential("vm_agent_creds")
.endTemplate()
.build();

Jenkins.getInstance().clouds.add(myCloud);
// --dv