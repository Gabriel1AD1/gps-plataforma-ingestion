FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copia solo el jar que NO contiene "plain"
COPY build/libs/*[^plain].jar app.jar

# Ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]