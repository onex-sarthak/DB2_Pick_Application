package org.onextel.db2_pick_app.repository;

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.model.MessageInfo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Repository
public class CustomMessageRepositoryImpl implements CustomMessageRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
//    @Transactional()  -> this is not required as there is only one thread executing this, and also we are using a stored procedure
    public List<MessageInfo> fetchPendingMessagesBatch(int batchSize) {
        Query query = entityManager.createNativeQuery("CALL SMS_SCHEMA.FETCH_PENDING_MESSAGES(:BATCH_SIZE)", MessageInfo.class);
        query.setParameter("BATCH_SIZE", batchSize);
        return query.getResultList();
    }

    @Override
    @Transactional
    public void updateStatusFlags(List<String> ids) {
        Query query = entityManager.createQuery("UPDATE MessageInfo m SET m.statusFlag = '1' WHERE m.uniqueId IN :ids");
        query.setParameter("ids", ids);
        query.executeUpdate();
    }

    @Override
    @Transactional
    public void updateMessageStatusBatch(String uniqueIds, Integer newStatus) {
        Query query = entityManager.createNativeQuery("CALL SMS_SCHEMA.UPDATE_MESSAGE_STATUS_BATCH(:UNIQUE_IDS , :NEW_STATUS)");
        query.setParameter("UNIQUE_IDS", uniqueIds);
        query.setParameter("NEW_STATUS", newStatus);
        query.executeUpdate();
    }

    @Override
    @Transactional
    public List<MessageInfo> fetchAndUpdatePendingMessagesBatch(int batchSize) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery(
                    "SMS_SCHEMA.PROCESS_SMS_BATCH",
                    MessageInfo.class);

            query.registerStoredProcedureParameter("BATCH_SIZE", Integer.class, ParameterMode.IN);
            query.setParameter("BATCH_SIZE", batchSize);

            // This handles both the call and result set properly
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error executing stored procedure", e);
        }
    }

}
