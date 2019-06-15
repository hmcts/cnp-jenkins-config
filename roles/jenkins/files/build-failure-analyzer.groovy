import com.sonyericsson.jenkins.plugins.bfa.db.MongoDBKnowledgeBase
import com.sonyericsson.jenkins.plugins.bfa.sod.ScanOnDemandVariables

import jenkins.model.Jenkins

import hudson.util.Secret

Jenkins jenkins = Jenkins.getInstance();
def plugin = jenkins.getDescriptor('com.sonyericsson.jenkins.plugins.bfa.PluginImpl');

plugin.noCausesMessage = 'No problems were identified. If you know why this problem occurred, please add a suitable Cause for it - <a href="https://tools.hmcts.net/confluence/display/CNP/Build+failure+analyzer">documentation</a>';
plugin.globalEnabled = true;
plugin.doNotAnalyzeAbortedJob = false;
plugin.gerritTriggerEnabled = false;
plugin.graphsEnabled = true;
plugin.testResultParsingEnabled = true;
plugin.testResultCategories = "test-failures";
plugin.maxLogSize = 0;
plugin.nrOfScanThreads = 3;

sodVariables = new ScanOnDemandVariables()

sodVariables.setSodCorePoolNumberOfThreads(ScanOnDemandVariables.DEFAULT_SOD_COREPOOL_THREADS);
sodVariables.setSodWaitForJobShutdownTimeout(ScanOnDemandVariables.DEFAULT_SOD_WAIT_FOR_JOBS_SHUTDOWN_TIMEOUT);
sodVariables.setSodThreadKeepAliveTime(ScanOnDemandVariables.DEFAULT_SOD_THREADS_KEEP_ALIVE_TIME);
sodVariables.setMinimumSodWorkerThreads(ScanOnDemandVariables.DEFAULT_MINIMUM_SOD_WORKER_THREADS);
sodVariables.setMaximumSodWorkerThreads(ScanOnDemandVariables.DEFAULT_MAXIMUM_SOD_WORKER_THREADS);

plugin.sodVariables = sodVariables

plugin.knowledgeBase = new MongoDBKnowledgeBase(
        "${hostname}",
        27017,
        "build-failure-analyzer", // dbName
        "build-failure-analyzer", // username
        Secret.fromString("${password}"),
        true, // enableStatistics
        false // successfulLogging
);

plugin.save();
