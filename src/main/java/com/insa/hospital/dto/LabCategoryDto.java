package com.insa.hospital.dto;

public record LabCategoryDto(
    Long id,
    String category,
    String description,
    String reference_value
) {}
