# Build (unchanged)
#FROM eclipse-temurin:17-jdk-jammy AS build
#WORKDIR /src
#COPY . .
#RUN ./mvnw -q -DskipTests package
#
## Runtime: glibc-based + G1GC
#FROM eclipse-temurin:17-jre-jammy
#WORKDIR /opt/app
#COPY --from=build /src/target/app.jar /opt/app/app.jar
#
## Keep logs small and avoid native GC quirks
#ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
#
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
FROM maven:3.9.8-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy only the pom first to cache dependencies
COPY pom.xml ./
# If you have a multi-module repo, copy the module poms too:
# COPY */pom.xml ./

# Pre-fetch deps (optional but speeds up builds)
#RUN ./mvnw -q -DskipTests package

# Now add sources and build
COPY src ./src
RUN mvn -q -B -DskipTests package && \
    echo "Built artifacts:" && ls -lah target

# ---------- Runtime stage (JRE17, slim) ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /opt/app

#


# copy the built jar (assumes a single jar is produced)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=10 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-c","java -XX:+UseG1GC -XX:MaxRAMPercentage=75 -jar app.jar"]