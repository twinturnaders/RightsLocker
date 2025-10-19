# Build (unchanged)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY . .
RUN ./mvnw -q -DskipTests package

# Runtime: glibc-based + G1GC
FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/app
COPY --from=build /src/target/app.jar /opt/app/app.jar

# Keep logs small and avoid native GC quirks
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
