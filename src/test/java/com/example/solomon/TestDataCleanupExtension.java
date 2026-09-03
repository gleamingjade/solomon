package com.example.solomon;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Tests that commit real transactions (e.g. to let Debezium see the change) leave rows behind in
// the shared, static TestContainers. Register this extension on any such test to wipe MySQL back
// to an empty state afterwards, instead of hand-rolling per-test cleanup.
public class TestDataCleanupExtension implements AfterEachCallback {

    private static final Set<String> MYSQL_TABLES_TO_KEEP = Set.of("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        truncateMysql();
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

}
