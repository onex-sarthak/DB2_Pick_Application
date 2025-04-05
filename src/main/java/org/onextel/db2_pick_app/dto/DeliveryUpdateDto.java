package org.onextel.db2_pick_app.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryUpdateDto {
    private Long srNo;
    private String deliveryStatus;
    private String deliveryCode;
    private LocalDateTime deliveryTime;
}
