package org.onextel.db2_pick_app.service.pollandprocess;

import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.onextel.db2_pick_app.model.MessageStatus;
import org.onextel.db2_pick_app.repository.CustomMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DbStatusUpdater implements StatusUpdater {

    private final CustomMessageRepository customMessageRepository;

    public DbStatusUpdater(CustomMessageRepository customMessageRepository) {
        this.customMessageRepository = customMessageRepository;
    }

    @Override
    public void markAsFailed(List<PendingSmsDto> messages) {
        if (messages == null || messages.isEmpty()) return;

        try {
            String messageIds = messages.stream()
                    .map(dto -> String.valueOf(dto.getSrNo()))
                    .collect(Collectors.joining(","));
            log.info("Updating status to {} for {}", MessageStatus.FAILED, messageIds);
            customMessageRepository.updateMessageStatusBatch(messageIds, MessageStatus.FAILED);
        } catch (Exception e) {
            log.error("Error updating message status to FAILED", e);
        }
    }

    @Override
    public void resetToPending(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;

        try {
            String idString = String.join(",", ids);
            log.info("Resetting message status to {} for {}", MessageStatus.PENDING, idString);
            customMessageRepository.updateMessageStatusBatch(idString, MessageStatus.PENDING);
        } catch (Exception e) {
            log.error("Error resetting status to PENDING", e);
        }
    }
}
