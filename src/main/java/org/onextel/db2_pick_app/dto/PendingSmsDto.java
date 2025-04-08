package org.onextel.db2_pick_app.dto;

import lombok.Data;

@Data
public class PendingSmsDto {
    private Long srNo;
    private String destination;
    private String message;
    private String templateId;
    private String smsType;
    private Integer status;
}
