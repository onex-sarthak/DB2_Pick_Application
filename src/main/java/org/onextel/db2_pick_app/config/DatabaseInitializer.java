package org.onextel.db2_pick_app.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@Slf4j
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        createStoredProcedures();
    }

    private void createStoredProcedures() {
        try {
            String fetchAndProcessMessagesSql = new String(Files.readAllBytes(Paths.get("/home/hemal/Documents/DB2_Pick_Application/src/main/resources/fetch_and_process_messages.sql")));
            String fetchPendingMessagesSql = new String(Files.readAllBytes(Paths.get("/home/hemal/Documents/DB2_Pick_Application/src/main/resources/fetch_pending_messages.sql")));
            String updateMessageStatusBatchSql = new String(Files.readAllBytes(Paths.get("/home/hemal/Documents/DB2_Pick_Application/src/main/resources/update_message_status_batch.sql")));

            jdbcTemplate.execute(fetchPendingMessagesSql);
            log.info("Stored procedure FETCH_PENDING_MESSAGES created");


            jdbcTemplate.execute(updateMessageStatusBatchSql);
            log.info("Stored procedure UPDATE_MESSAGE_STATUS_BATCH created");

            jdbcTemplate.execute(fetchAndProcessMessagesSql);
            log.info("Stored procedure PROCESS_SMS_BATCH created");
        } catch (IOException e) {
            log.error("Error reading SQL files", e);
        }
    }
}