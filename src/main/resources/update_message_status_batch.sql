CREATE OR REPLACE PROCEDURE SMS_SCHEMA.UPDATE_MESSAGE_STATUS_BATCH(
    IN UNIQUE_IDS VARCHAR(10000),  -- Comma-separated list of UNIQUE_ID values
    IN NEW_STATUS INTEGER         -- New status value to set
)
LANGUAGE SQL
BEGIN
    DECLARE SQL_QUERY VARCHAR(32000);
    DECLARE v_unique_ids VARCHAR(10000);

    -- First remove all spaces, then replace commas with quoted commas
    SET v_unique_ids = REPLACE(REPLACE(TRIM(UNIQUE_IDS), ' ', ''), ',', ''',''');

    -- Add single quotes at the beginning and end of the list
    SET v_unique_ids = '''' || v_unique_ids || '''';

    -- Build the SQL query dynamically
    SET SQL_QUERY = 'UPDATE SMS_SCHEMA.MESSAGE_INFO SET STATUS_FLAG = ' || CAST(NEW_STATUS AS VARCHAR(10)) ||
                    ' WHERE UNIQUE_ID IN (' || v_unique_ids || ')';

    -- Execute the dynamically constructed SQL query
EXECUTE IMMEDIATE SQL_QUERY;

-- Commit the changes
COMMIT;
END;