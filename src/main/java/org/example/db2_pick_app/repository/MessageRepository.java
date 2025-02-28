package org.example.db2_pick_app.repository;

import org.example.db2_pick_app.model.MessageQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public interface MessageRepository extends JpaRepository<MessageQueue, String> {

    @Transactional
//    @Procedure(name = "SCH.FETCH_PENDING_MESSAGES")
    @Query(value = "CALL SCH.FETCH_PENDING_MESSAGES(:BATCH_SIZE);", nativeQuery = true)
    List<MessageQueue> fetchPendingMessagesBatch(@Param("BATCH_SIZE") int batch_size);

    @Modifying
    @Transactional
    @Query("UPDATE MessageQueue m SET m.statusFlag = 1 WHERE m.uniqueId IN :ids")
    void updateStatusFlags(List<String> ids);


//    @Procedure(name = "Sch.UPDATE_MESSAGE_STATUS_BATCH")
    @Modifying
    @Transactional
    @Query(value = "CALL Sch.UPDATE_MESSAGE_STATUS_BATCH(:UNIQUE_IDS , :NEW_STATUS);", nativeQuery = true)
    void updateMessageStatusBatch(@Param("UNIQUE_IDS") String uniqueIds,
                                  @Param("NEW_STATUS") Integer newStatus);
}
