# TaskFlow
Java 21 / Spring Boot / Maven demo task manager. No Terraform required.

## Ubuntu setup
    sudo apt update
    sudo apt install openjdk-21-jdk maven git -y
    mvn clean package
    java -jar target/taskflow-1.0.0.jar

Open http://localhost:8080. Health: http://localhost:8080/actuator/health.

## Docker (after installing Docker)
    mvn clean package
    docker build -t taskflow:1.0 .
    docker run --rm -p 8080:8080 taskflow:1.0

Stop the standalone Java process before running Docker on the same port.

## GitHub
Create an empty repository, then run:
    git init
    git add .
    git commit -m "Initial TaskFlow application"
    git branch -M main
    git remote add origin https://github.com/YOUR_USERNAME/taskflow.git
    git push -u origin main

## Scope
Tasks are stored in Java memory, not a database. Restarting loses tasks; multiple replicas have separate data. Use one replica for this demo. No authentication; do not expose publicly with sensitive data. Jenkins, SonarQube, Ansible and Kubernetes configurations will be added during later stages. Build/runtime not verified in the generation environment.
