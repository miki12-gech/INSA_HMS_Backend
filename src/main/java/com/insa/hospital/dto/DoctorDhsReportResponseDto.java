package com.insa.hospital.dto;

import java.util.List;

/**
 * Doctor-facing DHS disease registration report.
 *
 * The report is shaped for the monthly disease table used by the
 * DHS registration screen and print layout.
 */
public record DoctorDhsReportResponseDto(
        String organizationUnit,
        String datasetLabel,
        String periodLabel,
        String dateFrom,
        String dateTo,
        String selectedDepartment,
        List<String> departments,
        String outcome,
        List<String> ageBuckets,
        List<DiseaseRowDto> diseaseRows,
        List<Long> grandMaleCounts,
        List<Long> grandFemaleCounts,
        long grandTotal
) {

    public record DiseaseRowDto(
            String disease,
            List<Long> maleCounts,
            List<Long> femaleCounts,
            long total
    ) {}
}
