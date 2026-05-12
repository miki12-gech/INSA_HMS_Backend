package com.insa.hospital.dto;

import lombok.Data;

@Data
public class CalendarEventDto {
    private Long id;
    private String title;
    private String start;
    private String end;
    private String color;
}
