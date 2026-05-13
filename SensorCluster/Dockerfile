FROM eclipse-temurin:21-alpine
LABEL maintainer="Reinhard Herzog <reinhard.herzog@iosb.fraunhofer.de>"

ENV BASE_URL http://localhost:8080/FROST-Server/v1.0/
ENV BROKER localhost
ENV SESSION 0815
ENV WORKERS 10
ENV POSTDELAY 1

ARG JAR_FILE
ADD target/${JAR_FILE} ./frostbenchmark.jar   

CMD ["java", "-jar", "./frostbenchmark.jar"]

