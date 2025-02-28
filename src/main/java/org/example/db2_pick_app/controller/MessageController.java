package org.example.db2_pick_app.controller;

import org.example.db2_pick_app.repository.MessageRepository;
import org.example.db2_pick_app.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Controller
public class MessageController {

    MessageService messageService;

    @Autowired
    MessageController(MessageService messageService) {
        this.messageService =  messageService;
    }

    @PostMapping("/update-status")
    public ResponseEntity<String> updateStatus(@RequestBody List<String> ids) {
        try {
            messageService.updateStatusFlagsToZero(ids);
            return ResponseEntity.ok("Status updated successfully for given IDs.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating status: " + e.getMessage());
        }
    }
}
