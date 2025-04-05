package org.onextel.db2_pick_app.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SmsTempOutLogDlr {
    private Long srNo;
    private String deliveryStatus;
    private String deliveryCode;
    private LocalDateTime deliveryTime;
}
