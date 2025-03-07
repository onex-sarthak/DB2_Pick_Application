package org.onextel.db2_pick_app.repository;

import org.onextel.db2_pick_app.model.MessageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public interface MessageRepository extends JpaRepository<MessageInfo, String> {

    @Transactional
////    @Procedure(name = "SCH.FETCH_PENDING_MESSAGES")
    @Query(value = "CALL SMS_SCHEMA.FETCH_PENDING_MESSAGES(:BATCH_SIZE);", nativeQuery = true)
    List<MessageInfo> fetchPendingMessagesBatch(@Param("BATCH_SIZE") int batch_size);



    @Modifying
    @Transactional
    @Query("UPDATE MessageInfo m SET m.statusFlag = '1' WHERE m.uniqueId IN :ids")
    void updateStatusFlags(List<String> ids);


//    @Procedure(name = "Sch.UPDATE_MESSAGE_STATUS_BATCH")
    @Modifying
    @Transactional
    @Query(value = "CALL SMS_SCHEMA.UPDATE_MESSAGE_STATUS_BATCH(:UNIQUE_IDS , :NEW_STATUS);", nativeQuery = true)
    void updateMessageStatusBatch(@Param("UNIQUE_IDS") String uniqueIds,
                                  @Param("NEW_STATUS") Integer newStatus);
//
//    @Modifying
//    @Transactional
//    @Procedure(name = "updateMessageStatusBatch")
//    void updateMessageStatusBatch(@Param("UNIQUE_IDS") String uniqueIds,@Param("NEW_STATUS") String newStatus);


//    @Transactional
//    @Procedure(name="FETCH_PENDING_MESSAGES")
//    List <MessageInfo> fetchPendingMessagesBatch(@Param("BATCH_SIZE") Integer batchSize);


    List<MessageInfo> findAllByRecipientMobileIn(List<String> recipientMobileNumbers);
}
