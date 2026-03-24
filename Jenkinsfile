pipeline {
    agent any

    environment {
        PROJECT_NAME       = 'ingestion-service'
        BUILD_DIR          = 'build/libs'
        JAR_PATTERN        = "gps-plataforma-ingestion.jar"
        DOCKER_IMAGE       = "ms-ingestion"
        NETWORK            = "home-net"

        SERVER_USER        = "root"
        SERVER_HOST        = "173.249.36.100"
        SERVER_PORT        = "22"
        PUERTO_HTTP        = "4365"
        SSH_CREDENTIAL_ID  = 'yaw-iot'
        ENV_FILE           = '/home/gps-plataforma/ms-core/.env'

        CONTAINER_MEMORY   = "2g"
        CONTAINER_CPUS     = "2"
        JAVA_OPTS          = "-Xms512m -Xmx1536m"
        INSTANCES          = "3"
        BASE_PORT          = "4365"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def jarFile = sh(
                        script: "find ${BUILD_DIR} -name '${JAR_PATTERN}' | head -n 1",
                        returnStdout: true
                    ).trim()

                    if (!jarFile) {
                        error "No se encontró JAR: ${JAR_PATTERN}"
                    }

                    echo "Construyendo imagen Docker con JAR ${jarFile}..."

                    sh """
                        cp ${jarFile} ./app.jar
                        docker build -t ${DOCKER_IMAGE} .
                    """
                }
            }
        }

        stage('Deploy to Server') {
            steps {
                script {
                    withCredentials([
                        sshUserPrivateKey(
                            credentialsId: "${SSH_CREDENTIAL_ID}",
                            keyFileVariable: 'SSH_KEY'
                        )
                    ]) {

                        sh "ssh-keyscan -p ${SERVER_PORT} ${SERVER_HOST} >> ~/.ssh/known_hosts"
                        sh "docker save ${DOCKER_IMAGE} -o ${PROJECT_NAME}.tar"
                        sh "scp -i $SSH_KEY -P ${SERVER_PORT} ${PROJECT_NAME}.tar ${SERVER_USER}@${SERVER_HOST}:/tmp/"

                        sh """
                        ssh -i $SSH_KEY -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} '
                            docker load -i /tmp/${PROJECT_NAME}.tar

                            echo "🚀 Deployando instancia principal en puerto ${BASE_PORT}"

                            docker stop ${PROJECT_NAME} || true
                            docker rm ${PROJECT_NAME} || true

                            docker run -d \\
                              --restart always \\
                              --network ${NETWORK} \\
                              --name ${PROJECT_NAME} \\
                              --env-file ${ENV_FILE} \\
                              -e JAVA_OPTS="${JAVA_OPTS}" \\
                              -p ${BASE_PORT}:${PUERTO_HTTP} \\
                              --memory=${CONTAINER_MEMORY} \\
                              --cpus=${CONTAINER_CPUS} \\
                              ${DOCKER_IMAGE}

                            for i in \$(seq 3 ${INSTANCES})
                            do
                                PORT=\$(( ${BASE_PORT} + i - 1 ))

                                echo "🚀 Deployando instancia \$i en puerto \$PORT"

                                docker stop ${PROJECT_NAME}-\$i || true
                                docker rm ${PROJECT_NAME}-\$i || true

                                docker run -d \\
                                  --restart always \\
                                  --network ${NETWORK} \\
                                  --name ${PROJECT_NAME}-\$i \\
                                  --env-file ${ENV_FILE} \\
                                  -e JAVA_OPTS="${JAVA_OPTS}" \\
                                  -p \$PORT:${PUERTO_HTTP} \\
                                  --memory=${CONTAINER_MEMORY} \\
                                  --cpus=${CONTAINER_CPUS} \\
                                  ${DOCKER_IMAGE}
                            done

                            rm -f /tmp/${PROJECT_NAME}.tar
                        '
                        """
                    }
                }
            }
        }

    }

    post {
        success {
            echo "✅ Deploy completado"
        }
        failure {
            echo "❌ Error en el pipeline"
        }
    }
}