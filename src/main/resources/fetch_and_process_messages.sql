CREATE OR REPLACE PROCEDURE PROCESS_SMS_BATCH (
    IN batch_size INTEGER
)
LANGUAGE SQL
MODIFIES SQL DATA
BEGIN
    -- Declare cursor that will both update and return the rows
    DECLARE result_cursor CURSOR WITH RETURN FOR
SELECT UNIQUE_ID, MESSAGE_CONTENT, RECIPIENT_MOBILE_NUMBER
FROM FINAL TABLE (
            UPDATE (
                SELECT *
                FROM SMS_SCHEMA.MESSAGE_INFO
                WHERE STATUS_FLAG = '0'
                ORDER BY TIMESTAMP
                FETCH FIRST batch_size ROWS ONLY
            )
            SET STATUS_FLAG = 1
        );

-- Open the cursor which executes the update and returns results
OPEN result_cursor;
END