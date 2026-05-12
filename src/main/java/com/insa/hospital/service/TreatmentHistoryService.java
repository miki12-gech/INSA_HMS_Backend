package com.insa.hospital.service;

import com.insa.hospital.dto.MedicalHistoryRequestDto;
import com.insa.hospital.dto.MedicalHistoryResponseDto;
import com.insa.hospital.dto.PatientNoteRequestDto;
import com.insa.hospital.dto.PatientNoteResponseDto;
import com.insa.hospital.entity.MedicalHistory;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.PatientNote;
import com.insa.hospital.entity.Doctor;
import com.insa.hospital.repository.MedicalHistoryRepository;
import com.insa.hospital.repository.PatientNoteRepository;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.DoctorRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TreatmentHistoryService {

    private final MedicalHistoryRepository medicalHistoryRepository;
    private final PatientNoteRepository patientNoteRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    @Autowired
    public TreatmentHistoryService(
            MedicalHistoryRepository medicalHistoryRepository,
            PatientNoteRepository patientNoteRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository) {
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.patientNoteRepository = patientNoteRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // ─── MEDICAL HISTORY (CASE HISTORY) ──────────────────────────────────────

    public MedicalHistoryResponseDto addMedicalHistory(MedicalHistoryRequestDto dto, String hospitalId) {
        MedicalHistory history = new MedicalHistory();
        BeanUtils.copyProperties(dto, history);
        history.setHospitalId(hospitalId);
        
        String currentTime = String.valueOf(Instant.now().getEpochSecond());
        history.setDate(dto.getDate() != null && !dto.getDate().isEmpty() ? dto.getDate() : currentTime);
        history.setRegistrationTime(currentTime);

        // Fetch patient details
        Patient patient = patientRepository.findById(Integer.parseInt(dto.getPatientId())).orElse(null);
        if (patient != null && patient.getHospitalId().equals(hospitalId)) {
            history.setPatientName(patient.getName());
            history.setPatientPhone(patient.getPhone());
            history.setPatientAddress(patient.getAddress());
        } else {
            history.setPatientName("0");
            history.setPatientPhone("0");
            history.setPatientAddress("0");
        }

        medicalHistoryRepository.save(history);
        return mapToMedicalHistoryResponse(history);
    }

    public MedicalHistoryResponseDto updateMedicalHistory(Integer id, MedicalHistoryRequestDto dto, String hospitalId) {
        MedicalHistory history = medicalHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medical History not found"));
        
        if (!history.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized");
        }

        BeanUtils.copyProperties(dto, history, "patientId");
        
        if (dto.getDate() != null && !dto.getDate().isEmpty()) {
            history.setDate(dto.getDate());
        }

        medicalHistoryRepository.save(history);
        return mapToMedicalHistoryResponse(history);
    }

    public void deleteMedicalHistory(Integer id, String hospitalId) {
        MedicalHistory history = medicalHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medical History not found"));
        if (!history.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized");
        }
        medicalHistoryRepository.delete(history);
    }

    public List<MedicalHistoryResponseDto> getMedicalHistoriesByPatientId(String patientId, String hospitalId) {
        return medicalHistoryRepository.findByPatientIdAndHospitalId(patientId, hospitalId)
                .stream().map(this::mapToMedicalHistoryResponse).collect(Collectors.toList());
    }

    // ─── PATIENT NOTES ───────────────────────────────────────────────────────

    public PatientNoteResponseDto addPatientNote(PatientNoteRequestDto dto, String hospitalId, String doctorIonUserId) {
        PatientNote note = new PatientNote();
        BeanUtils.copyProperties(dto, note);
        note.setHospitalId(hospitalId);

        String currentTime = String.valueOf(Instant.now().getEpochSecond());
        String finalDate = (dto.getDate() != null && !dto.getDate().isEmpty()) ? dto.getDate() : currentTime;
        note.setDate(finalDate);
        note.setRegistrationTime(currentTime);

        // Date string formatters replication
        long unixTime = Long.parseLong(finalDate);
        Instant instant = Instant.ofEpochSecond(unixTime);
        DateTimeFormatter dateStrFormatter = DateTimeFormatter.ofPattern("dd-MM-yy").withZone(ZoneId.systemDefault());
        DateTimeFormatter datetimeStrFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        
        note.setDateString(dateStrFormatter.format(instant));
        note.setDatetimeString(datetimeStrFormatter.format(instant));

        // Set doctor name and ID
        Doctor doctor = doctorRepository.findByIonUserId(doctorIonUserId).stream().findFirst().orElse(null);
        if (doctor != null) {
            note.setDoctorName(doctor.getName());
            note.setDoctorId(doctorIonUserId);
        } else {
            note.setDoctorName("");
            note.setDoctorId(doctorIonUserId);
        }

        // Set patient name
        Patient patient = patientRepository.findById(Integer.parseInt(dto.getPatientId())).orElse(null);
        if (patient != null && patient.getHospitalId().equals(hospitalId)) {
            note.setPatientName(patient.getName());
        } else {
            note.setPatientName("0");
        }

        // Order sheet check
        String template1 = dto.getTemplate1();
        if ("21".equals(template1)) {
            note.setStatus("1"); // 1 == orderSheet for legacy
        } else {
            note.setStatus("0");
        }

        patientNoteRepository.save(note);
        return mapToPatientNoteResponse(note);
    }

    public PatientNoteResponseDto updatePatientNote(Integer id, PatientNoteRequestDto dto, String hospitalId) {
        PatientNote note = patientNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient Note not found"));

        if (!note.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized");
        }

        BeanUtils.copyProperties(dto, note, "patientId");
        
        if (dto.getDate() != null && !dto.getDate().isEmpty()) {
            note.setDate(dto.getDate());
            
            long unixTime = Long.parseLong(dto.getDate());
            Instant instant = Instant.ofEpochSecond(unixTime);
            DateTimeFormatter dateStrFormatter = DateTimeFormatter.ofPattern("dd-MM-yy").withZone(ZoneId.systemDefault());
            DateTimeFormatter datetimeStrFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
            
            note.setDateString(dateStrFormatter.format(instant));
            note.setDatetimeString(datetimeStrFormatter.format(instant));
        }

        String template1 = dto.getTemplate1();
        if ("21".equals(template1)) {
            note.setStatus("1");
        } else {
            note.setStatus("0");
        }

        patientNoteRepository.save(note);
        return mapToPatientNoteResponse(note);
    }

    public void deletePatientNote(Integer id, String hospitalId) {
        PatientNote note = patientNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient Note not found"));
        if (!note.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized");
        }
        patientNoteRepository.delete(note);
    }

    public List<PatientNoteResponseDto> getPatientNotesByPatientId(String patientId, String hospitalId) {
        return patientNoteRepository.findByPatientIdAndHospitalId(patientId, hospitalId)
                .stream().map(this::mapToPatientNoteResponse).collect(Collectors.toList());
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private MedicalHistoryResponseDto mapToMedicalHistoryResponse(MedicalHistory history) {
        MedicalHistoryResponseDto dto = new MedicalHistoryResponseDto();
        BeanUtils.copyProperties(history, dto);
        return dto;
    }

    private PatientNoteResponseDto mapToPatientNoteResponse(PatientNote note) {
        PatientNoteResponseDto dto = new PatientNoteResponseDto();
        BeanUtils.copyProperties(note, dto);
        return dto;
    }
}
