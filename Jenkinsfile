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
                    sh './gradlew backend:cleanTest backend:test --no-daemon'
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
                    sh './gradlew frontend:frontendLint frontend:frontendBuildProd --no-daemon'
                }
            }
        }
        stage('Jar') {
            steps {
                script {
                    sh './gradlew jar --no-daemon'
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
            junit 'backend/build/test-results/**/*.xml'
        }
    }
}
