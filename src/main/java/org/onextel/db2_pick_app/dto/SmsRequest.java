package org.onextel.db2_pick_app.dto;

import lombok.*;

import java.util.List;

@Data

@AllArgsConstructor
@NoArgsConstructor
public class SmsRequest {
    private String key;
    private List<SmsDetail> listsms;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SmsDetail {
        private String from;
        private String to;
        private String body;
        private String clientsmsid;
        private String templateid;
    }
}
