package com.onextel.db2_pick_app.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.*;
import org.springframework.stereotype.Service;

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
    }
}
