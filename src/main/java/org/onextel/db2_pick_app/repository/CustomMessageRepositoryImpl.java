package org.onextel.db2_pick_app.repository;

import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.onextel.db2_pick_app.model.MessageStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Slf4j

@Repository
public class CustomMessageRepositoryImpl implements CustomMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomMessageRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void updateMessageStatusBatch(String srNos, MessageStatus newStatus) {
        jdbcTemplate.update("CALL SMS.UPDATE_SMS_STATUS_BATCH(?, ?)",
                srNos, newStatus.getDbValue());
    }

    @Transactional
    @Override
    public List<PendingSmsDto> fetchAndUpdatePendingMessagesBatch(int batchSize) {
        String sql = "CALL SMS.FETCH_AND_UPDATE_PENDING_SMS_BATCH(?)";
        List<PendingSmsDto> result = jdbcTemplate.query(sql, new Object[]{batchSize}, new PendingSmsRowMapper());
        log.info("Fetched {} pending SMS records", result.size()); // Log the count
        return result;
    }


    @Transactional
    @Override
    public List<PendingSmsDto> fetchPendingMessagesBatch(int batchSize) {
        String sql = "CALL SMS.GET_SMS_BATCH(?)";
        List<PendingSmsDto> result = jdbcTemplate.query(sql, new Object[]{batchSize}, new PendingSmsRowMapper());
        log.info("Fetched {} pending SMS records", result.size());
        return result;
    }

    private static class PendingSmsRowMapper implements RowMapper<PendingSmsDto> {
        @Override
        public PendingSmsDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            PendingSmsDto dto = new PendingSmsDto();
            dto.setSrNo(rs.getLong("SR_NO"));
            dto.setDestination(rs.getString("DESTINATION"));
            dto.setMessage(rs.getString("MESSAGE"));
            dto.setTemplateId(rs.getString("TEMPLATE_ID"));
            dto.setSmsType(rs.getString("SMS_TYPE"));
            dto.setStatus(rs.getInt("STATUS"));
            return dto;
        }
    }
}
