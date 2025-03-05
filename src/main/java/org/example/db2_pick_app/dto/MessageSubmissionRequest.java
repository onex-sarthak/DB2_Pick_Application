package org.example.db2_pick_app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageSubmissionRequest {    private String recipientMobile;
    private String messageContent;
    private String uniqueRequestId;
    private String dltTemplateId;
}
