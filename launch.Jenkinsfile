pipeline {
    agent {
        node LAUNCH_NODE
    }

    tools {
        jdk 'openjdk-11.0.2'
    }

    environment {
        LOG_FILE="tsw-${env.BUILD_NUMBER}.log"
        DOWNLOADED_JAR_NAME = "${TSW_BUILD_JOBNAME}-${TSW_BUILD_NUMBER}-${JAR_NAME}"
        LAUNCH_COMMAND = 'export TSW_DB_USERNAME="${TSW_DB_USERNAME}"; export TSW_DB_PASSWORD="${TSW_DB_PASSWORD}"; ' +
            'export TSW_KEYSTORE_FILENAME="${TSW_KEYSTORE_ALIAS}.jks"; export TSW_KEYSTORE_ALIAS="${TSW_KEYSTORE_ALIAS}"; export TSW_KEYSTORE_PASSWORD="${TSW_KEYSTORE_PASSWORD}"; ' +
            "nohup bash -c \"java -jar '${DOWNLOADED_JAR_NAME}' --spring.profiles.active=common,prod\" > '${LOG_FILE}' &"
    }

    stages {
        stage('Prepare') {
            when {
                not {
                    fileExists("${DOWNLOADED_JAR_NAME}")
                }
            }
            steps {
                script {
                    sh 'pwd'
                    copyArtifacts(projectName: "${TSW_BUILD_JOBNAME}", selector: specific("${TSW_BUILD_NUMBER}"), filter: "build/libs/${JAR_NAME}")
                    sh 'cp "build/libs/${JAR_NAME}" "${DOWNLOADED_JAR_NAME}"'
                    sh 'rm -rf "build"'
                }
            }
        }
        stage('Stop') {
            steps {
                script {
                    processOutput = sh returnStdout: true, script: "ps -C java -u '${env.USER}' -o pid=,command= | grep 'spring.profiles.active=common,prod' | awk '{print \$1;}'"
                    processOutput.split('\n').each { pid ->
                        if (pid.length() > 0) {
                            echo "Killing: ${pid}"
                            killStatus = sh returnStatus: true, script: "kill ${pid}"
                            if (killStatus != 0) {
                                echo "Process ${pid} must have already been stopped."
                            }
                        }
                    }
                    sleep 5
                    curlStatus = sh returnStatus: true, script: "curl http://localhost:8080"
                    if (curlStatus == 0) {
                        error "The app is still running or something else has taken up port :8080! Kill it manually."
                    }
                    sh "mkdir -p 'old-logs' && mv tsw-*.log ./old-logs || echo 'No logs to move.'"
                }
            }
        }
        stage('Launch') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: "${TSW_DB_CREDENTIALS}",
                        usernameVariable: 'TSW_DB_USERNAME', passwordVariable: 'TSW_DB_PASSWORD'),
                    usernamePassword(credentialsId: "${TSW_KEYSTORE_CREDENTIALS}",
                        usernameVariable: 'TSW_KEYSTORE_ALIAS', passwordVariable: 'TSW_KEYSTORE_PASSWORD')
                ]) {
                    // https://devops.stackexchange.com/questions/1473/running-a-background-process-in-pipeline-job
                    withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                        script {
                            sh LAUNCH_COMMAND
                        }
                    }
                }
            }
        }
        stage('Verify') {
            steps {
                sleep 20
                script {
                    if (fileExists(LOG_FILE)) {
                        sh "tail -n 100 '${LOG_FILE}'"
                    } else {
                        error "The app does not have an output file '${LOG_FILE}'!"
                    }
                    sh "curl https://localhost:443 | grep '<tsw-root>'"
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: "${LOG_FILE}", fingerprint: true
        }
    }
}
