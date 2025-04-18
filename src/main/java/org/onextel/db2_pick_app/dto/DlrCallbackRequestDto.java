package org.onextel.db2_pick_app.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class DlrCallbackRequestDto {
    @JsonProperty("CLIENTSMSID") // Map "CLIENTSMSID" from JSON to "srNo"
    private Long srNo;

    @JsonProperty("STATUS") // Map "STATUS" from JSON to "dlrStatus"
    private String dlrStatus;

    @JsonProperty("ERRORCODE") // Map "ERRORCODE" from JSON to "deliveryCode"
    private String deliveryCode;

    @JsonProperty("STATUSDATETIME") // Map "STATUSDATETIME" from JSON to "deliveryTime"
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveryTime;
}
