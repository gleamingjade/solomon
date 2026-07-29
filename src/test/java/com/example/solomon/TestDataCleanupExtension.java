package com.example.solomon;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Tests that commit real transactions (e.g. to let Debezium see the change) leave rows behind in
// the shared, static TestContainers. Register this extension on any such test to wipe MySQL/Scylla
// back to an empty state afterwards, instead of hand-rolling per-test cleanup.
public class TestDataCleanupExtension implements AfterEachCallback {

    private static final Set<String> MYSQL_TABLES_TO_KEEP = Set.of("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");
    private static final Set<String> SCYLLA_TABLES_TO_KEEP = Set.of("databasechangelog", "databasechangeloglock");

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        truncateMysql();
        truncateScylla();
    }

    private void truncateMysql() throws Exception {
        try (Connection connection = TestContainersConfig.MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }

            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String table : tables) {
                if (MYSQL_TABLES_TO_KEEP.contains(table.toUpperCase())) {
                    continue;
                }
                statement.execute("TRUNCATE TABLE `" + table + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void truncateScylla() {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(
                        TestContainersConfig.SCYLLA.getHost(),
                        TestContainersConfig.SCYLLA.getMappedPort(TestContainersConfig.SCYLLA_PORT)))
                .withLocalDatacenter("datacenter1")
                .withKeyspace(TestContainersConfig.SCYLLA_KEYSPACE)
                .build()) {

            List<String> tables = new ArrayList<>();
            for (Row row : session.execute(
                    "SELECT table_name FROM system_schema.tables WHERE keyspace_name = '"
                            + TestContainersConfig.SCYLLA_KEYSPACE + "'")) {
                tables.add(row.getString("table_name"));
            }

            for (String table : tables) {
                // Tables like "<name>$paxos" are Scylla/Cassandra's own internal bookkeeping for
                // lightweight-transaction tables (e.g. Liquibase's lock table); they aren't ours to touch.
                if (SCYLLA_TABLES_TO_KEEP.contains(table.toLowerCase()) || table.contains("$")) {
                    continue;
                }

                String cql = "TRUNCATE \"" + TestContainersConfig.SCYLLA_KEYSPACE + "\".\"" + table + "\"";
                try {
                    session.execute(cql);
                } catch (RuntimeException e) {
                    throw new IllegalStateException("Failed to truncate Scylla table with CQL: " + cql, e);
                }
            }
        }
    }

}