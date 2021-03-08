pipeline {
    agent any
    tools {
        jdk 'openjdk-15.0.2'
    }

    stages {
        stage('Display Info') {
            steps {
                sh 'java -version'
            }
        }
        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'mewore',
                    url: 'git@github.com:mewore/terraria-server-web.git'
            }
        }
        stage('Backend build') {
            steps {
                script {
                    sh './gradlew backend:compileJava --no-daemon'
                }
            }
        }
        stage('Backend tests') {
            steps {
                script {
                    sh './gradlew backend:test --no-daemon'
                }
            }
        }
        stage('Frontend lint') {
            steps {
                script {
                    sh './gradlew frontend:frontendLint --no-daemon'
                }
            }
        }
        stage('Frontend build') {
            steps {
                script {
                    sh './gradlew frontend:frontendBuildProd --no-daemon'
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
            junit 'backend/build/reports/**/*.xml'
        }
    }
}
