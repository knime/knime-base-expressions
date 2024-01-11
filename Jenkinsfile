#!groovy

def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2024-06'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-core-table/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
    ]),
	parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    knimetools.defaultTychoBuild('org.knime.update.base.expressions')

    // TODO(workflow-tests)
    // workflowTests.runTests(
    //     dependencies: [
    //         repositories: [
    //             'knime-core-columnar',
    //         ]
    //     ]
    // )

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
		// TODO(workflow-tests) remove empty list once workflow tests are enabled
        workflowTests.runSonar([])
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
