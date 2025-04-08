package org.onextel.db2_pick_app.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
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
            String fetchAndUpdatePendingSmsBatchSql = new String(Files.readAllBytes(Paths.get("/home/hemal/Documents/DB2_Pick_Application/src/main/resources/fetch_and_update_sms.sql")));
//            String fetchPendingMessagesSql = new String(Files.readAllBytes(Paths.get("/home/hemal/Documents/DB2_Pick_Application/src/main/resources/fetch_pending_messages.sql")));
            String updateSmsStatusBatchSql = new String(Files.readAllBytes(Paths.get("/home/hemal/Documents/DB2_Pick_Application/src/main/resources/update_message_status_batch.sql")));

//            jdbcTemplate.execute(fetchPendingMessagesSql);
//            log.info("Stored procedure FETCH_PENDING_MESSAGES created");


            jdbcTemplate.execute(updateSmsStatusBatchSql);
            log.info("Stored procedure UPDATE_MESSAGE_STATUS_BATCH created");

            jdbcTemplate.execute(fetchAndUpdatePendingSmsBatchSql);
            log.info("Stored procedure PROCESS_SMS_BATCH created");
        } catch (IOException e) {
            log.error("Error reading SQL files", e);
        }
    }
}