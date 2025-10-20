#
#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /app
#COPY pom.xml .
#RUN mvn -q -e -DskipTests dependency:go-offline
#
#COPY src ./src
#RUN mvn -q -DskipTests package
#
#FROM eclipse-temurin:17-jdk-jammy
#WORKDIR /app
#
#COPY --from=build /app/target/*.jar app.jar
#
#EXPOSE 808
#ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests package

# Runtime: glibc-based + G1GC
FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/app
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

# Keep logs small and avoid native GC quirks
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]