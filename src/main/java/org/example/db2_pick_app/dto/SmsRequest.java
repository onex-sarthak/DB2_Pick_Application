package org.example.db2_pick_app.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

@Data
@Setter
@Getter
@AllArgsConstructor
public class SmsRequest {
    private String key;
    private List<SmsDetail> listsms;

    @Data
    @AllArgsConstructor
    public static class SmsDetail {
        private String from;
        private String to;
        private String body;
        private String clientsmsid;
    }
}
