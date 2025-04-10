package org.onextel.db2_pick_app.service.pollandprocess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CPaaSMessageSender implements MessageSender {

    private final CPaaSIntegrationService cPaaSIntegrationService;

    @Override
    public boolean sendBatch(List<PendingSmsDto> batch) {
        try {
            return Boolean.TRUE.equals(cPaaSIntegrationService.sendMessagesInBatch(batch));
        } catch (Exception e) {
            log.error("Exception occurred while sending message batch", e);
            return false;
        }
    }
}