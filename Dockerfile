FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:21-jdk-slim

WORKDIR /app

COPY ./exam-system-online-server-1.0-SNAPSHOT app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]

