package org.onextel.db2_pick_app.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SmsTempOutLog {
    private Long srNo;
    private String destination;
    private String message;
    private String templateId;
    private String smsType;
    private Integer status;
    private String vendor;
    private LocalDateTime processingTime;
}
