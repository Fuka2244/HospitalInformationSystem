package com.hospitalinfo.hospitalinformationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MedicineInventoryDto {
    @NotNull(message = "药品ID不能为空")
    private Long medicineId;
    @NotNull(message = "数量不能为空")
    private Integer quantity;
    private Integer minStock;
    private Integer maxStock;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String supplier;
    private String batchNumber;
    private LocalDateTime expiryDate;
}