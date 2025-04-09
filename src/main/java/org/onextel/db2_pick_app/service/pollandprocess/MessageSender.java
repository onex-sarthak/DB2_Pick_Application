package org.onextel.db2_pick_app.service.pollandprocess;

import org.onextel.db2_pick_app.dto.PendingSmsDto;

import java.util.List;

public interface MessageSender {
    boolean sendBatch(List<PendingSmsDto> batch);
}
