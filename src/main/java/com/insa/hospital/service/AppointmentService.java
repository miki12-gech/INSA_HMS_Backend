package com.insa.hospital.service;

import com.insa.hospital.dto.AppointmentRequestDto;
import com.insa.hospital.dto.AppointmentResponseDto;
import com.insa.hospital.dto.CalendarEventDto;
import com.insa.hospital.dto.DataTablesResponseDto;
import com.insa.hospital.entity.Appointment;
import com.insa.hospital.entity.Doctor;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.AppointmentRepository;
import com.insa.hospital.repository.DoctorRepository;
import com.insa.hospital.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Appointment Service — Business Logic Layer
 *
 * Key responsibilities:
 *  1. Book, update, and cancel appointments.
 *  2. Set add_date (MM/DD/YY) and registration_time (Unix epoch) at save time.
 *  3. Default status = 'Pending Confirmation' if not supplied.
 *  4. Enrich response DTOs with patientName + doctorName (resolved from DB).
 *  5. Enforce multi-tenant scoping on every query.
 */
@Service
@Transactional
public class AppointmentService {

    private static final DateTimeFormatter ADD_DATE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yy");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    
    @Autowired
    private SmsService smsService;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                               PatientRepository patientRepository,
                               DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // ─── Book Appointment ─────────────────────────────────────────────────────

    /**
     * Books a new appointment.
     * Sets add_date, registration_time, and defaults status = 'Pending Confirmation'.
     */
    public AppointmentResponseDto bookAppointment(AppointmentRequestDto dto,
                                                   String hospitalId,
                                                   String bookedByUserId) {
        String patientIdStr = dto.patient();
        
        // 1. Handle "add_new" cascade
        if ("add_new".equals(patientIdStr)) {
            Patient newPatient = new Patient();
            newPatient.setPatientId(String.valueOf((int) (Math.random() * 990000) + 10000));
            newPatient.setName(dto.p_name());
            String email = dto.p_email();
            if (email == null || email.trim().isEmpty()) {
                email = dto.p_name() + "-" + (int)(Math.random() * 1000) + "@example.com";
            }
            newPatient.setEmail(email);
            newPatient.setPhone(dto.p_phone());
            newPatient.setSex(dto.p_gender());
            newPatient.setAge(dto.p_age());
            newPatient.setAddDate(LocalDate.now().format(ADD_DATE_FMT));
            newPatient.setRegistrationTime(String.valueOf(System.currentTimeMillis() / 1000L));
            newPatient.setHowAdded("from_appointment");
            newPatient.setHospitalId(hospitalId);
            
            newPatient = patientRepository.save(newPatient);
            patientIdStr = String.valueOf(newPatient.getId());
        }

        Appointment appt = new Appointment();
        mapDtoToEntity(dto, appt);
        appt.setPatient(patientIdStr); // override mapped value

        appt.setHospitalId(hospitalId);
        appt.setUser(bookedByUserId);
        appt.setAddDate(LocalDate.now().format(ADD_DATE_FMT));
        appt.setRegistrationTime(String.valueOf(System.currentTimeMillis() / 1000L));

        // Default status mirrors legacy PHP behaviour
        if (!StringUtils.hasText(dto.status())) {
            appt.setStatus("Pending Confirmation");
        }

        Appointment saved = appointmentRepository.save(appt);
        
        // 2. Update patient doctor list
        if (patientIdStr != null && patientIdStr.matches("\\d+")) {
            patientRepository.findById(Integer.parseInt(patientIdStr)).ifPresent(p -> {
                String existingDocs = p.getDoctor();
                if (existingDocs == null || existingDocs.isEmpty()) {
                    p.setDoctor(dto.doctor());
                    patientRepository.save(p);
                } else {
                    List<String> docList = new java.util.ArrayList<>(java.util.Arrays.asList(existingDocs.split(",")));
                    if (!docList.contains(dto.doctor())) {
                        p.setDoctor(existingDocs + "," + dto.doctor());
                        patientRepository.save(p);
                    }
                }
            });
        }
        
        // 3. SMS notification check
        if ("Yes".equalsIgnoreCase(dto.sms()) || "true".equalsIgnoreCase(dto.sms())) {
            if (smsService != null) {
                smsService.sendSmsDuringAppointment(patientIdStr, dto.doctor(), dto.date(), dto.sTime(), dto.eTime());
            }
        }
        
        return enrichAndMap(saved);
    }

    // ─── List / Search ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> listAppointments(String hospitalId, Pageable pageable) {
        return appointmentRepository
                .findByHospitalId(hospitalId, pageable)
                .map(this::enrichAndMap);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> listByDoctor(String doctorId, String hospitalId, Pageable pageable) {
        return appointmentRepository
                .findByDoctorAndHospitalId(doctorId, hospitalId, pageable)
                .map(this::enrichAndMap);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> listByPatient(String patientId, String hospitalId, Pageable pageable) {
        return appointmentRepository
                .findByPatientAndHospitalId(patientId, hospitalId, pageable)
                .map(this::enrichAndMap);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> listByStatus(String status, String hospitalId, Pageable pageable) {
        return appointmentRepository
                .findByStatusAndHospitalId(status, hospitalId, pageable)
                .map(this::enrichAndMap);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listByDoctorAndDate(String doctorId, String date, String hospitalId) {
        return appointmentRepository
                .findByDoctorAndDateAndHospitalId(doctorId, date, hospitalId)
                .stream()
                .map(this::enrichAndMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> search(String query, String hospitalId, Pageable pageable) {
        return appointmentRepository
                .searchAppointments(hospitalId, query, pageable)
                .map(this::enrichAndMap);
    }

    // ─── DataTables Legacy Support ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DataTablesResponseDto listAppointmentsDataTables(String hospitalId, int draw, int start, int length, String search) {
        Pageable pageable;
        if (length == -1) {
            pageable = Pageable.unpaged(); // All records
        } else {
            pageable = PageRequest.of(start / length, length, org.springframework.data.domain.Sort.by("id").descending());
        }
        
        Page<Appointment> page;
        if (StringUtils.hasText(search)) {
            page = appointmentRepository.searchAppointments(hospitalId, search, pageable);
        } else {
            page = appointmentRepository.findByHospitalId(hospitalId, pageable);
        }
        
        List<List<Object>> data = new java.util.ArrayList<>();
        for (Appointment appt : page.getContent()) {
            List<Object> row = new java.util.ArrayList<>();
            row.add(appt.getId());
            
            String pName = "";
            String pPhone = "";
            if (StringUtils.hasText(appt.getPatient())) {
                try {
                    Patient p = patientRepository.findById(Integer.parseInt(appt.getPatient())).orElse(null);
                    if (p != null) {
                        pName = p.getName();
                        pPhone = p.getPhone();
                    }
                } catch (Exception e) {}
            }
            row.add(pName);
            row.add(pPhone);
            
            // Due balance fallback/mock format (assume $0.00 if payment not migrated fully)
            row.add("$0.00"); 
            
            // Legacy options string exact PHP format
            String options1 = " <a type=\"button\" class=\"btn editbutton\" title=\"Edit\" data-toggle=\"modal\" data-id=\"" + appt.getId() + "\"><i class=\"fa fa-edit\"> </i> Edit</a>";
            String options2 = " <a class=\"btn detailsbutton\" title=\"Info\" style=\"color: #fff;\" href=\"appointment/appointmentDetails?id=" + appt.getId() + "\"><i class=\"fa fa-info\"></i> Info</a>";
            String options3 = " <a class=\"btn green\" title=\"History\" style=\"color: #fff;\" href=\"appointment/medicalHistory?id=" + appt.getId() + "\"><i class=\"fa fa-stethoscope\"></i> History</a>";
            String options4 = " <a class=\"btn invoicebutton\" title=\"Payment\" style=\"color: #fff;\" href=\"finance/appointmentPaymentHistory?appointment=" + appt.getId() + "\"><i class=\"fa fa-money\"></i> Payment</a>";
            String options5 = " <a class=\"btn delete_button\" title=\"Delete\" href=\"appointment/delete?id=" + appt.getId() + "\" onclick=\"return confirm('Are you sure you want to delete this item?');\"><i class=\"fa fa-trash-o\"></i> Delete</a>";
            
            row.add(options1 + options2 + options3 + options4 + options5);
            data.add(row);
        }
        
        long totalRecords = appointmentRepository.countByHospitalId(hospitalId);
        long filteredRecords = StringUtils.hasText(search) ? page.getTotalElements() : totalRecords;
        
        return new DataTablesResponseDto(draw, totalRecords, filteredRecords, data);
    }

    // ─── Get Single ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointmentById(Integer id, String hospitalId) {
        Appointment appt = appointmentRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        return enrichAndMap(appt);
    }

    // ─── Update Status ────────────────────────────────────────────────────────

    /**
     * Update an appointment's status (Confirmed / Pending Confirmation / Treated).
     * Legacy flow: receptionist confirms, doctor marks Treated.
     */
    public AppointmentResponseDto updateStatus(Integer id, String status, String hospitalId) {
        Appointment appt = appointmentRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        
        String previousStatus = appt.getStatus();
        appt.setStatus(status);
        
        if (!"Approved".equals(previousStatus) && "Approved".equals(status)) {
            if (smsService != null) {
                smsService.appointmentApproved(id);
            }
        }
        
        return enrichAndMap(appointmentRepository.save(appt));
    }

    /** Full appointment update (for editing remarks, time slot, category, etc.). */
    public AppointmentResponseDto updateAppointment(Integer id,
                                                     AppointmentRequestDto dto,
                                                     String hospitalId) {
        Appointment appt = appointmentRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        mapDtoToEntity(dto, appt);
        return enrichAndMap(appointmentRepository.save(appt));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteAppointment(Integer id, String hospitalId) {
        Appointment appt = appointmentRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        appointmentRepository.delete(appt);
    }

    // ─── Request Workflow ─────────────────────────────────────────────────────

    /**
     * List appointments submitted via the legacy patient-request workflow.
     * These are appointments with a non-empty `request` column.
     */
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> listRequested(String hospitalId, Pageable pageable) {
        return appointmentRepository
                .findRequestedAppointments(hospitalId, pageable)
                .map(this::enrichAndMap);
    }

    /**
     * Confirm a requested appointment — sets status to 'Confirmed'.
     * Mirrors the legacy PHP confirm-request workflow.
     */
    public AppointmentResponseDto confirmAppointment(Integer id, String hospitalId) {
        Appointment appt = appointmentRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        appt.setStatus("Confirmed");
        return enrichAndMap(appointmentRepository.save(appt));
    }

    /**
     * Decline a requested appointment — sets status to 'Declined'.
     * This reflects a rejected patient-initiated appointment request.
     */
    public AppointmentResponseDto declineAppointment(Integer id, String hospitalId) {
        Appointment appt = appointmentRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        appt.setStatus("Declined");
        return enrichAndMap(appointmentRepository.save(appt));
    }

    // ─── Calendar Endpoints ───────────────────────────────────────────────────

    private long getTimeSecondsLegacy(String timeString) {
        try {
            String[] parts = timeString.split(" ");
            String[] hms = parts[0].split(":");
            long h = Long.parseLong(hms[0]);
            long m = hms.length > 1 ? Long.parseLong(hms[1]) : 0;
            if ("AM".equalsIgnoreCase(parts[1])) {
                return h * 3600 + m * 60;
            } else {
                return 12 * 3600 + h * 3600 + m * 60;
            }
        } catch (Exception e) {
            return 0; // fallback
        }
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDto> getAppointmentsForCalendar(String hospitalId, String role, String userId) {
        List<Appointment> appointments;
        
        if ("Doctor".equalsIgnoreCase(role)) {
            Doctor doc = doctorRepository.findByIonUserIdAndHospitalId(userId, hospitalId).orElse(null);
            if(doc != null) {
                appointments = appointmentRepository.findByDoctorAndHospitalId(String.valueOf(doc.getId()), hospitalId);
            } else {
                appointments = List.of();
            }
        } else if ("Patient".equalsIgnoreCase(role)) {
            Patient pat = patientRepository.findByIonUserId(userId).orElse(null);
            if(pat != null && hospitalId.equals(pat.getHospitalId())) {
                appointments = appointmentRepository.findByPatientAndHospitalId(String.valueOf(pat.getId()), hospitalId);
            } else {
                appointments = List.of();
            }
        } else {
            appointments = appointmentRepository.findByHospitalIdOrderByIdAsc(hospitalId);
        }

        return appointments.stream().map(appt -> {
            CalendarEventDto dto = new CalendarEventDto();
            dto.setId(Long.valueOf(appt.getId()));
            
            String doctorName = "";
            if (StringUtils.hasText(appt.getDoctor())) {
                try {
                    Doctor doctor = doctorRepository.findById(Integer.parseInt(appt.getDoctor())).orElse(null);
                    if (doctor != null) {
                        doctorName = doctor.getName();
                    }
                } catch (Exception e) {}
            }
            
            String patientName = "";
            String patientMobile = "";
            if (StringUtils.hasText(appt.getPatient())) {
                try {
                    Patient patient = patientRepository.findById(Integer.parseInt(appt.getPatient())).orElse(null);
                    if (patient != null) {
                        patientName = patient.getName();
                        patientMobile = patient.getPhone();
                    }
                } catch (Exception e) {}
            }
            
            String timeSlot = appt.getTimeSlot();
            long dayStartSeconds = 0;
            long dayEndSeconds = 0;
            if (timeSlot != null && timeSlot.contains(" To ")) {
                String[] timeSlotNew = timeSlot.split(" To ");
                dayStartSeconds = getTimeSecondsLegacy(timeSlotNew[0]);
                if (timeSlotNew.length > 1) {
                    dayEndSeconds = getTimeSecondsLegacy(timeSlotNew[1]);
                }
            }
            
            long dateUnix = 0;
            try {
                dateUnix = Long.parseLong(appt.getDate());
            } catch (Exception e) {}
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            java.time.LocalDateTime startLdt = java.time.LocalDateTime.ofEpochSecond(dateUnix + dayStartSeconds, 0, java.time.ZoneOffset.UTC);
            java.time.LocalDateTime endLdt = java.time.LocalDateTime.ofEpochSecond(dateUnix + dayEndSeconds, 0, java.time.ZoneOffset.UTC);
            dto.setStart(startLdt.format(formatter));
            dto.setEnd(endLdt.format(formatter));
            
            String info = "<br/>Status: " + appt.getStatus() + 
                          "<br>Patient: " + patientName + 
                          "<br/>Phone: " + patientMobile + 
                          "<br/> Doctor: " + doctorName + 
                          "<br/>Remarks: " + appt.getRemarks();
            dto.setTitle(info);
            
            String color = "gray"; // default
            if ("Pending Confirmation".equalsIgnoreCase(appt.getStatus())) color = "yellowgreen";
            if ("Confirmed".equalsIgnoreCase(appt.getStatus())) color = "#009988";
            if ("Treated".equalsIgnoreCase(appt.getStatus())) color = "#112233";
            if ("Cancelled".equalsIgnoreCase(appt.getStatus())) color = "red";
            dto.setColor(color);
            
            return dto;
        }).toList();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void mapDtoToEntity(AppointmentRequestDto dto, Appointment appt) {
        appt.setPatient(dto.patient());
        appt.setDoctor(dto.doctor());
        appt.setDate(dto.date());
        appt.setTimeSlot(dto.timeSlot() != null ? dto.timeSlot() : "Not Selected");
        appt.setSTime(dto.sTime() != null ? dto.sTime() : "Not Selected");
        appt.setETime(dto.eTime() != null ? dto.eTime() : "");
        appt.setSTimeKey(dto.sTimeKey() != null ? dto.sTimeKey() : "0");
        appt.setRemarks(dto.remarks() != null ? dto.remarks() : "");
        appt.setStatus(StringUtils.hasText(dto.status()) ? dto.status() : "Pending Confirmation");
        appt.setRequest(dto.request() != null ? dto.request() : "");
        appt.setCategory(dto.category() != null ? dto.category() : "Out Patient");
    }

    /**
     * Enriches an Appointment entity with patient name and doctor name from DB,
     * then maps to AppointmentResponseDto.
     * Falls back to null if the patient/doctor record doesn't exist (defensive).
     */
    private AppointmentResponseDto enrichAndMap(Appointment appt) {
        String patientName = null;
        String doctorName = null;
        String doctorDepartment = null;

        if (StringUtils.hasText(appt.getPatient())) {
            try {
                patientName = patientRepository.findById(Integer.parseInt(appt.getPatient()))
                        .map(Patient::getName)
                        .orElse(null);
            } catch (NumberFormatException ignored) {}
        }

        if (StringUtils.hasText(appt.getDoctor())) {
            try {
                Doctor doc = doctorRepository.findById(Integer.parseInt(appt.getDoctor()))
                        .orElse(null);
                if (doc != null) {
                    doctorName = doc.getName();
                    doctorDepartment = doc.getDepartment();
                }
            } catch (NumberFormatException ignored) {}
        }

        return AppointmentResponseDto.from(appt, patientName, doctorName, doctorDepartment);
    }
}
