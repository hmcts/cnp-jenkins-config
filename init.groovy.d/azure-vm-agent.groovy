#!groovy
import com.microsoft.azure.vmagent.builders.*
import jenkins.model.Jenkins

def String getNonEmptyVariable(String envVariableName) {
    def variable = System.getenv(envVariableName)
    if (variable == null || variable.isEmpty()) {
        println envVariableName + " was not found, exiting"
        System.exit(1)
    }
    variable
}

def environment = getNonEmptyVariable("ENVIRONMENT")
def dnsServer = getNonEmptyVariable("DNS_SERVER") // should bin this and just let it get it from the vnet

def jenkinsAgentImageUri = getNonEmptyVariable("JENKINS_AGENT_IMAGE_URI")

def jenkinsAgentVnet = getNonEmptyVariable("JENKINS_AGENT_VNET")
def vnetResourceGroup = getNonEmptyVariable("JENKINS_AGENT_VNET_RG")
def jenkinsAgentSubnet = getNonEmptyVariable("JENKINS_AGENT_SUBNET")
def jenkinsAgentExecutors = getNonEmptyVariable("JENKINS_AGENT_EXECUTORS")

def jenkinsAgentMaxRunning = getNonEmptyVariable("JENKINS_AGENT_MAX")

def cloudName = "cnp-azure"

def myCloud = new AzureVMCloudBuilder()
.withCloudName(cloudName)
.withAzureCredentialsId("jenkinsServicePrincipal")
.withExistingResourceGroupName("mgmt-vmimg-store-" + environment)
.withMaxVirtualMachinesLimit(jenkinsAgentMaxRunning)
.withDeploymentTimeout("1200")
.addNewTemplate()
    .withName("cnp-jenkins-builders")
    .withDescription("Jenkins build agents for HMCTS")
    .withWorkspace("/opt/jenkins")
    .withLocation("UK South")
    .withVirtualMachineSize("Standard_D4_v3")
    .withExistingStorageAccount("mgmtvmimgstore" + environment)
    .withStorageAccountType("Standard_LRS")
    .addNewAdvancedImage()
        .withCustomManagedImage(jenkinsAgentImageUri)
        .withOsType("Linux")
        .withLaunchMethod("SSH")
        .withInitScript("""usermod -a -G docker jenkinsssh
umount /mnt/resource
mkdir -pv /opt/jenkins
mount /dev/sdc1 /opt/jenkins
chown -R jenkinsssh:jenkinsssh /opt/jenkins
mv /tmp/jenkinsssh_id_rsa /home/jenkinsssh/.ssh/id_rsa
chown jenkinsssh:jenkinsssh /home/jenkinsssh/.ssh/id_rsa
chmod 0600 /home/jenkinsssh/.ssh/id_rsa
mkdir /opt/jenkins/.gradle && echo 'org.gradle.daemon=false' > /opt/jenkins/.gradle/gradle.properties
cat > /etc/dnsmasq.d/10-internal-forwarding<<EOF
server=/#/${dnsServer}
EOF
systemctl restart dnsmasq
cat > /etc/security/limits.d/30-jenkins.conf<<EOF
jenkinsssh soft nofile 40960
jenkinsssh hard nofile 40960
jenkinsssh soft nproc 32768
jenkinsssh hard nproc 32768
EOF
ssh-keyscan github.com github.com >> /home/jenkinsssh/.ssh/known_hosts
ssh-keygen -F github.com -f /home/jenkinsssh/.ssh/known_hosts # verifies key is correctly installed
""")
        .withRunScriptAsRoot(true)
        .withDoNotUseMachineIfInitFails(true)
        .withVirtualNetworkName(jenkinsAgentVnet)
        .withVirtualNetworkResourceGroupName(vnetResourceGroup)
        .withSubnetName(jenkinsAgentSubnet)
        .withUsePrivateIP(true)
        .withNumberOfExecutors(jenkinsAgentExecutors)
        .withJvmOptions("-Xms4G -Xmx4G -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+AlwaysPreTouch -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:+UnlockDiagnosticVMOptions -XX:G1SummarizeRSetStatsPeriod=1")
    .endAdvancedImage()
    .withAdminCredential("vm_agent_creds")
.endTemplate()
.build();

def clouds = Jenkins.get().clouds
def oldCloud = clouds.getByName(cloudName)

if (oldCloud != null) {
    clouds.replace(oldCloud, myCloud);
} else {
    clouds.add(myCloud)
}
