package com.hospital.hms.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {

    public void sendSmsDuringAppointment(String patientId, String doctorId, String date, String sTime, String eTime) {
        // Stub for legacy this->sms->sendSmsDuringAppointment()
        System.out.println("Triggered SMS Notification: New Appointment for Patient " + patientId + " with Doctor " + doctorId + " on " + date);
    }

    public void appointmentApproved(Long appointmentId) {
        // Stub for legacy this->sms->appointmentApproved()
        System.out.println("Triggered SMS Notification: Appointment " + appointmentId + " Approved.");
    }
}
