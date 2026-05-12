package com.insa.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.insa.hospital.entity.Report;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {
    private Long id;
    private String report_type;
    private String description;
    private String patient;
    private String doctor;
    private String date;
    private String add_date;
    private String hospital_id;

    public static ReportResponseDto fromEntity(Report report) {
        if (report == null) return null;
        return new ReportResponseDto(
                report.getId(),
                report.getReportType(),
                report.getDescription(),
                report.getPatient(),
                report.getDoctor(),
                report.getDate(),
                report.getAddDate(),
                report.getHospitalId()
        );
    }
}
