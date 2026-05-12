package com.hospital.hms.service;

import com.hospital.hms.dto.AppointmentDto;
import com.hospital.hms.entity.Appointment;
import com.hospital.hms.entity.Patient;
import com.hospital.hms.repository.AppointmentRepository;
import com.hospital.hms.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final SmsService smsService;

    private static final String[] ALL_SLOT;

    static {
        ALL_SLOT = new String[288];
        ALL_SLOT[0] = "12:00 PM";
        for (int i = 1; i <= 143; i++) {
            ALL_SLOT[i] = formatTimeLegacy(i * 5, "AM");
            if (i == 8) ALL_SLOT[i] = "12:40 PM"; // Preserve legacy typo
        }
        ALL_SLOT[144] = "12:00 AM";
        for (int i = 145; i < 288; i++) {
            ALL_SLOT[i] = formatTimeLegacy((i - 144) * 5, "PM");
        }
    }

    private static String formatTimeLegacy(int totalMinutes, String amPm) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours == 0) hours = 12;
        return String.format("%02d:%02d %s", hours, mins, amPm);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByIdDesc();
    }

    public List<Appointment> getAppointmentsByDoctor(String doctor) {
        return appointmentRepository.findByDoctorOrderByIdDesc(doctor);
    }

    public List<Appointment> getAppointmentsByStatus(String status) {
        return appointmentRepository.findByStatusOrderByIdDesc(status);
    }

    public List<Appointment> getRequestedAppointments() {
        return appointmentRepository.findByRequestOrderByIdDesc("Yes");
    }

    @Transactional
    public void addAppointment(AppointmentDto dto) throws Exception {
        String patientIdStr = dto.getPatient();
        
        // Handling the 'add_new' cascade explicitly replicating the legacy PHP flow
        if ("add_new".equals(patientIdStr)) {
            Patient newPatient = new Patient();
            newPatient.setPatientId(String.valueOf((int) (Math.random() * 990000) + 10000));
            newPatient.setName(dto.getPName());
            
            String email = dto.getPEmail();
            if (email == null || email.trim().isEmpty()) {
                email = dto.getPName() + "-" + (int)(Math.random() * 1000) + "@example.com";
            }
            newPatient.setEmail(email);
            newPatient.setPhone(dto.getPPhone());
            newPatient.setSex(dto.getPGender());
            newPatient.setAge(dto.getPAge());
            newPatient.setAddDate(new SimpleDateFormat("MM/dd/yy").format(new Date()));
            newPatient.setRegistrationTime(String.valueOf(System.currentTimeMillis() / 1000));
            newPatient.setHowAdded("from_appointment");
            
            // Note: skipping IonAuth password logic natively; handled by JWT architecture in standard accounts.
            newPatient = patientRepository.save(newPatient);
            patientIdStr = String.valueOf(newPatient.getId());
        }

        Appointment appointment;
        String addDate;
        String registrationTime;

        if (dto.getId() == null) {
            addDate = new SimpleDateFormat("MM/dd/yy").format(new Date());
            registrationTime = String.valueOf(System.currentTimeMillis() / 1000);
            appointment = new Appointment();
        } else {
            appointment = appointmentRepository.findById(dto.getId())
                    .orElseThrow(() -> new Exception("Appointment not found"));
            addDate = appointment.getAddDate();
            registrationTime = appointment.getRegistrationTime();
            
            // Status check for SMS
            if (!"Approved".equals(appointment.getStatus()) && "Approved".equals(dto.getStatus())) {
                smsService.appointmentApproved(appointment.getId());
            }
        }

        // Calculate sTimeKey
        int sTimeKeyIndex = getSlotIndex(dto.getSTime());
        String sTimeKey = sTimeKeyIndex != -1 ? String.valueOf(sTimeKeyIndex) : null;

        // Unix timestamp formatting if a specific date string was passed.
        String dateVal = dto.getDate();
        if (dateVal != null && !dateVal.matches("\\d+")) { 
            // e.g. dd-mm-yyyy or yyyy-mm-dd passed, we attempt casting to unix timestamp
            try {
               Date parsed = new SimpleDateFormat("yyyy-MM-dd").parse(dateVal);
               dateVal = String.valueOf(parsed.getTime() / 1000);
            } catch (Exception e) {}
        }

        appointment.setPatient(patientIdStr);
        appointment.setDoctor(dto.getDoctor());
        appointment.setDate(dateVal);
        appointment.setSTime(dto.getSTime());
        appointment.setETime(dto.getETime());
        appointment.setTimeSlot(dto.getTimeSlot());
        appointment.setRemarks(dto.getRemarks());
        appointment.setAddDate(addDate);
        appointment.setRegistrationTime(registrationTime);
        appointment.setStatus(dto.getStatus());
        appointment.setSTimeKey(sTimeKey);
        appointment.setUser(dto.getUser());
        appointment.setCategory(dto.getPatientCategory());
        appointment.setRequest(dto.getRequest() != null && !dto.getRequest().isEmpty() ? dto.getRequest() : "");

        appointment = appointmentRepository.save(appointment);

        if (dto.getId() == null) {
            if ("Yes".equalsIgnoreCase(dto.getSms()) || "true".equalsIgnoreCase(dto.getSms())) {
                smsService.sendSmsDuringAppointment(patientIdStr, dto.getDoctor(), dateVal, dto.getSTime(), dto.getETime());
            }
            
            // Patient-Doctor association update
            if (patientIdStr != null && patientIdStr.matches("\\d+")) {
                Patient p = patientRepository.findById(Long.parseLong(patientIdStr)).orElse(null);
                if (p != null) {
                    String existingDocs = p.getDoctor();
                    if (existingDocs == null || existingDocs.isEmpty()) {
                        p.setDoctor(dto.getDoctor());
                        patientRepository.save(p);
                    } else {
                        List<String> docList = Arrays.asList(existingDocs.split(","));
                        if (!docList.contains(dto.getDoctor())) {
                            p.setDoctor(existingDocs + "," + dto.getDoctor());
                            patientRepository.save(p);
                        }
                    }
                }
            }
        }
    }

    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }

    private int getSlotIndex(String time) {
        if (time == null) return -1;
        for (int i = 0; i < ALL_SLOT.length; i++) {
            if (ALL_SLOT[i].equals(time)) {
                return i;
            }
        }
        return -1;
    }
}
