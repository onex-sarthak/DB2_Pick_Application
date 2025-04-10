package org.onextel.db2_pick_app.repository;

import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.onextel.db2_pick_app.model.MessageStatus;

import java.util.List;

public interface CustomMessageRepository {

    void updateMessageStatusBatch(String uniqueIds, MessageStatus newStatus);

    List<PendingSmsDto> fetchAndUpdatePendingMessagesBatch(int batchSize);

    List<PendingSmsDto> fetchPendingMessagesBatch(int batchSize);
}
