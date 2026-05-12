package com.insa.hospital.service;

import com.insa.hospital.dto.ReportRequestDto;
import com.insa.hospital.dto.ReportResponseDto;
import com.insa.hospital.entity.Report;
import com.insa.hospital.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private static final DateTimeFormatter LEGACY_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yy");

    public List<ReportResponseDto> getAllReports(String hospitalId) {
        return reportRepository.findByHospitalId(hospitalId)
                .stream()
                .map(ReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReportResponseDto> getReportsByType(String hospitalId, String type) {
        return reportRepository.findByHospitalIdAndReportType(hospitalId, type)
                .stream()
                .map(ReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public ReportResponseDto getReportById(Long id, String hospitalId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        // Ensure it belongs to the hospital
        if (!report.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized");
        }
        return ReportResponseDto.fromEntity(report);
    }

    public ReportResponseDto createReport(ReportRequestDto dto, String hospitalId) {
        Report report = new Report();
        report.setReportType(dto.getType());
        report.setDescription(dto.getDescription());
        report.setPatient(dto.getPatient());
        report.setDoctor(dto.getDoctor());
        report.setDate(dto.getDate());
        
        // Exact legacy logic: add_date = date('m/d/y')
        report.setAddDate(LocalDate.now().format(LEGACY_DATE_FORMAT));
        report.setHospitalId(hospitalId);

        Report saved = reportRepository.save(report);
        return ReportResponseDto.fromEntity(saved);
    }

    public ReportResponseDto updateReport(Long id, ReportRequestDto dto, String hospitalId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
                
        if (!report.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized");
        }

        report.setReportType(dto.getType());
        report.setDescription(dto.getDescription());
        report.setPatient(dto.getPatient());
        report.setDoctor(dto.getDoctor());
        report.setDate(dto.getDate());
        
        // Preserves existing add_date and hospital_id implicitly
        Report updated = reportRepository.save(report);
        return ReportResponseDto.fromEntity(updated);
    }

    public void deleteReport(Long id, String hospitalId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
                
        if (!report.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        reportRepository.delete(report);
    }
}
