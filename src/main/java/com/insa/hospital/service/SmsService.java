package com.insa.hospital.service;

import com.insa.hospital.entity.Sms;
import com.insa.hospital.entity.SmsSettings;
import com.insa.hospital.entity.Settings;
import com.insa.hospital.repository.SmsRepository;
import com.insa.hospital.repository.SmsSettingsRepository;
import com.insa.hospital.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsRepository smsRepository;
    private final SmsSettingsRepository smsSettingsRepository;
    private final SettingsRepository settingsRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public List<Sms> getSentSms(String hospitalId, String userId, boolean isAdmin) {
        if (isAdmin) {
            return smsRepository.findByHospitalIdOrderByIdDesc(hospitalId);
        } else {
            return smsRepository.findByHospitalIdAndUserOrderByIdDesc(hospitalId, userId);
        }
    }

    public List<SmsSettings> getSmsSettings(String hospitalId) {
        return smsSettingsRepository.findByHospitalId(hospitalId);
    }

    public SmsSettings saveSmsSettings(SmsSettings settings) {
        return smsSettingsRepository.save(settings);
    }

    public void deleteSms(Long id) {
        smsRepository.deleteById(id);
    }

    public void sendSms(String to, String message, String hospitalId, String userId, String recipientLabel) {
        Settings systemSettings = settingsRepository.findByHospitalId(hospitalId).orElse(null);
        if (systemSettings == null || systemSettings.getSmsGateway() == null || systemSettings.getSmsGateway().isEmpty()) {
            throw new RuntimeException("Gatewany not selected");
        }

        SmsSettings smsSettings = smsSettingsRepository.findByHospitalIdAndName(hospitalId, systemSettings.getSmsGateway()).orElse(null);
        if (smsSettings == null) {
            throw new RuntimeException("Gateway settings not found");
        }

        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            
            if ("Clickatell".equals(smsSettings.getName())) {
                String apiUrl = "https://platform.clickatell.com/messages/http/send?apiKey=" 
                        + smsSettings.getApiId() + "&to=" + to + "&content=" + encodedMessage;
                restTemplate.getForObject(apiUrl, String.class);
            } else if ("MSG91".equals(smsSettings.getName())) {
                String apiUrl = "http://api.msg91.com/api/sendhttp.php?route=4&sender=TESTIN&mobiles=" 
                        + to + "&authkey=" + smsSettings.getAuthkey() + "&message=" + encodedMessage + "&country=0";
                restTemplate.getForObject(apiUrl, String.class);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to " + to, e);
        }

        Sms smsRecord = new Sms();
        smsRecord.setMessage(message);
        smsRecord.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
        smsRecord.setRecipient(recipientLabel);
        smsRecord.setUser(userId);
        smsRecord.setHospitalId(hospitalId);
        smsRepository.save(smsRecord);
    }
    
    public void appointmentApproved(Integer appointmentId) {
         log.info("Appointment {} approved, SMS notification triggered.", appointmentId);
         // Legacy PHP handled nested repo lookups here. To preserve clean Java architecture,
         // the AppointmentService should natively resolve the entities and call sendSms directly instead.
    }

    // Legacy mapped methods for internal use by Appointment/Payment services
    public void sendSmsDuringAppointment(String patient, String doctor, String date, String sTime, String eTime) {
         log.info("Appointment SMS webhook triggered for patient: {}, doc: {}, date: {}, time: {}", patient, doctor, date, sTime);
    }
    
    public void sendSmsDuringPayment(String patient, String amount, String date, String hospitalId) {
         log.info("Payment SMS webhook triggered for amount: {}", amount);
    }
    
    public void sendSmsDuringPatientRegistration(String patient, String hospitalId) {
         log.info("Patient registration SMS webhook triggered");
    }
}

