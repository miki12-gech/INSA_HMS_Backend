package com.insa.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabResultSubmissionDto {
    private Integer orderId;
    private String reportHtml;
    private String laboratorianId;
}
