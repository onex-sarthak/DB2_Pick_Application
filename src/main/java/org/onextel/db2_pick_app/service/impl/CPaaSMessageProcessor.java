package org.onextel.db2_pick_app.service.impl;

import org.onextel.db2_pick_app.model.MessageInfo;
import org.onextel.db2_pick_app.service.CPaaSIntegrationService;
import org.onextel.db2_pick_app.service.interfaces.MessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CPaaSMessageProcessor implements MessageProcessor {
    private final CPaaSIntegrationService cPaaSIntegrationService;

    @Autowired
    public CPaaSMessageProcessor(CPaaSIntegrationService cPaaSIntegrationService) {
        this.cPaaSIntegrationService = cPaaSIntegrationService;
    }

    @Override
    public boolean processMessages(List<MessageInfo> messages) {
        return cPaaSIntegrationService.sendMessagesInBatch(messages);
    }
}
