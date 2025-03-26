package org.onextel.db2_pick_app.repository;

import org.onextel.db2_pick_app.model.MessageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Repository
public interface MessageRepository extends JpaRepository<MessageInfo, String> {

//    @Transactional
//    @Query(value = "CALL SMS_SCHEMA.FETCH_PENDING_MESSAGES(:BATCH_SIZE);", nativeQuery = true)
//    List<MessageInfo> fetchPendingMessagesBatch(@Param("BATCH_SIZE") int batch_size);
//

    @Procedure(name = "MessageInfo.FETCH_PENDING_MESSAGES")
    @Transactional
    List<Object[]> fetchPendingMessagesBatchRaw(@Param("BATCH_SIZE") Integer BATCH_SIZE);


    @Modifying
    @Transactional
    @Query("UPDATE MessageInfo m SET m.statusFlag = '1' WHERE m.uniqueId IN :ids")
    void updateStatusFlags(List<String> ids);


    default List<MessageInfo> fetchPendingMessagesBatch(Integer batchSize) {
        return fetchPendingMessagesBatchRaw(batchSize).stream()
                .map(this::mapToMessageInfo)
                .collect(Collectors.toList());
    }

    private MessageInfo mapToMessageInfo(Object[] result) {
        MessageInfo message = new MessageInfo();
        message.setUniqueId((String) result[0]);
        message.setMessageContent((String) result[1]);
        message.setRecipientMobileNumber((String) result[2]);
        message.setStatusFlag((String) result[3]);
        if (result[4] != null) {
            Timestamp timestamp = (Timestamp) result[4];
            message.setTimestamp(timestamp.toLocalDateTime());
        }
        message.setDlrStatus((String) result[5]);
        message.setErrorCode((String) result[6]);
        return message;
    }


    @Modifying
    @Transactional
    @Query(value = "CALL SMS_SCHEMA.UPDATE_MESSAGE_STATUS_BATCH(:UNIQUE_IDS , :NEW_STATUS);", nativeQuery = true)
    void updateMessageStatusBatch(@Param("UNIQUE_IDS") String uniqueIds,
                                  @Param("NEW_STATUS") Integer newStatus);


    List<MessageInfo> findAllByRecipientMobileNumberIn(List<String> recipientMobileNumbers);
}
