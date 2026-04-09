FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Gradle wrapper dosyalarını kopyala
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# Gradle 8.8'e zorla (properties dosyasını komple yeniden yaz)
RUN echo "distributionBase=GRADLE_USER_HOME" > gradle/wrapper/gradle-wrapper.properties && \
    echo "distributionPath=wrapper/dists" >> gradle/wrapper/gradle-wrapper.properties && \
    echo "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.8-bin.zip" >> gradle/wrapper/gradle-wrapper.properties && \
    echo "networkTimeout=10000" >> gradle/wrapper/gradle-wrapper.properties && \
    echo "validateDistributionUrl=true" >> gradle/wrapper/gradle-wrapper.properties && \
    echo "zipStoreBase=GRADLE_USER_HOME" >> gradle/wrapper/gradle-wrapper.properties && \
    echo "zipStorePath=wrapper/dists" >> gradle/wrapper/gradle-wrapper.properties

# Dependency cache
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN ./gradlew dependencies --no-daemon || true

# Build
COPY src src
RUN ./gradlew bootJar --no-daemon -x test -x check

# Production image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]