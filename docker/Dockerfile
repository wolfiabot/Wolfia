FROM openjdk:10-jdk-slim

ENV ENV docker

RUN mkdir /opt/wolfia

COPY build/libs/wolfia.jar /opt/wolfia/wolfia.jar

EXPOSE 4567

WORKDIR /opt/wolfia
ENTRYPOINT ["java", "-jar", "-Xmx512m", "wolfia.jar"]
