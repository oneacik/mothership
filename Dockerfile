FROM gradle:8-jdk21 AS build 
ARG project="ledger"
ADD . /build/
WORKDIR /build/app/${project}/
RUN gradle --no-daemon shadowJar


FROM amd64/eclipse-temurin:21-jre AS mothership
ARG project="ledger"
EXPOSE 8080

ENV STORAGE_PATH=/storage
RUN mkdir /storage/

COPY --from=build /build/app/${project}/build/libs/${project}-all.jar /app/app.jar

CMD java -jar /app/app.jar
