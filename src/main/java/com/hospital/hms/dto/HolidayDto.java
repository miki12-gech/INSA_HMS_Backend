package com.hospital.hms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDto {
    private Long id;
    private String doctor;
    private String date;
    private String hospitalId;
}
