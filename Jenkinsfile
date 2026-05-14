pipeline {
    agent any

    parameters {
        choice(
            name: 'SERVICE_NAME',
            choices: [
                'all',
                'alert-service',
                'analytics-service',
                'api-gateway',
                'authservice',
                'eureka-service',
                'payment-service',
                'product-service',
                'purchase-service',
                'stockmovement-services',
                'supplier-service',
                'warehouse-service'
            ],
            description: 'Choose all services or one backend service to build and push.'
        )
    }

    environment {
        DOCKERHUB_NAMESPACE = 'sunnysingh12'
        DOCKERHUB_CREDENTIALS = 'dockerhub-credentials'
    }

    tools {
        maven 'Maven-3.9'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Branch: ${env.BRANCH_NAME}"
            }
        }

        stage('Maven Build') {
            steps {
                sh '''
                    set -e
                    if [ "$SERVICE_NAME" = "all" ]; then
                        mvn clean package -DskipTests --batch-mode
                    else
                        mvn clean package -DskipTests --batch-mode -pl "$SERVICE_NAME" -am
                    fi
                '''
            }
        }

        stage('Run Tests') {
            when { expression { return false } }
            steps {
                sh 'mvn test --batch-mode -Dtest="!*ApplicationTests" -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            when { expression { return false } }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=stockpro \
                          -Dsonar.token=$SONAR_AUTH_TOKEN \
                          --batch-mode
                    '''
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "${DOCKERHUB_CREDENTIALS}",
                    usernameVariable: 'DOCKERHUB_USERNAME',
                    passwordVariable: 'DOCKERHUB_TOKEN'
                )]) {
                    sh '''
                        set -e

                        if [ "$SERVICE_NAME" = "all" ]; then
                            SERVICES="alert-service analytics-service api-gateway authservice \
                                      eureka-service payment-service product-service purchase-service \
                                      stockmovement-services supplier-service warehouse-service"
                        else
                            SERVICES="$SERVICE_NAME"
                        fi

                        IMAGE_TAG="${GIT_COMMIT:-manual}"
                        IMAGE_TAG="$(printf "%s" "$IMAGE_TAG" | cut -c1-12)"

                        printf "%s" "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin

                        for svc in $SERVICES; do
                            JAR=$(find "${svc}/target" -maxdepth 1 -name "*.jar" ! -name "*original*" 2>/dev/null | head -1)
                            if [ -z "$JAR" ]; then
                                echo "[SKIP] ${svc}: JAR not found in target/"
                                continue
                            fi

                            cp "$JAR" "${svc}/app.jar"
                            cat > "${svc}/Dockerfile.ci" <<'CIEOF'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
CIEOF

                            DOCKER_BUILDKIT=0 docker build \
                                -t "${DOCKERHUB_NAMESPACE}/stockpro-backend-${svc}:latest" \
                                -t "${DOCKERHUB_NAMESPACE}/stockpro-backend-${svc}:${IMAGE_TAG}" \
                                -f "${svc}/Dockerfile.ci" \
                                "${svc}/"

                            docker push "${DOCKERHUB_NAMESPACE}/stockpro-backend-${svc}:latest"
                            docker push "${DOCKERHUB_NAMESPACE}/stockpro-backend-${svc}:${IMAGE_TAG}"

                            rm -f "${svc}/app.jar" "${svc}/Dockerfile.ci"
                            echo "[DONE] ${DOCKERHUB_NAMESPACE}/stockpro-backend-${svc}:${IMAGE_TAG} pushed successfully"
                        done

                        docker logout
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Backend images built and pushed successfully!'
        }
        failure {
            echo 'Backend pipeline failed!'
        }
        always {
            cleanWs()
        }
    }
}
