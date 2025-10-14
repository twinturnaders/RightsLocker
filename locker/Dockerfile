# ---------- Build stage (Maven + JDK17) ----------
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the pom first to cache dependencies
COPY pom.xml ./
# If you have a multi-module repo, copy the module poms too:
# COPY */pom.xml ./

# Pre-fetch deps (optional but speeds up builds)
RUN mvn -q -B -DskipTests dependency:go-offline

# Now add sources and build
COPY src ./src
RUN mvn -q -B -DskipTests package && \
    echo "Built artifacts:" && ls -lah target

# ---------- Runtime stage (JRE17, slim) ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /opt/app

# non-root
RUN addgroup -S app && adduser -S app -G app
USER app

# copy the built jar (assumes a single jar is produced)
COPY --from=build /src/target/*-SNAPSHOT.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=10 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-c","java -XX:+UseZGC -XX:MaxRAMPercentage=75 -jar app.jar"]