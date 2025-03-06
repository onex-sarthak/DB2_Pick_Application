package org.example.db2_pick_app.controller;

import org.example.db2_pick_app.dto.DlrCallbackRequest;
import org.example.db2_pick_app.model.MessageInfo;
import org.example.db2_pick_app.service.DlrCallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dlr")
public class DLRController {
    DlrCallbackService dlrCallbackService;

    @Autowired
    public DLRController(DlrCallbackService dlrCallbackService) {
        this.dlrCallbackService = dlrCallbackService;
    }

    @PostMapping("/callback")
    public ResponseEntity<String> callback(@RequestBody List<DlrCallbackRequest> dlrCallbackRequests) {
        dlrCallbackService.processDlrCallbacks(dlrCallbackRequests);
        System.out.println("DLR Callbacks Processed Successfully");
        return ResponseEntity.ok("Status updated successfully for the following dlrCallbackRequests : "+ dlrCallbackRequests.toString());
    }


}
