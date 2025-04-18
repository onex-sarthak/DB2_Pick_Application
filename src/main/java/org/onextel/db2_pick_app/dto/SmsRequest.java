package org.onextel.db2_pick_app.dto;

import lombok.*;

import java.util.List;

@Data
public class SmsRequest {
    private String key;
    private List<SmsDetail> listsms;

    @Data
    public static class SmsDetail {
        private String from;
        private String to;
        private String body;
        private String clientsmsid;
        private String templateid;
    }
}
