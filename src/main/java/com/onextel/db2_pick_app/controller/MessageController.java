package com.onextel.db2_pick_app.controller;

import lombok.AllArgsConstructor;
import com.onextel.db2_pick_app.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class MessageController {

    MessageService messageService;


    @PostMapping("/update-status")
    public ResponseEntity<String> updateStatus(@RequestBody List<String> ids) {
        try {
            messageService.resetMessageStatus(ids);
            return ResponseEntity.ok("Status updated successfully for given ids : "+ids);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating status: " + e.getMessage());
        }
    }
}
