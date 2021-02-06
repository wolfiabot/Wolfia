FROM adoptopenjdk:11-openj9
MAINTAINER napster@npstr.space

RUN groupadd -g 420 wolfia && \
	useradd -r -u 420 -g wolfia wolfia

USER wolfia:wolfia

ENV ENV docker

WORKDIR /opt/wolfia

EXPOSE 4567

ENTRYPOINT ["java", "-Xmx512m", "-jar", "wolfia.jar"]

COPY build/libs/wolfia.jar /opt/wolfia/wolfia.jar
