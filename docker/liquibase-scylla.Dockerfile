FROM gradle:8-jdk21 AS deps
WORKDIR /deps
COPY liquibase-scylla-deps.build.gradle /deps/build.gradle
RUN gradle copyDeps --no-daemon

FROM liquibase/liquibase:5.0.2
COPY --from=deps /deps/build/resolved-libs/*.jar /liquibase/lib/
