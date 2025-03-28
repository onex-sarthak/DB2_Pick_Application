package org.onextel.db2_pick_app.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.hibernate.annotations.QueryHints;
import org.onextel.db2_pick_app.model.MessageInfo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class CustomMessageRepositoryImpl implements CustomMessageRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
//    @Transactional()  -> this is not needed as there is only one thread executing this and also we are using a stored procedure
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

}
