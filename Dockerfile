# Imagen generica para todos los microservicios.
# Antes de construir: mvn -DskipTests package  (genera <modulo>/target/<modulo>-2.0.0.jar)
FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY ${MODULE}/target/${MODULE}-2.0.0.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
