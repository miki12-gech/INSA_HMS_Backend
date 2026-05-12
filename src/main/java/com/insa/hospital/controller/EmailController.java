package com.insa.hospital.controller;

import com.insa.hospital.entity.Email;
import com.insa.hospital.entity.EmailSettings;
import com.insa.hospital.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @GetMapping("/sent")
    public ResponseEntity<List<Email>> getSentEmails(@RequestParam String hospitalId, @RequestParam String userId, @RequestParam boolean isAdmin) {
        return ResponseEntity.ok(emailService.getSentEmails(hospitalId, userId, isAdmin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmail(@PathVariable Long id) {
        emailService.deleteEmail(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<EmailSettings> getEmailSettings(@RequestParam String hospitalId) {
        return ResponseEntity.ok(emailService.getEmailSettings(hospitalId));
    }

    @PostMapping("/settings")
    public ResponseEntity<EmailSettings> saveSettings(@RequestBody EmailSettings settings) {
        return ResponseEntity.ok(emailService.saveEmailSettings(settings));
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendManualEmail(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String subject = payload.get("subject");
        String message = payload.get("message");
        String hospitalId = payload.get("hospitalId");
        String userId = payload.get("userId");
        String recipientLabel = payload.get("recipientLabel");
        
        boolean success = emailService.sendEmail(to, subject, message, hospitalId, userId, recipientLabel);
        if (success) {
            return ResponseEntity.ok("Message Sent");
        } else {
            return ResponseEntity.badRequest().body("Email Failed");
        }
    }
}

