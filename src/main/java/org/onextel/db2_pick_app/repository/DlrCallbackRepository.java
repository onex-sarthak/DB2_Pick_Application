package org.onextel.db2_pick_app.repository;

import org.onextel.db2_pick_app.dto.DlrCallbackRequestDto;

import java.sql.SQLException;
import java.util.List;

public interface DlrCallbackRepository {
    public void updateBatch(List<DlrCallbackRequestDto> batch) throws SQLException;
}
