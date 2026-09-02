package com.family.finance;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest {
    @Test
    void migratesEmptyDatabaseAndIsSafeToRunAgain() throws Exception {
        MySQLContainer<?> container = null;
        String url = System.getenv("TEST_DB_URL");
        String username = System.getenv().getOrDefault("TEST_DB_USERNAME", "test");
        String password = System.getenv().getOrDefault("TEST_DB_PASSWORD", "test");
        try {
            if (url == null || url.isBlank()) {
                container = new MySQLContainer<>("mysql:8.4")
                        .withDatabaseName("family_finance_test")
                        .withUsername(username)
                        .withPassword(password);
                try {
                    container.start();
                    url = container.getJdbcUrl();
                } catch (IllegalStateException unavailable) {
                    Assumptions.assumeTrue(false,
                            "Testcontainers 无法连接本机 Docker；请设置 TEST_DB_URL 使用专用 MySQL 测试库");
                }
            }
            Flyway flyway = Flyway.configure()
                    .cleanDisabled(false)
                    .dataSource(url, username, password)
                    .locations("classpath:db/migration")
                    .load();
            flyway.clean();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(flyway.migrate().migrationsExecuted).isZero();

            try (Connection connection = DriverManager.getConnection(url, username, password);
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT table_name FROM information_schema.tables " +
                                 "WHERE table_schema = DATABASE()")) {
                Set<String> tables = new HashSet<>();
                while (resultSet.next()) tables.add(resultSet.getString(1));
                assertThat(tables).contains("household", "app_user", "finance_category", "ledger_entry");
            }
        } finally {
            if (container != null) container.stop();
        }
    }
}
