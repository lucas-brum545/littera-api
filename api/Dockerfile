# --- Estágio de Build ---
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle

RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew bootJar --no-daemon

# --- Estágio Final (Execução) ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -g 1001 spring && useradd -u 1001 -g spring -m spring
USER spring

COPY --from=builder --chown=spring:spring /build/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]