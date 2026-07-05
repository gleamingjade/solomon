package com.example.solomon;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.awaitility.Awaitility;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.scylladb.ScyllaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;
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
                        .withExposedPorts(3306)
                        .withDatabaseName("localdb")
                        .withUsername("localuser")
                        .withPassword("localpass")
                        .withCopyToContainer(
                                        Transferable.of("""
                                                        GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT
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
                        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

        public static final ScyllaDBContainer SCYLLA = new ScyllaDBContainer("scylladb/scylla:2026.1.7")
                        .withNetwork(NETWORK)
                        .withNetworkAliases("scylla")
                        .withExposedPorts(9042)
                        .withCommand("--smp", "1", "--memory", "2G")
                        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

        // @formatter:off
        // In Scylla, the table must be created in advance before registering the source connector.
        // @formatter:on
        private static void initScyllaSchema() {
                CqlSession session = CqlSession.builder()
                                .addContactPoint(new InetSocketAddress(
                                                SCYLLA.getHost(),
                                                SCYLLA.getMappedPort(9042)))
                                .withLocalDatacenter("datacenter1")
                                .build();

                session.execute("CREATE KEYSPACE IF NOT EXISTS localscylla WITH replication = "
                                + "{'class': 'NetworkTopologyStrategy', 'datacenter1': 1}");

                session.execute("USE localscylla");

                session.execute("""
                                CREATE TABLE IF NOT EXISTS chat_message (
                                    trial_id UUID,
                                    id UUID,
                                    member_id BIGINT,
                                    content TEXT,
                                    PRIMARY KEY (trial_id, id)
                                ) WITH cdc = {'enabled': true}
                                                            """);

                session.close();
        }

        // We can't use @ServiceConnection with Scylla..
        private static void initScyllaProperties() {
                System.setProperty("spring.cassandra.contact-points", SCYLLA.getHost());
                System.setProperty("spring.cassandra.port", SCYLLA.getMappedPort(9042).toString());
                System.setProperty("spring.cassandra.local-datacenter", "datacenter1");
                System.setProperty("spring.cassandra.schema-action", "none");
        }

        public static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
                        DockerImageName.parse("confluentinc/cp-kafka:8.2.2"))
                        .withNetwork(NETWORK)
                        .withNetworkAliases("kafka")
                        .withListener("kafka:19092")
                        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

        public static final ImageFromDockerfile debeziumImage = new ImageFromDockerfile("debezium")
                        .withDockerfileFromBuilder(builder -> builder
                                        .from("quay.io/debezium/connect:3.4.3.Final")
                                        .user("root")
                                        .run("mkdir -p /kafka/connect/scylla")
                                        .run("curl -L https://repo1.maven.org/maven2/com/scylladb/scylla-cdc-source-connector/2.0.3/scylla-cdc-source-connector-2.0.3-jar-with-dependencies.jar -o /kafka/connect/scylla/scylla-cdc-source-connector.jar")
                                        .run("chown -R kafka:kafka /kafka/connect/scylla")
                                        .user("kafka")
                                        .build());

        public static final GenericContainer<?> DEBEZIUM = new GenericContainer<>(debeziumImage)
                        .withNetwork(NETWORK)
                        .withExposedPorts(8083)
                        .dependsOn(KAFKA)
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
                        .waitingFor(Wait.forHttp("/").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));

        static {
                // Parallel start.
                Startables.deepStart(MYSQL, SCYLLA, KAFKA).join();

                // @formatter:off
                // I said that it is impossible for Scylla to register Scylla source connector in advance without schema.
                // And the library 'spring-boot-testcontainers' i'm using does not support @ServiceConnection for Scylla.
                // @formatter:on
                initScyllaSchema();
                initScyllaProperties();

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
        public KafkaTestSupport kafkaTestSupport() {
                return new KafkaTestSupport(KAFKA.getBootstrapServers());
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