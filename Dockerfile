FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat build.gradle.kts ./
COPY gradle/ gradle/
COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar

RUN jdeps --ignore-missing-deps -q \
    --recursive \
    --multi-release 21 \
    --print-module-deps \
    --class-path 'build/libs/*' \
    build/libs/parental-control.jar > deps.txt

RUN jlink \
    --add-modules $(cat deps.txt),java.logging,java.compiler,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.crypto.ec,jdk.unsupported \
    --compress zip-9 \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --output /custom-jre

# Runtime stage \
FROM gcr.io/distroless/base-debian12
WORKDIR /app

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

COPY --from=build /custom-jre /opt/java/openjdk

COPY --from=build /app/build/libs/parental-control.jar ./app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD ["java", "-XX:+UseZGC", "-jar", "app.jar", "health"]

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-Xmx128m", "-XX:+UseZGC", "-jar", "app.jar"]