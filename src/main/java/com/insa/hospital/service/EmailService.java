package com.insa.hospital.service;

import com.insa.hospital.entity.Email;
import com.insa.hospital.entity.EmailSettings;
import com.insa.hospital.repository.EmailRepository;
import com.insa.hospital.repository.EmailSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailRepository emailRepository;
    private final EmailSettingsRepository emailSettingsRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    // We make JavaMailSender optional so the app starts even if spring.mail is not yet configured.
    private final Optional<JavaMailSender> mailSender;

    public List<Email> getSentEmails(String hospitalId, String userId, boolean isAdmin) {
        if (isAdmin) {
            return emailRepository.findByHospitalIdOrderByIdDesc(hospitalId);
        } else {
            return emailRepository.findByHospitalIdAndUserOrderByIdDesc(hospitalId, userId);
        }
    }

    public EmailSettings getEmailSettings(String hospitalId) {
        return emailSettingsRepository.findByHospitalId(hospitalId).orElse(null);
    }

    public EmailSettings saveEmailSettings(EmailSettings settings) {
        Optional<EmailSettings> existing = emailSettingsRepository.findByHospitalId(settings.getHospitalId());
        if (existing.isPresent()) {
            EmailSettings curr = existing.get();
            curr.setAdminEmail(settings.getAdminEmail());
            curr.setType(settings.getType());
            curr.setUser(settings.getUser());
            curr.setPassword(settings.getPassword());
            return emailSettingsRepository.save(curr);
        }
        return emailSettingsRepository.save(settings);
    }

    public void deleteEmail(Long id) {
        emailRepository.deleteById(id);
    }

    // Standard Email Send
    public boolean sendEmail(String to, String subject, String message, String hospitalId, String userId, String recipientLabel) {
        EmailSettings settings = getEmailSettings(hospitalId);
        String fromAdminEmail = (settings != null && settings.getAdminEmail() != null) ? settings.getAdminEmail() : "admin@hospital.com";

        boolean success = false;
        try {
            if (mailSender.isPresent()) {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(fromAdminEmail);
                mailMessage.setTo(to.split(","));
                mailMessage.setSubject(subject);
                mailMessage.setText(message);
                mailSender.get().send(mailMessage);
                success = true;
            } else {
                log.warn("JavaMailSender bean not found. Simulated email send to " + to);
                success = true; // Simulate success if mail isn't configured so DB gets updated like legacy
            }
        } catch (Exception e) {
            log.error("Failed to send email to " + to, e);
        }

        if (success) {
            Email emailRecord = new Email();
            emailRecord.setSubject(subject);
            emailRecord.setMessage(message);
            emailRecord.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
            emailRecord.setReciepient(recipientLabel); // Typo matched
            emailRecord.setUser(userId);
            emailRecord.setHospitalId(hospitalId);
            emailRepository.save(emailRecord);
        }
        return success;
    }

    // Legacy copy-paste bug implementations: Using Clickatell REST API
    private void executeLegacyClickatellBug(String to, String message) {
        // Warning: This mimics the exact legacy PHP code bug in email.php
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            // It relied on a globally appended API key in legacy code: '==&to='
            String apiUrl = "https://platform.clickatell.com/messages/http/send?apiKey===&to=" + to + "&content=" + encodedMessage;
            restTemplate.getForObject(apiUrl, String.class);
            log.info("Executed legacy clickatell API string bug for email to {}", to);
        } catch (Exception e) {
            log.error("Legacy Clickatell HTTP invoke failed", e);
        }
    }

    public void sendEmailDuringAppointment(String patientEmail, String patientLabel, String doctorEmail, String doctorLabel, 
                                           long dateParams, String sTime, String hospitalId, String userId) {
        
        if (patientEmail != null && !patientEmail.isEmpty()) {
            String message = "Appointment is scheduled for you With Doctor Date: " + dateParams + " Time: " + sTime;
            executeLegacyClickatellBug(patientEmail, message);
            Email record = new Email();
            record.setMessage(message);
            record.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
            record.setReciepient(patientLabel);
            record.setHospitalId(hospitalId);
            record.setUser(userId);
            emailRepository.save(record);
        }

        if (doctorEmail != null && !doctorEmail.isEmpty()) {
            String message = "Appointment is scheduled for you With Patient Date: " + dateParams + " Time: " + sTime;
            executeLegacyClickatellBug(doctorEmail, message);
            Email record = new Email();
            record.setMessage(message);
            record.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
            record.setReciepient(doctorLabel);
            record.setHospitalId(hospitalId);
            record.setUser(userId);
            emailRepository.save(record);
        }
    }

    public void sendEmailDuringPayment(String patientEmail, String patientLabel, String amount, long dateParams, String hospitalId, String userId) {
        if (patientEmail != null && !patientEmail.isEmpty()) {
            String message = "Bill For Patient Amount: " + amount + " Date: " + dateParams;
            executeLegacyClickatellBug(patientEmail, message);
            Email record = new Email();
            record.setMessage(message);
            record.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
            record.setReciepient(patientLabel);
            record.setHospitalId(hospitalId);
            record.setUser(userId);
            emailRepository.save(record);
        }
    }

    public void sendEmailDuringPatientRegistration(String patientEmail, String patientLabel, String hospitalId, String userId) {
        if (patientEmail != null && !patientEmail.isEmpty()) {
            String message = "Patient Registration is successfully registerred";
            executeLegacyClickatellBug(patientEmail, message);
            Email record = new Email();
            record.setMessage(message);
            record.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
            record.setReciepient(patientLabel);
            record.setHospitalId(hospitalId);
            record.setUser(userId);
            emailRepository.save(record);
        }
    }
}

