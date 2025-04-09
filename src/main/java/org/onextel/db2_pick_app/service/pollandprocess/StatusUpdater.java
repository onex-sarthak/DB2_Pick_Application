package org.onextel.db2_pick_app.service.pollandprocess;

import org.onextel.db2_pick_app.dto.PendingSmsDto;
import java.util.List;



public interface StatusUpdater {
    void markAsFailed(List<PendingSmsDto> messages);
    void resetToPending(List<String> ids);
}