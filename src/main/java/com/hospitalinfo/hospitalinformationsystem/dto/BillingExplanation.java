package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI费用解释结果
 */
@Data
public class BillingExplanation {
    private BigDecimal totalAmount;
    private String breakdown;
    private String explanation;
    private String suggestion;
}
