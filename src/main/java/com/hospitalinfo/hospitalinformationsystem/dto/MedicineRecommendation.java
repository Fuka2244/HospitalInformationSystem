package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * AI药物推荐结果
 */
@Data
public class MedicineRecommendation {
    private String medicineName;
    private String reason;
    private String dosage;
    private String precautions;
}
