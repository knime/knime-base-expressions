#!groovy

def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2025-07'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([]),
	parameters(
        workflowTests.getConfigurationsAsParameters() +
        [
            booleanParam(
                defaultValue: BRANCH_NAME == 'master',
                description: "Run the benchmarks",
                name: "RUN_BENCHMARKS",
            ),
        ]),
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

    // TODO run on a specific benchmark node
    if (params["RUN_BENCHMARKS"]) {
        node('maven && java17 && ubuntu22.04 && workflow-tests') {
            stage('Run benchmarks') {
                env.lastStage = env.STAGE_NAME

                // Checkout source code
                checkout scm

                // Run benchmarks
                withMaven(mavenOpts: '-Xmx10G') {
                    withCredentials([
                        usernamePassword(credentialsId: 'ARTIFACTORY_CREDENTIALS',
                        passwordVariable: 'ARTIFACTORY_PASSWORD',
                        usernameVariable: 'ARTIFACTORY_LOGIN'),
                    ]) {
                        sh '''
                            mvn -e -Dmaven.test.failure.ignore=true -Dtycho.localArtifacts=ignore -Dknime.p2.repo=${P2_REPO} clean verify -Pbenchmark
                        '''
                    }
                }

                // Archive results
                resultFile = "target/benchmark-results.json"
                sh """
                    jq -s add \$(find . -path "*/target/surefire-reports/benchmark-results.json") > ${WORKSPACE}/${resultFile}
                """
                archiveArtifacts artifacts: resultFile
                jmhReport resultFile
            }
        }
    }

} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
