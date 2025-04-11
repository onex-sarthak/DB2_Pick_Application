package org.onextel.db2_pick_app.transformer;

import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.onextel.db2_pick_app.dto.SmsRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
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

    public static List<SmsRequest.SmsDetail> createSmsDetails(List<PendingSmsDto> messages, String smsSenderId) {
        if (messages == null ||  messages.isEmpty()) {
            return new ArrayList<>();
        }
        return messages.stream()
            .map(message -> new SmsRequest.SmsDetail(
                    smsSenderId,
                    message.getDestination(),
                    message.getMessage(),
                    String.valueOf(message.getSrNo()),
                    message.getTemplateId()
            ))
            .toList();
    }

}
