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

        // ── 5. DOCKER BUILD ───────────────────────────────────
        stage('Docker Build') {
            steps {
                withCredentials([file(credentialsId: 'stockpro-env', variable: 'ENV_FILE')]) {
                    sh '''
                        trap 'rm -f .env' EXIT
                        cp $ENV_FILE .env
                        DOCKER_BUILDKIT=0 docker compose --env-file .env build
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
                        docker compose --env-file .env up -d --remove-orphans
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
