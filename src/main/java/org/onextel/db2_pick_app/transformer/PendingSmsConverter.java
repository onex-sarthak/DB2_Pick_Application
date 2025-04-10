package org.onextel.db2_pick_app.transformer;

import org.onextel.db2_pick_app.dto.PendingSmsDto;

import java.util.List;
import java.util.stream.Collectors;

public class PendingSmsConverter {
    /**
     * Converts a list of PendingSmsDto to a comma-separated string of srNo values.
     *
     * @param pendingSmsList the list of PendingSmsDto
     * @return a comma-separated string of srNo values
     */
    public static String convertToCommaSeparatedSrNos(List<PendingSmsDto> pendingSmsList) {
        if (pendingSmsList == null || pendingSmsList.isEmpty()) {
            return "";
        }

        return pendingSmsList.stream()
                .map(dto -> String.valueOf(dto.getSrNo())) // Extract srNo as String
                .collect(Collectors.joining(",")); // Join with commas
    }
}
