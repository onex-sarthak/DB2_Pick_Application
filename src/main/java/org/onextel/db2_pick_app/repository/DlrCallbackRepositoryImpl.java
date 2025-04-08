package org.onextel.db2_pick_app.repository;

import lombok.RequiredArgsConstructor;
import org.onextel.db2_pick_app.dto.DlrCallbackRequestDto;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Repository
@RequiredArgsConstructor
public class DlrCallbackRepositoryImpl implements DlrCallbackRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void updateBatch(List<DlrCallbackRequestDto> batch) throws SQLException {
        String sql = "UPDATE SMS.SMS_TEMP_OUT_LOG_DLR SET DELIVERY_STATUS = ?, DELIVERY_CODE = ?, DELIVERY_TIME = ? WHERE SR_NO = ?";
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            for (DlrCallbackRequestDto dto : batch) {
                ps.setString(1, dto.getDlrStatus());
                ps.setString(2, dto.getDeliveryCode());
                ps.setTimestamp(3, Timestamp.valueOf(dto.getDeliveryTime()));
                ps.setLong(4, dto.getSrNo());
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
        }
    }


}
