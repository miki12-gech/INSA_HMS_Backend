package com.insa.hospital.service;

import com.insa.hospital.entity.MedicalReport;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.MedicalReportRepository;
import com.insa.hospital.security.JwtContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Medical Report Service — Business Logic Layer
 *
 * Manages CRUD operations for the legacy `report` table.
 * Supports filtering by report_type, patient, doctor.
 * Sets add_date (MM/DD/YY) and date (Unix epoch) at creation time.
 */
@Service
@Transactional
public class MedicalReportService {

    private static final DateTimeFormatter ADD_DATE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yy");

    private final MedicalReportRepository reportRepository;

    @Autowired
    public MedicalReportService(MedicalReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public MedicalReport create(MedicalReport report, String hospitalId) {
        report.setHospitalId(hospitalId);
        // Set date/add_date if not already set
        if (!StringUtils.hasText(report.getAddDate())) {
            report.setAddDate(LocalDate.now().format(ADD_DATE_FMT));
        }
        if (!StringUtils.hasText(report.getDate())) {
            report.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
        }
        return reportRepository.save(report);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<MedicalReport> listAll(String hospitalId, Pageable pageable) {
        return reportRepository.findByHospitalId(hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicalReport> listByType(String reportType, String hospitalId, Pageable pageable) {
        return reportRepository.findByReportTypeAndHospitalId(reportType, hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicalReport> listByPatient(String patient, String hospitalId, Pageable pageable) {
        return reportRepository.findByPatientAndHospitalId(patient, hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicalReport> listByDoctor(String doctor, String hospitalId, Pageable pageable) {
        return reportRepository.findByDoctorAndHospitalId(doctor, hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public MedicalReport getById(Integer id, String hospitalId) {
        return reportRepository.findById(id)
                .filter(r -> hospitalId.equals(r.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("MedicalReport", "id", id));
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public MedicalReport update(Integer id, MedicalReport updated, String hospitalId) {
        MedicalReport existing = getById(id, hospitalId);
        existing.setReportType(updated.getReportType());
        existing.setPatient(updated.getPatient());
        existing.setDoctor(updated.getDoctor());
        existing.setDescription(updated.getDescription());
        existing.setDate(updated.getDate());
        return reportRepository.save(existing);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void delete(Integer id, String hospitalId) {
        MedicalReport existing = getById(id, hospitalId);
        reportRepository.delete(existing);
    }
}
