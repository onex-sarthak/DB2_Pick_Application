package org.onextel.db2_pick_app.service.impl;


import org.onextel.db2_pick_app.service.interfaces.StoredProcedureManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JdbcStoredProcedureManager implements StoredProcedureManager {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcStoredProcedureManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createFetchPendingMessagesProcedure() {
        String sql = """
            CREATE OR REPLACE PROCEDURE SMS_SCHEMA.FETCH_PENDING_MESSAGES(
                IN BATCH_SIZE INTEGER
            )
            LANGUAGE SQL
            SPECIFIC FETCH_PENDING_MESSAGES
            RESULT SETS 1
            BEGIN
                DECLARE message_cursor CURSOR WITH RETURN FOR
                    SELECT *
                    FROM SMS_SCHEMA.MESSAGE_INFO
                    WHERE STATUS_FLAG = 0
                    ORDER BY TIMESTAMP ASC
                    FETCH FIRST BATCH_SIZE ROWS ONLY;

                SET CURRENT LOCK TIMEOUT 10;
                OPEN message_cursor;
            END
        """;
        jdbcTemplate.execute(sql);
    }

    @Override
    public void createUpdateMessageStatusBatchProcedure() {
        String sql = """
            CREATE OR REPLACE PROCEDURE SMS_SCHEMA.UPDATE_MESSAGE_STATUS_BATCH(
                IN UNIQUE_IDS VARCHAR(1000),
                IN NEW_STATUS INTEGER
            )
            LANGUAGE SQL
            BEGIN
                DECLARE SQL_QUERY VARCHAR(2000);
                DECLARE v_unique_ids VARCHAR(1000);

                SET v_unique_ids = REPLACE(TRIM(UNIQUE_IDS), ',', ''',''');
                SET v_unique_ids = '''' || v_unique_ids || '''';

                SET SQL_QUERY = 'UPDATE SMS_SCHEMA.MESSAGE_INFO SET STATUS_FLAG = ' || NEW_STATUS ||
                                 ' WHERE UNIQUE_ID IN (' || v_unique_ids || ')';

                EXECUTE IMMEDIATE SQL_QUERY;
                COMMIT;
            END;
        """;
        jdbcTemplate.execute(sql);
    }
}
