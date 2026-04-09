FROM eclipse-temurin:17-jdk

WORKDIR /app

# El Jenkinsfile ya copió el JAR como app.jar en la raíz
COPY app.jar app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]