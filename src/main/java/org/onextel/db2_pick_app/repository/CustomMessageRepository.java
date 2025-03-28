package org.onextel.db2_pick_app.repository;

import org.onextel.db2_pick_app.model.MessageInfo;

import java.util.List;

public interface CustomMessageRepository {
    List<MessageInfo> fetchPendingMessagesBatch(int batchSize);

    void updateStatusFlags(List<String> ids);

    void updateMessageStatusBatch(String uniqueIds, Integer newStatus);
}
