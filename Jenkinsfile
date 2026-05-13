// ============================================================
// StockPro Backend — Jenkinsfile
// Repo: stockpro (Spring Boot Microservices)
// Stages: Build → Test → Docker Build → Deploy
// ============================================================

pipeline {
    agent any

    environment {
        COMPOSE_FILE = 'docker-compose.yml'
    }

    tools {
        maven 'Maven-3.9'
    }

    stages {

        // ── 1. CHECKOUT ─────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                echo "Branch: ${env.BRANCH_NAME}"
            }
        }

        // ── 2. BUILD ALL MICROSERVICES ───────────────────────
        stage('Maven Build') {
            steps {
                sh 'mvn clean package -DskipTests --batch-mode'
            }
        }

        // ── 3. UNIT TESTS ────────────────────────────────────
        stage('Run Tests') {
            steps {
                sh 'mvn test --batch-mode -Dtest="!*ApplicationTests" -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ── 4. CODE QUALITY (SonarQube) ───────────────────────
        stage('SonarQube Analysis') {
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

        // ── 5. DOCKER BUILD (uses pre-built JARs from Maven stage) ────
        stage('Docker Build') {
            steps {
                withCredentials([file(credentialsId: 'stockpro-env', variable: 'ENV_FILE')]) {
                    sh '''
                        trap 'rm -f .env' EXIT
                        cp $ENV_FILE .env

                        SERVICES="alert-service analytics-service api-gateway authservice \
                                  eureka-service payment-service product-service purchase-service \
                                  stockmovement-services supplier-service warehouse-service"

                        for svc in $SERVICES; do
                            JAR=$(find "${svc}/target" -maxdepth 1 -name "*.jar" \
                                  ! -name "*original*" 2>/dev/null | head -1)
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
                                -t "stockpro-backend-${svc}:latest" \
                                -f "${svc}/Dockerfile.ci" \
                                "${svc}/"

                            rm -f "${svc}/app.jar" "${svc}/Dockerfile.ci"
                            echo "[DONE] stockpro-backend-${svc}:latest built successfully"
                        done
                    '''
                }
            }
        }

        // ── 6. DEPLOY (main branch only) ──────────────────────
        stage('Deploy') {
            when {
                anyOf {
                    branch 'main'
                    expression { env.GIT_BRANCH == 'main' || env.GIT_BRANCH == 'origin/main' }
                }
            }
            steps {
                withCredentials([file(credentialsId: 'stockpro-env', variable: 'ENV_FILE')]) {
                    sh '''
                        trap 'rm -f .env' EXIT
                        cp $ENV_FILE .env
                        docker compose --env-file .env up -d --no-build --remove-orphans \
                            rabbitmq redis eureka-service authservice product-service \
                            purchase-service payment-service supplier-service warehouse-service \
                            stockmovement-services analytics-service alert-service api-gateway
                    '''
                }
            }
        }

    }

    // ── POST ACTIONS ─────────────────────────────────────────
    post {
        success {
            echo ' Backend pipeline succeeded!'
        }
        failure {
            echo ' Backend pipeline failed!'
        }
        always {
            cleanWs()
        }
    }
}
