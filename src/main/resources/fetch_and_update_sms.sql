CREATE OR REPLACE PROCEDURE SMS.FETCH_AND_UPDATE_PENDING_SMS_BATCH(
    IN BATCH_SIZE INTEGER
)
    LANGUAGE SQL
    MODIFIES SQL DATA
    DYNAMIC RESULT SETS 1
BEGIN
    -- Update and return only the required fields in one operation
    DECLARE C1 CURSOR WITH RETURN TO CALLER FOR
        SELECT
            SR_NO,
            DEST AS DESTINATION,
            MESSAGE,
            TEMPLATE_ID,
            SMS_TYPE,
            STATUS
        FROM FINAL TABLE (
                          UPDATE (
                SELECT * FROM SMS.SMS_TEMP_OUT_LOG
                WHERE STATUS = 0
                AND VENDOR = 'OneXtel'
                ORDER BY PTIME
                FETCH FIRST BATCH_SIZE ROWS ONLY
            )
            SET STATUS = 1
            );
    OPEN C1;
END