package com.example.solomon;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.awaitility.Awaitility;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

        private static final Network NETWORK = Network.newNetwork();

        private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

        public static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
                        .withNetwork(NETWORK)
                        .withNetworkAliases("mysql")
                        .withDatabaseName("localdb")
                        .withUsername("localuser")
                        .withPassword("localpass")
                        .withCopyToContainer(
                                        Transferable.of("""
                                                        GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION CLIENT, REPLICATION SLAVE
                                                        ON *.* TO 'localuser'@'%';
                                                        FLUSH PRIVILEGES;
                                                        """),
                                        "/docker-entrypoint-initdb.d/init-permissions.sql")
                        .withCommand(
                                        "mysqld",
                                        "--character-set-server=utf8mb4",
                                        "--collation-server=utf8mb4_unicode_ci",
                                        "--default-time-zone=+09:00",
                                        "--server-id=1",
                                        "--log-bin=mysql-bin",
                                        "--binlog-format=ROW",
                                        "--binlog-row-image=FULL")
                        .waitingFor(
                                        Wait.forLogMessage(".*ready for connections.*\\n", 1)
                                                        .withStartupTimeout(Duration.ofMinutes(2)));

        public static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
                        DockerImageName.parse("confluentinc/cp-kafka:8.2.2"))
                        .withNetwork(NETWORK)
                        .withNetworkAliases("kafka")
                        .withListener("kafka:19092")
                        .waitingFor(
                                        Wait.forLogMessage(".*started.*", 1)
                                                        .withStartupTimeout(Duration.ofMinutes(2)));

        public static final GenericContainer<?> DEBEZIUM = new GenericContainer<>(
                        DockerImageName.parse("quay.io/debezium/connect:3.4.3.Final"))
                        .withNetwork(NETWORK)
                        .withExposedPorts(8083)
                        .dependsOn(MYSQL, KAFKA)
                        .withEnv("BOOTSTRAP_SERVERS", "kafka:19092")
                        .withEnv("GROUP_ID", "1")
                        .withEnv("CONFIG_STORAGE_TOPIC", "connect-configs")
                        .withEnv("OFFSET_STORAGE_TOPIC", "connect-offsets")
                        .withEnv("STATUS_STORAGE_TOPIC", "connect-status")
                        .withEnv("KEY_CONVERTER",
                                        "org.apache.kafka.connect.json.JsonConverter")
                        .withEnv("VALUE_CONVERTER",
                                        "org.apache.kafka.connect.json.JsonConverter")
                        .withEnv("KEY_CONVERTER_SCHEMAS_ENABLE", "false")
                        .withEnv("VALUE_CONVERTER_SCHEMAS_ENABLE", "false")
                        .waitingFor(
                                        Wait.forHttp("/")
                                                        .forStatusCode(200)
                                                        .withStartupTimeout(Duration.ofMinutes(2)));

        static {
                Startables.deepStart(MYSQL, KAFKA).join();
                DEBEZIUM.start();
        }

        @Bean
        @ServiceConnection
        public MySQLContainer mysqlContainer() {
                return MYSQL;
        }

        @Bean
        public GenericContainer<?> kafkaContainer() {
                return KAFKA;
        }

        @Bean
        public SmartInitializingSingleton initializeDebezium() {
                return () -> {
                        if (!INITIALIZED.compareAndSet(false, true)) {
                                return;
                        }

                        waitForConnectorPlugin(DEBEZIUM);
                        registerDebeziumConnector(DEBEZIUM);
                };
        }

        private void waitForConnectorPlugin(GenericContainer<?> debezium) {
                Awaitility.await()
                                .atMost(Duration.ofMinutes(1))
                                .pollInterval(Duration.ofSeconds(2))
                                .ignoreExceptions()
                                .until(() -> {

                                        String url = "http://"
                                                        + debezium.getHost()
                                                        + ":"
                                                        + debezium.getMappedPort(8083)
                                                        + "/connector-plugins";

                                        HttpResponse<String> response = HttpClient.newHttpClient().send(
                                                        HttpRequest.newBuilder()
                                                                        .uri(URI.create(url))
                                                                        .GET()
                                                                        .build(),
                                                        HttpResponse.BodyHandlers.ofString());

                                        return response.statusCode() == 200
                                                        && response.body().contains(
                                                                        "io.debezium.connector.mysql.MySqlConnector");
                                });
        }

        private void registerDebeziumConnector(GenericContainer<?> debezium) {
                Awaitility.await()
                                .atMost(Duration.ofMinutes(1))
                                .pollInterval(Duration.ofSeconds(2))
                                .ignoreExceptions()
                                .untilAsserted(() -> {
                                        String url = "http://"
                                                        + debezium.getHost()
                                                        + ":"
                                                        + debezium.getMappedPort(8083)
                                                        + "/connectors";

                                        Map<String, Object> config = new HashMap<>();

                                        config.put("connector.class", "io.debezium.connector.mysql.MySqlConnector");
                                        config.put("tasks.max", "1");

                                        config.put("database.hostname", "mysql");
                                        config.put("database.port", "3306");
                                        config.put("database.user", "localuser");
                                        config.put("database.password", "localpass");

                                        config.put("database.server.id", "1234");
                                        config.put("database.include.list", "localdb");
                                        config.put("table.include.list", "localdb.trial");

                                        config.put("topic.prefix", "cdc-mysql");

                                        config.put("schema.history.internal.kafka.bootstrap.servers", "kafka:19092");
                                        config.put("schema.history.internal.kafka.topic", "schema-changes.localdb");

                                        config.put("provide.transaction.metadata", "true");

                                        Map<String, Object> request = Map.of(
                                                        "name", "mysql-source-connector",
                                                        "config", config);

                                        String json = new ObjectMapper().writeValueAsString(request);

                                        HttpResponse<String> response = HttpClient.newHttpClient().send(
                                                        HttpRequest.newBuilder()
                                                                        .uri(URI.create(url))
                                                                        .header("Content-Type", "application/json")
                                                                        .POST(HttpRequest.BodyPublishers.ofString(json))
                                                                        .build(),
                                                        HttpResponse.BodyHandlers.ofString());

                                        if (response.statusCode() != 201
                                                        && response.statusCode() != 409) {
                                                throw new IllegalStateException(response.body());
                                        }
                                });
        }

}