package org.onextel.db2_pick_app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "MESSAGE_INFO", schema = "SMS_SCHEMA")
@Getter
@Setter
public class MessageInfo {

    @Id
    @Column(name = "UNIQUE_ID", nullable = false)
    private String uniqueId;

    @Column(name = "MESSAGE_CONTENT", nullable = false, length = 1000)
    private String messageContent;

    @Column(name = "RECIPIENT_MOBILE_NUMBER", nullable = false, length = 15)
    private String recipientMobileNumber;

    @Column(name = "STATUS_FLAG", nullable = false)
    private String statusFlag;

    @Column(name = "TIMESTAMP", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "DLR_STATUS", length = 10)
    private String dlrStatus;

    @Column(name = "ERROR_CODE", length = 50, nullable = true)
    private String errorCode;

}