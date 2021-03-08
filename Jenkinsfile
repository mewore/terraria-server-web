pipeline {
    agent any
    tools {
        jdk 'openjdk-15.0.2'
    }

    stages {
        stage('Prepare') {
            steps {
                git branch: 'main',
                    credentialsId: 'mewore',
                    url: 'git@github.com:mewore/terraria-server-web.git'
                sh 'java -version'
            }
        }
        stage('Backend') {
            steps {
                script {
                    sh './gradlew backend:cleanTest backend:test --no-daemon'
                }
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
            archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
            junit 'backend/build/test-results/**/*.xml'
        }
    }
}
