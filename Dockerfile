# Imagen generica para cualquiera de los microservicios.
# Requiere haber ejecutado antes "mvn package -DskipTests" en la raiz.
# docker-compose.yml la construye una vez por servicio con el argumento SERVICE.
FROM eclipse-temurin:17-jre
ARG SERVICE
WORKDIR /app
COPY ${SERVICE}/target/${SERVICE}-2.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
