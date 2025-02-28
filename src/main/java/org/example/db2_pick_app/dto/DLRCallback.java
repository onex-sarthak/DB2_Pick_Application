package org.example.db2_pick_app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DLRCallback {
    private String uniqueRequestId;
    private String status;
    private String timestamp;
}
