package org.example.db2_pick_app.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

@Data
@Setter
@Getter
public class SmsRequest {
    private String key;
    private List<SmsDetail> listsms;

    @Data
    public static class SmsDetail {
        private String from;
        private String to;
        private String body;
        private String clientsmsid;
    }
}
