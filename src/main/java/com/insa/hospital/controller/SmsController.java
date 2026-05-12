package com.insa.hospital.controller;

import com.insa.hospital.entity.Sms;
import com.insa.hospital.entity.SmsSettings;
import com.insa.hospital.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    @GetMapping("/sent")
    public ResponseEntity<List<Sms>> getSentSms(@RequestParam String hospitalId, @RequestParam String userId, @RequestParam boolean isAdmin) {
        return ResponseEntity.ok(smsService.getSentSms(hospitalId, userId, isAdmin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSms(@PathVariable Long id) {
        smsService.deleteSms(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<List<SmsSettings>> getSmsSettings(@RequestParam String hospitalId) {
        return ResponseEntity.ok(smsService.getSmsSettings(hospitalId));
    }

    @PostMapping("/settings")
    public ResponseEntity<SmsSettings> saveSettings(@RequestBody SmsSettings settings) {
        return ResponseEntity.ok(smsService.saveSmsSettings(settings));
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendManualSms(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String message = payload.get("message");
        String hospitalId = payload.get("hospitalId");
        String userId = payload.get("userId");
        String recipientLabel = payload.get("recipientLabel");
        
        try {
            smsService.sendSms(to, message, hospitalId, userId, recipientLabel);
            return ResponseEntity.ok("Message Sent");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

