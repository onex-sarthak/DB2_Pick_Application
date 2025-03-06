package org.example.db2_pick_app.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class DlrCallbackRequest {
    private String recipientMobileNumber;
    private String dlrStatus;
    private String dlrErrorCode;
}
