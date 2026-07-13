FROM maven:3-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home --home-dir /app werkpilot
COPY --from=build /workspace/target/werkpilot-backend-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir -p /var/lib/werkpilot/report-files \
    && chown -R werkpilot:werkpilot /app /var/lib/werkpilot

USER werkpilot
EXPOSE 8080

ENV WERKPILOT_REPORT_FILES_DIR=/var/lib/werkpilot/report-files

HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
