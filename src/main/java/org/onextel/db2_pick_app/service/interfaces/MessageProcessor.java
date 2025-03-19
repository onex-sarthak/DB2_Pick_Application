package org.onextel.db2_pick_app.service.interfaces;

import org.onextel.db2_pick_app.model.MessageInfo;

import java.util.List;

public interface MessageProcessor {
    boolean processMessages(List<MessageInfo> messages);
}