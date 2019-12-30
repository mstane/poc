FROM openjdk:11.0.4-jre

RUN mkdir -p /app
WORKDIR /app

COPY target/loggingpoc-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "server", "/config.yml"]
