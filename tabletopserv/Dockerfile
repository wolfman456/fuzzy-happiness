# ---- build stage: compile the multi-module reactor, package the runnable service ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY mvnw .mvn pom.xml ./
COPY tabletopapi/pom.xml tabletopapi/pom.xml
COPY tabletopservice/pom.xml tabletopservice/pom.xml
RUN ./mvnw -B -pl tabletopservice -am dependency:go-offline || true

COPY . .
RUN ./mvnw -B -pl tabletopservice -am package -DskipTests

# ---- runtime stage: executable JAR with embedded Tomcat ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/tabletopservice/target/tabletopservice-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]