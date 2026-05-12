package com.insa.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineDto {
    private Integer id;
    private String name;
    private String category;
    private String price;
    private Integer quantity;
    private String sPrice;
    private String generic;
    private String company;
    private String eDate;
    private String hospitalId;
}
