package org.onextel.db2_pick_app.controller;

import lombok.AllArgsConstructor;
import org.onextel.db2_pick_app.dto.DlrCallbackRequest;
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
public class DLRController {

    DlrCallbackService dlrCallbackService;

    @PostMapping("/callback")
    public ResponseEntity<String> callback(@RequestBody List<DlrCallbackRequest> dlrCallbackRequests) {
        dlrCallbackService.processDlrCallbacks(dlrCallbackRequests);
        System.out.println("DLR Callbacks Processed Successfully");
        return ResponseEntity.ok("Status updated successfully for the following dlrCallbackRequests : "+ dlrCallbackRequests.toString());
    }
}
