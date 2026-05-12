package com.insa.hospital.dto;

public record TemplateDto(
    Long id,
    String name,
    String template,
    String category,
    String user
) {}
