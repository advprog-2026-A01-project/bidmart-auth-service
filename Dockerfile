FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081 9091

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
