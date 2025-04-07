CREATE OR REPLACE PROCEDURE SMS_SCHEMA.UPDATE_SMS_STATUS_BATCH(
    IN SR_NOS VARCHAR(32000),  -- Comma-separated list of SR_NO values
    IN NEW_STATUS INTEGER      -- New status value to set
)
    LANGUAGE SQL
BEGIN
    DECLARE SQL_QUERY VARCHAR(32000);
    DECLARE v_sr_nos VARCHAR(10000);

    -- Clean up the input string (remove spaces and format for IN clause)
    SET v_sr_nos = REPLACE(REPLACE(TRIM(SR_NOS), ' ', ''), ',', ''',''');

    -- Add single quotes at the beginning and end of the list if needed
    IF v_sr_nos <> '' THEN
        SET v_sr_nos = '''' || v_sr_nos || '''';
    END IF;

    -- Build the SQL query dynamically
    SET SQL_QUERY = 'UPDATE SMS.SMS_TEMP_OUT_LOG SET STATUS = ' || CAST(NEW_STATUS AS VARCHAR(10));

    -- Only add WHERE clause if we have IDs
    IF v_sr_nos <> '' THEN
        SET SQL_QUERY = SQL_QUERY || ' WHERE SR_NO IN (' || v_sr_nos || ')';
    END IF;

    -- Execute the dynamically constructed SQL query
    EXECUTE IMMEDIATE SQL_QUERY;

    -- Commit the changes
    COMMIT;
END