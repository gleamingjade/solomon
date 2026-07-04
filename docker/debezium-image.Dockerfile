FROM quay.io/debezium/connect:3.4.3.Final

USER root

RUN mkdir -p /kafka/connect/scylla

RUN curl -L \
  https://repo1.maven.org/maven2/com/scylladb/scylla-cdc-source-connector/2.0.3/scylla-cdc-source-connector-2.0.3-jar-with-dependencies.jar \
  -o /kafka/connect/scylla/scylla-cdc-source-connector.jar

RUN chown -R kafka:kafka /kafka/connect/scylla

USER kafka