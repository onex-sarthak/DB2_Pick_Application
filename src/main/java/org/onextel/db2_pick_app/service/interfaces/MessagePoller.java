package org.onextel.db2_pick_app.service.interfaces;

import org.onextel.db2_pick_app.model.MessageInfo;

import java.util.List;

public interface MessagePoller {
    List<MessageInfo> fetchPendingMessages(int batchSize);
}