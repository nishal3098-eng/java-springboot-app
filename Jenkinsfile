
pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'nishal3098/taskflow'
        IMAGE_TAG = "${BUILD_NUMBER}-${BUILD_ID}"
        AWS_REGION = 'ap-south-1'
        EKS_CLUSTER = 'taskflow-eks-cluster'
        KUBECONFIG = '/var/lib/jenkins/.kube/config'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    triggers {
        githubPush()
    }

    stages {

        stage('Git Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/nishal3098-eng/java-springboot-app.git'
            }
        }

        stage('Maven Build and Test') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
                        -Dsonar.projectKey=taskflow \
                        -Dsonar.projectName=TaskFlow
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate(
                        abortPipeline: true,
                        webhookSecretId: 'sonarqube-webhook-secret'
                    )
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} .
                    docker images ${DOCKER_IMAGE}
                '''
            }
        }

        stage('Docker Hub Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh '''
                        set +x
                        echo "$DOCKER_PASSWORD" | docker login \
                        -u "$DOCKER_USERNAME" --password-stdin

                        docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
                        docker logout
                    '''
                }
            }
        }

        stage('Ansible Deployment to EKS') {
            steps {
                sh '''
                    aws eks update-kubeconfig \
                    --region ${AWS_REGION} \
                    --name ${EKS_CLUSTER}

                    ansible-playbook -i localhost, ansible/deploy.yaml

                    kubectl set image deployment/taskflow \
                    taskflow=${DOCKER_IMAGE}:${IMAGE_TAG} \
                    -n default

                    kubectl rollout status deployment/taskflow \
                    -n default --timeout=300s
                '''
            }
        }

        stage('Kubernetes Verification') {
            steps {
                sh '''
                    kubectl get deployments -n default
                    kubectl get pods -n default -o wide
                    kubectl get services -n default

                    kubectl get endpointslices -n default \
                    -l kubernetes.io/service-name=taskflow-service

                    kubectl get deployment taskflow -n default \
                    -o jsonpath='{.spec.template.spec.containers[*].image}'

                    echo ""
                    echo "TaskFlow deployment verified successfully."
                '''
            }
        }
    }

    post {
        success {
            echo 'CI/CD Pipeline completed successfully!'
            echo "Deployed image: ${DOCKER_IMAGE}:${IMAGE_TAG}"
        }

        failure {
            echo 'CI/CD Pipeline failed. Check the console output.'
        }

        always {
            sh 'docker logout || true'
            echo 'Pipeline execution completed.'
        }
    }
}
