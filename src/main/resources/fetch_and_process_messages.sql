CREATE OR REPLACE PROCEDURE SMS_SCHEMA.PROCESS_SMS_BATCH (
    IN BATCH_SIZE INTEGER
)
    LANGUAGE SQL
    MODIFIES SQL DATA
    DYNAMIC RESULT SETS 1
BEGIN
    -- Update and return in one operation
    DECLARE C1 CURSOR WITH RETURN TO CALLER FOR
        SELECT * FROM FINAL TABLE (
                                   UPDATE (
                SELECT * FROM SMS_SCHEMA.MESSAGE_INFO
                WHERE STATUS_FLAG = '0'
                ORDER BY TIMESTAMP
                FETCH FIRST BATCH_SIZE ROWS ONLY
            )
            SET STATUS_FLAG = '1'
            );
    OPEN C1;
END