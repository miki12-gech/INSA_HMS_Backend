package com.hospital.hms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeScheduleDto {
    private Long id;
    private String doctor;
    private String sTime;
    private String eTime;
    private String weekday;
    private String sTimeKey;
    private String duration;
    private String hospitalId;
}
