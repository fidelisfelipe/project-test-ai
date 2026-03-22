FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY flight-monitor-app/pom.xml flight-monitor-app/
COPY flight-monitor-admin/pom.xml flight-monitor-admin/
RUN mvn dependency:go-offline -B -pl flight-monitor-app -am \
    --no-transfer-progress || true
COPY flight-monitor-app/src flight-monitor-app/src
COPY flight-monitor-admin/src flight-monitor-admin/src
RUN mvn package -pl flight-monitor-app -am \
    -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S flightmonitor && adduser -S flightmonitor -G flightmonitor

# ✅ Criar o diretório e dar ownership ANTES de trocar de usuário
RUN mkdir -p /app/data && chown -R flightmonitor:flightmonitor /app/data

USER flightmonitor
COPY --from=build /app/flight-monitor-app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=sync
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx400m -XX:+UseContainerSupport"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
