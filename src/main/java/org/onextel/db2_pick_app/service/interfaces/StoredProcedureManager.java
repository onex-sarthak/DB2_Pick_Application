package org.onextel.db2_pick_app.service.interfaces;

public interface StoredProcedureManager {
    void createFetchPendingMessagesProcedure();
    void createUpdateMessageStatusBatchProcedure();
}