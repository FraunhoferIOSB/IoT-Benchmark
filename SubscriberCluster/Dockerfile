FROM openjdk:8-jre
MAINTAINER Reinhard Herzog <reinhard.herzog@iosb.fraunhofer.de>

ENV NAME Listener_1
ENV BASE_URL http://localhost:8080/FROST-Server/v1.0/
ENV BROKER localhost
ENV SESSION 0815
ENV WORKERS 10
ENV COVERAGE 50
ENV POSTDELAY 1

ADD target/${JAR_FILE} ./frostbenchmark.jar
CMD ["/usr/bin/java", "-jar", "./frostbenchmark.jar"]

