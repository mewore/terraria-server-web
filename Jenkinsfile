pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps { //Checking out the repo
                git branch: 'master',
                    credentialsId: 'mewore',
                    url: 'git@github.com:mewore/terraria-server-web.git'
            }
        }
//         stage('Jar') {
//             steps {
//                 script {
//                     sh './gradlew jar --no-daemon'
//                 }
//             }
//         }
    }
}
