package org.onextel.db2_pick_app.service;

import org.onextel.db2_pick_app.dto.DlrCallbackRequest;
import org.onextel.db2_pick_app.model.MessageInfo;
//import org.onextel.db2_pick_app.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DlrCallbackService {
//    private final MessageRepository messageRepository;
//
//    @Autowired
//    public DlrCallbackService(MessageRepository messageRepository) {
//        this.messageRepository = messageRepository;
//    }
//
//    @Transactional
//    public void processDlrCallbacks(List<DlrCallbackRequest> dlrCallbackRequests) {
//        // Fetch all records to be updated
//        List<MessageInfo> messagesToUpdate = messageRepository.findAllByRecipientMobileNumberIn(
//                dlrCallbackRequests.stream()
//                        .map(DlrCallbackRequest::getRecipientMobileNumber)
//                        .toList()
//        );
//
//        // Update the records in memory
//        for (MessageInfo message : messagesToUpdate) {
//            dlrCallbackRequests.stream()
//                    .filter(request -> request.getRecipientMobileNumber().equals(message.getRecipientMobileNumber()))
//                    .findFirst()
//                    .ifPresent(request -> {
//                        message.setDlrStatus(request.getDlrStatus());
//                        message.setErrorCode(request.getDlrErrorCode());
//                    });
//        }
//
//        // Save all updates in a batch
//        messageRepository.saveAll(messagesToUpdate);
//    }
}