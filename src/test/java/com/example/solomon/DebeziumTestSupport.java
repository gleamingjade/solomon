package com.example.solomon;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.testcontainers.containers.GenericContainer;

import static com.example.solomon.TestContainersConfig.DEBEZIUM_PORT;

public class DebeziumTestSupport {

    private static final String CONNECTORS_PATH = "/connectors";
    private static final String CONFIG_PATH = "/config";
    private static final String STATUS_PATH = "/status";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final GenericContainer<?> debezium;

    public DebeziumTestSupport(GenericContainer<?> debezium) {
        this.debezium = debezium;
        // this.debezium.followOutput(outputFrame -> System.out.print("[DEBEZIUM] " + outputFrame.getUtf8String()));
    }

    private String baseUrl() {
        return "http://" + debezium.getHost() + ":" + debezium.getMappedPort(DEBEZIUM_PORT);
    }

    private String connectorPath(String name) {
        return CONNECTORS_PATH + "/" + name;
    }

    private String get(String path) {
        try {
            return httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl() + path))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString())
                    .body();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getConnectors() {
        return get(CONNECTORS_PATH);
    }

    public String getConnectorConfig(String name) {
        return get(connectorPath(name) + CONFIG_PATH);
    }

    public String getConnectorStatus(String name) {
        return get(connectorPath(name) + STATUS_PATH);
    }
}