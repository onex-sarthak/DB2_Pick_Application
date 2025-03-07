package com.onextel.db2_pick_app.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class SmsResponse {
    private SmsList smslist;

    @Data
    public static class SmsList {
        private List<SmsDetail> sms;

        @Data
        public static class SmsDetail {
            private String reason;
            private int code;
            private String messageid;
            private List<String> messagepartids;
            private String mobileno;
            private String clientsmsid;
            private String status;
        }
    }
}