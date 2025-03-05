package org.example.db2_pick_app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.repository.query.Procedure;

import java.time.LocalDateTime;


@Entity
@Table(name = "MESSAGE_INFO", schema = "SMS_SCHEMA")

//@NamedStoredProcedureQuery(
//        name = "updateMessageStatusBatch",
//        procedureName = "SMS_SCHEMA.UPDATE_MESSAGE_STATUS_BATCH",
//        parameters = {
//                @StoredProcedureParameter(mode = ParameterMode.IN, name="UNIQUE_IDS", type = String.class),
//                @StoredProcedureParameter(mode = ParameterMode.IN, name="NEW_STATUS", type = Integer.class)
//        }
//)
//@NamedStoredProcedureQuery(
//        name = "fetchPendingMessages",
//        procedureName = "SMS_SCHEMA.FETCH_PENDING_MESSAGES",
//        parameters = {
//                @StoredProcedureParameter(mode = ParameterMode.IN,name="BATCH_SIZE", type = Integer.class)
//        }
//)
@Getter
@Setter
public class MessageInfo {

    @Id
    @Column(name = "UNIQUE_ID", nullable = false)
    private String uniqueId;

    @Column(name = "MESSAGE_CONTENT", nullable = false, length = 1000)
    private String messageContent;

    @Column(name = "RECIPIENT_MOBILE_NUMBER", nullable = false, length = 15)
    private String recipientMobile;

    @Column(name = "STATUS_FLAG", nullable = false)
    private String statusFlag;

    @Column(name = "TIMESTAMP", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "DLR_STATUS", length = 10)
    private String dlrStatus;

    @Column(name = "ERROR_CODE", length = 50, nullable = true)
    private String errorCode;

}