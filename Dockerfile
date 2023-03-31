FROM adoptopenjdk/openjdk11:alpine-jre
ENV USER_UID=1001 \
    USER_NAME=digital-document-service
RUN addgroup --gid ${USER_UID} ${USER_NAME} \
    && adduser --disabled-password --uid ${USER_UID} --ingroup ${USER_NAME} ${USER_NAME}
ADD digital-document-service/target/*.jar app.jar
USER digital-document-service
ENTRYPOINT ["/bin/sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
