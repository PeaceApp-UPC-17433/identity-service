# ---- Build: JDK 21 + Maven fijos, no depende de lo que tenga instalado cada integrante ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Capa de dependencias: solo se vuelve a descargar si cambia el pom.xml.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime: solo JRE 21, usuario sin privilegios ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S peaceapp && adduser -S peaceapp -G peaceapp
COPY --from=build /app/target/identity-service-*.jar app.jar
USER peaceapp

EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
