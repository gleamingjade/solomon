package com.example.solomon;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.awaitility.Awaitility;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    // Liquibase
    // ========================
    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));

    public static final GenericContainer<?> LIQUIBASE_MYSQL = new GenericContainer<>(
            DockerImageName.parse("liquibase/liquibase:5.0.2"))
            .withNetwork(NETWORK)
            .dependsOn(MYSQL)
            .withCopyFileToContainer(
                    MountableFile.forHostPath(PROJECT_ROOT.resolve("lbase/mysql")),
                    "/liquibase/changelog")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(PROJECT_ROOT.resolve("build/liquibase-drivers/mysql")),
                    "/liquibase/lib")
            .withCommand(
                    "--url=jdbc:mysql://" + MYSQL_NETWORK_ALIAS + ":" + MYSQL_PORT + "/" + MYSQL_DATABASE,
                    "--username=" + MYSQL_USERNAME,
                    "--password=" + MYSQL_PASSWORD,
                    "--driver=com.mysql.cj.jdbc.Driver",
                    "--changeLogFile=db.changelog-master.xml",
                    "update")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(5)));

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

    private static void initKafkaProperties() {
        System.setProperty("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());
    }

    // ========================
    // Redis
    // ========================
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_ALIAS = "redis";

    public static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:8"))
            .withNetwork(NETWORK)
            .withNetworkAliases(REDIS_ALIAS)
            .withExposedPorts(REDIS_PORT)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

    // Custom redis.* properties, not the standard spring.data.redis.* ones, so @ServiceConnection
    // doesn't apply here either.
    private static void initRedisProperties() {
        System.setProperty("redis.host", REDIS.getHost());
        System.setProperty("redis.port", REDIS.getMappedPort(REDIS_PORT).toString());
    }

    // ========================
    // Debezium
    // ========================
    public static final int DEBEZIUM_PORT = 8083;
    private static final String MYSQL_CONNECTOR_NAME = "mysql-source-connector";

    public static final GenericContainer<?> DEBEZIUM = new GenericContainer<>(
            DockerImageName.parse("quay.io/debezium/connect:3.4.3.Final"))
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
        Startables.deepStart(MYSQL, KAFKA, REDIS).join();

        initRedisProperties();
        initKafkaProperties();

        // Migrations must finish before Debezium registers connectors against the migrated tables.
        LIQUIBASE_MYSQL.start();

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
                            && response.body().contains("io.debezium.connector.mysql.MySqlConnector");
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

}
