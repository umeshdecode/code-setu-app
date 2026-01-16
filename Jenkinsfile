pipeline {
    agent any

    environment {
        IMAGE_NAME = "code-setu-app"                   // Local image name
        CONTAINER_NAME = "code-setu-app"               // Container name on EC2
        DOCKERHUB_USER = "decode200"            // Your Docker Hub username
        DOCKERHUB_REPO = "${DOCKERHUB_USER}/code-setu-app" // Docker Hub repo
        DOCKER_HUB_PASS = credentials('DOCKER_HUB_PASS')   // Jenkins secret
    }

    stages {

        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build Spring Boot JAR') {
            steps {
                sh './gradlew clean build -x test'
            }
        }

        stage('Stop Old Container') {
            steps {
                sh '''
                docker stop $CONTAINER_NAME || true
                docker rm $CONTAINER_NAME || true
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                # Build local Docker image
                docker build -t $IMAGE_NAME:latest .

                # Tag for Docker Hub
                docker tag $IMAGE_NAME:latest $DOCKERHUB_REPO:latest

                # Clean up unused images to save EC2 space
                docker image prune -f
                '''
            }
        }

        stage('Push Docker Image to Docker Hub') {
            steps {
                sh '''
                echo $DOCKER_HUB_PASS | docker login -u $DOCKERHUB_USER --password-stdin
                docker push $DOCKERHUB_REPO:latest
                '''
            }
        }

        stage('Run New Container') {
            steps {
                sh '''
                # Run the container on EC2
                docker run -d \
                --name $CONTAINER_NAME \
                -p 31201:31201 \
                $IMAGE_NAME:latest
                '''
            }
        }
    }

    post {
        always {
            sh 'docker ps -a'
            echo 'Deployment finished.'
        }
    }
}
