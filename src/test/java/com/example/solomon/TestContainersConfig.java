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

    // ========================
    // MySQL
    // ========================
    private static final String MYSQL_NETWORK_ALIAS = "mysql";
    private static final int MYSQL_PORT = 3306;
    public static final String MYSQL_DATABASE = "localmysql";
    private static final String MYSQL_USERNAME = "localuser";
    private static final String MYSQL_PASSWORD = "localpass";
    public static final String MYSQL_TRIAL_TABLE = "trial";
    public static final String MYSQL_CDC_TOPIC_PREFIX = "cdc-mysql";

    public static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withNetwork(NETWORK)
            .withNetworkAliases(MYSQL_NETWORK_ALIAS)
            .withExposedPorts(MYSQL_PORT)
            .withDatabaseName(MYSQL_DATABASE)
            .withUsername(MYSQL_USERNAME)
            .withPassword(MYSQL_PASSWORD)
            .withCopyToContainer(
                    Transferable.of("""
                            GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT
                            ON *.* TO '%s'@'%%';
                            FLUSH PRIVILEGES;
                            """.formatted(MYSQL_USERNAME)),
                    "/docker-entrypoint-initdb.d/init-permissions.sql")
            .withCommand(
                    "mysqld",
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-time-zone=+00:00",
                    "--server-id=1",
                    "--log-bin=mysql-bin",
                    "--binlog-format=ROW",
                    "--binlog-row-image=FULL")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

    // ========================
    // Scylla
    // ========================
    private static final String SCYLLA_NETWORK_ALIAS = "scylla";
    private static final int SCYLLA_PORT = 9042;
    private static final String SCYLLA_KEYSPACE = "localscylla";
    private static final String SCYLLA_CHAT_MESSAGE_TABLE = "chat_message";
    private static final String SCYLLA_CDC_TOPIC_PREFIX = "cdc-scylla";

    public static final ScyllaDBContainer SCYLLA = new ScyllaDBContainer("scylladb/scylla:2026.1.7")
            .withNetwork(NETWORK)
            .withNetworkAliases(SCYLLA_NETWORK_ALIAS)
            .withExposedPorts(SCYLLA_PORT)
            .withCommand("--smp", "1", "--memory", "2G")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

    // In Scylla, the table must be created in advance before registering the source connector.
    private static void initScyllaSchema() {
        CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(SCYLLA.getHost(), SCYLLA.getMappedPort(SCYLLA_PORT)))
                .withLocalDatacenter("datacenter1")
                .build();

        session.execute("CREATE KEYSPACE IF NOT EXISTS " + SCYLLA_KEYSPACE + " WITH replication = "
                + "{'class': 'NetworkTopologyStrategy', 'datacenter1': 1}");

        session.execute("USE " + SCYLLA_KEYSPACE);

        session.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                    trial_id UUID,
                    id UUID,
                    member_id BIGINT,
                    content TEXT,
                    PRIMARY KEY (trial_id, id)
                ) WITH cdc = {'enabled': true}
                """.formatted(SCYLLA_CHAT_MESSAGE_TABLE));

        session.close();
    }

    // We can't use @ServiceConnection with Scylla..
    private static void initScyllaProperties() {
        System.setProperty("spring.cassandra.contact-points", SCYLLA.getHost());
        System.setProperty("spring.cassandra.port", SCYLLA.getMappedPort(SCYLLA_PORT).toString());
        System.setProperty("spring.cassandra.keyspace-name", SCYLLA_KEYSPACE);
        System.setProperty("spring.cassandra.local-datacenter", "datacenter1");
        System.setProperty("spring.cassandra.schema-action", "none");
    }

    // ========================
    // Kafka
    // ========================
    private static final String KAFKA_NETWORK_ALIAS = "kafka";
    private static final String KAFKA_INTERNAL_BOOTSTRAP_SERVERS = "kafka:19092";

    public static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:8.2.2"))
            .withNetwork(NETWORK)
            .withNetworkAliases(KAFKA_NETWORK_ALIAS)
            .withListener(KAFKA_INTERNAL_BOOTSTRAP_SERVERS)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

    // ========================
    // Redis
    // ========================
    private static final int REDIS_PORT = 6379;
    private static final String SESSION_REDIS_ALIAS = "session-redis";
    private static final String TRIAL_CACHE_REDIS_ALIAS = "trial-cache-redis";

    public static final GenericContainer<?> SESSION_REDIS = new GenericContainer<>(DockerImageName.parse("redis:8"))
            .withNetwork(NETWORK)
            .withNetworkAliases(SESSION_REDIS_ALIAS)
            .withExposedPorts(REDIS_PORT)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

    public static final GenericContainer<?> TRIAL_CACHE_REDIS = new GenericContainer<>(DockerImageName.parse("redis:8"))
            .withNetwork(NETWORK)
            .withNetworkAliases(TRIAL_CACHE_REDIS_ALIAS)
            .withExposedPorts(REDIS_PORT)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

    // Custom redis.session.* / redis.trial.* properties, not the standard spring.data.redis.* ones,
    // so @ServiceConnection doesn't apply here either.
    private static void initRedisProperties() {
        System.setProperty("redis.session.host", SESSION_REDIS.getHost());
        System.setProperty("redis.session.port", SESSION_REDIS.getMappedPort(REDIS_PORT).toString());
        System.setProperty("redis.trial.host", TRIAL_CACHE_REDIS.getHost());
        System.setProperty("redis.trial.port", TRIAL_CACHE_REDIS.getMappedPort(REDIS_PORT).toString());
    }

    // ========================
    // Debezium
    // ========================
    private static final int DEBEZIUM_PORT = 8083;
    private static final String MYSQL_CONNECTOR_NAME = "mysql-source-connector";
    private static final String SCYLLA_CONNECTOR_NAME = "scylla-source-connector";

    public static final ImageFromDockerfile DEBEZIUM_IMAGE = new ImageFromDockerfile("debezium")
            .withDockerfileFromBuilder(builder -> builder
                    .from("quay.io/debezium/connect:3.4.3.Final")
                    .user("root")
                    .run("mkdir -p /kafka/connect/scylla")
                    .run("curl -L https://repo1.maven.org/maven2/com/scylladb/scylla-cdc-source-connector/2.0.3/scylla-cdc-source-connector-2.0.3-jar-with-dependencies.jar -o /kafka/connect/scylla/scylla-cdc-source-connector.jar")
                    .run("chown -R kafka:kafka /kafka/connect/scylla")
                    .user("kafka")
                    .build());

    public static final GenericContainer<?> DEBEZIUM = new GenericContainer<>(DEBEZIUM_IMAGE)
            .withNetwork(NETWORK)
            .withExposedPorts(DEBEZIUM_PORT)
            .dependsOn(KAFKA)
            .withEnv("BOOTSTRAP_SERVERS", KAFKA_INTERNAL_BOOTSTRAP_SERVERS)
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
        Startables.deepStart(MYSQL, SCYLLA, KAFKA, SESSION_REDIS, TRIAL_CACHE_REDIS).join();

        // I said that it is impossible for Scylla to register Scylla source connector in advance without schema.
        // And the library 'spring-boot-testcontainers' i'm using does not support @ServiceConnection for Scylla.
        initScyllaSchema();
        initScyllaProperties();
        initRedisProperties();

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
    public DebeziumTestSupport debeziumTestSupport() {
        return new DebeziumTestSupport(DEBEZIUM);
    }

    @Bean
    public SmartInitializingSingleton initializeDebezium() {
        return () -> {
            if (!INITIALIZED.compareAndSet(false, true)) {
                return;
            }

            waitForConnectorPlugin();
            registerMySQLSourceConnector();
            registerScyllaSourceConnector();
        };
    }

    private void waitForConnectorPlugin() {
        Awaitility.await()
                .atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    String url = "http://"
                            + DEBEZIUM.getHost()
                            + ":"
                            + DEBEZIUM.getMappedPort(DEBEZIUM_PORT)
                            + "/connector-plugins";

                    HttpResponse<String> response = HttpClient.newHttpClient().send(
                            HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

                    return response.statusCode() == 200
                            && response.body().contains("io.debezium.connector.mysql.MySqlConnector")
                            && response.body().contains("com.scylladb.cdc.debezium.connector.ScyllaConnector");
                });
    }

    private void registerMySQLSourceConnector() {
        Awaitility.await()
                .atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    String url = "http://"
                            + DEBEZIUM.getHost()
                            + ":"
                            + DEBEZIUM.getMappedPort(DEBEZIUM_PORT)
                            + "/connectors";

                    Map<String, Object> config = new HashMap<>();

                    config.put("connector.class", "io.debezium.connector.mysql.MySqlConnector");
                    config.put("tasks.max", "1");

                    config.put("database.hostname", MYSQL_NETWORK_ALIAS);
                    config.put("database.port", String.valueOf(MYSQL_PORT));
                    config.put("database.user", MYSQL_USERNAME);
                    config.put("database.password", MYSQL_PASSWORD);

                    config.put("database.server.id", "1234");
                    config.put("database.include.list", MYSQL_DATABASE);
                    config.put("table.include.list", MYSQL_DATABASE + "." + MYSQL_TRIAL_TABLE);

                    config.put("topic.prefix", MYSQL_CDC_TOPIC_PREFIX);

                    config.put("schema.history.internal.kafka.bootstrap.servers", KAFKA_INTERNAL_BOOTSTRAP_SERVERS);
                    config.put("schema.history.internal.kafka.topic", "schema-changes." + MYSQL_DATABASE);

                    config.put("provide.transaction.metadata", "true");

                    Map<String, Object> request = Map.of(
                            "name", MYSQL_CONNECTOR_NAME,
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

    private void registerScyllaSourceConnector() {
        Awaitility.await()
                .atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    String url = "http://"
                            + DEBEZIUM.getHost()
                            + ":"
                            + DEBEZIUM.getMappedPort(DEBEZIUM_PORT)
                            + "/connectors";

                    Map<String, Object> config = new HashMap<>();

                    config.put("connector.class",
                            "com.scylladb.cdc.debezium.connector.ScyllaConnector");
                    config.put("scylla.cluster.ip.addresses", SCYLLA_NETWORK_ALIAS + ":" + SCYLLA_PORT);
                    config.put("topic.prefix", SCYLLA_CDC_TOPIC_PREFIX);
                    config.put("scylla.table.names", SCYLLA_KEYSPACE + "." + SCYLLA_CHAT_MESSAGE_TABLE);

                    config.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
                    config.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
                    config.put("key.converter.schemas.enable", "true");
                    config.put("value.converter.schemas.enable", "true");

                    config.put("tasks.max", "1");

                    Map<String, Object> request = Map.of(
                            "name", SCYLLA_CONNECTOR_NAME,
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
