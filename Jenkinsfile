pipeline {
    agent any
    tools {
        jdk 'openjdk-11.0.2'
    }

    stages {
        stage('Prepare') {
            steps {
                git([
                    branch: env.BRANCH == null ? 'main' : env.BRANCH,
                    credentialsId: 'mewore',
                    url: 'git@github.com:mewore/terraria-server-web.git',
                ])
                sh 'java -version'
            }
        }
        stage('Backend') {
            steps {
                script {
                    sh './gradlew backend:test --no-daemon -PuseCheckerFramework'
                }
                jacoco([
                    classPattern: '**/backend/build/classes',
                    execPattern: '**/**.exec',
                    sourcePattern: '**/backend/src/main/java',
                    exclusionPattern: [
                        '**/test/**/*.class',
                        '**/Application.class',
                        '**/*Constants.class',
                        '**/*Entity.class',
                        '**/services/util/**/*.class',
                        '**/config/security/AuthorityRoles.class',
                    ].join(','),

                    // 100% health at:
                    maximumBranchCoverage: '90',
                    maximumClassCoverage: '95',
                    maximumComplexityCoverage: '90',
                    maximumLineCoverage: '95',
                    maximumMethodCoverage: '95',
                    // 0% health at:
                    minimumBranchCoverage: '70',
                    minimumClassCoverage: '80',
                    minimumComplexityCoverage: '70',
                    minimumLineCoverage: '80',
                    minimumMethodCoverage: '80',
                ])
            }
        }
        stage('Frontend') {
            steps {
                script {
                    sh './gradlew frontend:frontendLint frontend:frontendBuildProd frontend:frontendTest' +
                        ' --no-daemon -PuseCheckerFramework'
                }
                cobertura([
                    coberturaReportFile: '**/frontend/coverage/terraria-server-web/cobertura-coverage.xml',
                    conditionalCoverageTargets: '90, 70, 0',
                    lineCoverageTargets: '95, 80, 0',
                    methodCoverageTargets: '95, 80, 0',
                ])
            }
        }
        stage('Jar') {
            steps {
                script {
                    sh './gradlew jar --no-daemon -PuseCheckerFramework'
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts([
                artifacts: 'build/libs/**/*.jar',
                fingerprint: true,
            ])
        }
    }
}
