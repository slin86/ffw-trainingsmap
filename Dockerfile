##############################################################################
# Build stage
##############################################################################
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY --chown=1000:1000 . .
USER 1000
RUN ./gradlew bootJar --no-daemon

##############################################################################
# Runtime stage
##############################################################################
FROM eclipse-temurin:21-jre
RUN useradd -r -u 1000 -s /bin/false appuser
WORKDIR /app
COPY --from=build --chown=appuser:appuser /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
