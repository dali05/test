FROM ap27627-docker-pf.artifactory-dogen.group.echonet/bnpp-pf/openjdk-openj9-jre:17-21

WORKDIR /applis

COPY target/*SNAPSHOT.jar application.jar

EXPOSE 8080

USER 1001

CMD ["java","-jar","/applis/application.jar"]