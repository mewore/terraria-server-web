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
        stage('Jar') {
            steps {
                script {
                    sh './gradlew jar --no-daemon'
                }
            }
        }
    }
}
