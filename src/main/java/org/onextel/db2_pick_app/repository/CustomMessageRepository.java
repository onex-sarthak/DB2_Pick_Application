package org.onextel.db2_pick_app.repository;

import org.onextel.db2_pick_app.model.MessageInfo;
import org.onextel.db2_pick_app.model.MessageStatus;

import java.util.List;

public interface CustomMessageRepository {
    List<MessageInfo> fetchPendingMessagesBatch(int batchSize);

    void updateStatusFlags(List<String> ids);

    void updateMessageStatusBatch(String uniqueIds, MessageStatus newStatus);

    List<MessageInfo> fetchAndUpdatePendingMessagesBatch(int batchSize);
}
