FROM adoptopenjdk/openjdk11:alpine-jre
ADD digital-document-service/target/*.jar app.jar
ENTRYPOINT ["/bin/sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
