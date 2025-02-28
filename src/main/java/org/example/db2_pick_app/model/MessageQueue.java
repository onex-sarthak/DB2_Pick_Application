package org.example.db2_pick_app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "MESSAGE_QUEUE", schema = "Sch")
@Getter
@Setter
public class MessageQueue {

    @Id
    @Column(name = "UNIQUE_ID")
    private String uniqueId;

    @Column(name = "MESSAGE_CONTENT", nullable = false, length = 1000)
    private String messageContent;

    @Column(name = "MOBILE_NUMBER", nullable = false, length = 15)
    private String recipientMobile;

    @Column(name = "STATUS_FLAG", nullable = false)
    private Integer statusFlag;

    @Column(name = "POLICY_NUMBER", length = 50)
    private String policyNumber;

    @Column(name = "TIMESTAMP")
    private LocalDateTime timestamp;

    @Column(name = "MESSAGE_SUBMISSION_RESPONSE", length = 1000)
    private String messageSubmissionResponse;

    @Column(name = "DLR_STATUS", length = 50)
    private String dlrStatus;

    @Column(name = "DLR_TIMESTAMP")
    private LocalDateTime dlrTimestamp;

}