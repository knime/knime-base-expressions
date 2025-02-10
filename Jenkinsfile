#!groovy

def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2025-07'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([]),
	parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    node('maven && java17') {
        knimetools.defaultTychoBuild(updateSiteProject: 'org.knime.update.base.expressions')

        workflowTests.runTests(
            dependencies: [
                repositories: [
                    'knime-base-expressions',
                    'knime-scripting-editor',
                    'knime-core-columnar',
                ]
            ]
        )

        stage('Sonarqube analysis') {
            env.lastStage = env.STAGE_NAME
            // TODO(workflow-tests) remove empty list once workflow tests are enabled
            workflowTests.runSonar([])
        }

        owasp.sendNodeJSSBOMs(readMavenPom(file: 'pom.xml').properties['revision'])
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
