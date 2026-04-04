FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Gradle wrapper dosyalarını kopyala
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# Dependency cache için önce build dosyalarını kopyala
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN ./gradlew dependencies --no-daemon || true

# Kaynak kodu kopyala ve build et
COPY src src
RUN ./gradlew bootJar --no-daemon -x test -x check

# Production image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]