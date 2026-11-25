pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        DOCKER_IMAGE = 'nishal3098/taskflow'
        IMAGE_TAG = "${BUILD_NUMBER}-${BUILD_ID}"
        AWS_REGION = 'ap-south-1'
        EKS_CLUSTER = 'taskflow-eks-cluster'
        KUBECONFIG = '/var/lib/jenkins/.kube/config'
    }

    stages {

        stage('Git Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Maven Build and Test') {
            steps {
                sh 'mvn clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                        -Dsonar.projectKey=taskflow \
                        -Dsonar.projectName=TaskFlow
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
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
                '''
            }
        }

        stage('Docker Hub Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        set +x
                        echo "$DOCKER_PASS" | docker login \
                            -u "$DOCKER_USER" --password-stdin

                        trap 'docker logout' EXIT

                        docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
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

                    sed -i "s|image: nishal3098/taskflow:1.0|image: ${DOCKER_IMAGE}:${IMAGE_TAG}|" \
                        k8s/deployment.yaml

                    ansible-playbook -i localhost, ansible/deploy.yaml
                '''
            }
        }

        stage('Kubernetes Verification') {
            steps {
                sh '''
                    kubectl rollout status deployment/taskflow \
                        -n default --timeout=300s

                    kubectl get deployments -n default
                    kubectl get pods -n default -o wide
                    kubectl get svc -n default

                    kubectl get deployment taskflow -n default \
                        -o jsonpath='{.spec.template.spec.containers[0].image}'

                    echo
                '''
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: TaskFlow deployed to Amazon EKS.'
        }

        failure {
            echo 'FAILED: Check Jenkins console output.'
        }

        always {
            echo 'CI/CD pipeline execution completed.'
        }
    }
}
