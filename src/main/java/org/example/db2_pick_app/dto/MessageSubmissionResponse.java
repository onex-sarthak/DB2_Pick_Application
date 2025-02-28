package org.example.db2_pick_app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageSubmissionResponse {
    private boolean success;
    private String messageId;
    private String errorMessage;
    private String uniqueRequestId;
}
