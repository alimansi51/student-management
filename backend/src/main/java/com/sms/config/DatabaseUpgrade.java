package com.sms.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Small one-off fixes for databases created by older versions of the app,
 * for changes Hibernate's automatic schema update can't make by itself.
 */
@Component
public class DatabaseUpgrade implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public DatabaseUpgrade(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // videos used to be links ("url", required); they are uploaded files now
        jdbc.execute("ALTER TABLE IF EXISTS video DROP COLUMN IF EXISTS url");
    }
}
