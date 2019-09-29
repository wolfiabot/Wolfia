FROM adoptopenjdk:11-openj9
MAINTAINER napster@npstr.space

ENV ENV docker

WORKDIR /opt/wolfia

EXPOSE 4567

ENTRYPOINT ["java", "-Xmx512m", "-jar", "wolfia.jar"]

COPY build/libs/wolfia.jar /opt/wolfia/wolfia.jar
