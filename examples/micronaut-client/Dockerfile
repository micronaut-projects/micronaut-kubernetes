FROM openjdk:8-jdk-alpine 
RUN apk --no-cache add curl
COPY build/libs/*.jar micronaut-client.jar
CMD java ${JAVA_OPTS} -jar micronaut-client.jar