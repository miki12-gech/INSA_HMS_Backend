package com.hospital.hms.dto;

import lombok.Data;

@Data
public class PaymentCategoryDto {
    private Long id;
    private String category;
    private String description;
    private String cPrice;
    private String type;
    private Integer dCommission;
    private Integer hCommission;
    private String hospitalId;
}
