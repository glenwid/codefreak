FROM gradle:jdk8 AS build

COPY . /build

WORKDIR /build
RUN ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false clean bootJar

FROM openjdk:8-alpine

ARG GIT_COMMIT=""
ARG GIT_TAG="$GIT_COMMIT"

# Add some system dependecies required by libraries
# - gcompat for jsass
RUN apk add --no-cache gcompat

EXPOSE 8080

# Run everything as unprivileged user
RUN addgroup -g 1000 codefreak \
    && adduser -Su 1000 -G codefreak codefreak \
    && mkdir /app \
    && chown -R codefreak:codefreak /app

COPY --from=build --chown=1000:1000 /build/build/libs/ /app

# Create a consistent symlink to the jar file without any version suffix and make sure a codefreak.jar exists afterwards
RUN find /app -maxdepth 1 -name 'codefreak-*.jar' -exec ln -fs {} /app/codefreak.jar \; \
    && [ -f "/app/codefreak.jar" ]

USER codefreak

# Override this when running the container with -e ENV="dev"
ENV ENV "prod"
ENV SPRING_PROFILES_ACTIVE "$ENV"
ENV SENTRY_ENVIRONMENT "$ENV"
ENV SENTRY_RELEASE "${GIT_TAG}"

WORKDIR /app
CMD ["java", "-jar", "/app/codefreak.jar"]
