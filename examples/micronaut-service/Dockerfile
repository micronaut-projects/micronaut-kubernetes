FROM openjdk:8-jdk-alpine 
RUN apk --no-cache add curl
COPY build/libs/*.jar micronaut-service.jar
CMD java ${JAVA_OPTS} -jar micronaut-service.jar