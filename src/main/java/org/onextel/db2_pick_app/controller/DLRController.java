package org.onextel.db2_pick_app.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.DlrCallbackRequestDto;
import org.onextel.db2_pick_app.service.DlrCallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dlr")
@AllArgsConstructor
@Slf4j
public class DLRController {

    DlrCallbackService dlrCallbackService;

    @PostMapping("/callback")
    public ResponseEntity<String> callback(@RequestBody DlrCallbackRequestDto dlrCallbackRequest) {
        dlrCallbackService.addToBatch(dlrCallbackRequest);
        log.info("DLR Callbacks received Successfully : {}", dlrCallbackRequest.toString());
        return ResponseEntity.ok("Status updated successfully for the  dlrCallbackRequest : "+ dlrCallbackRequest.toString());
    }
}
