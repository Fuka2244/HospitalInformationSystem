package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medicine_inventory")
public class MedicineInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("medicine_id")
    private Long medicineId;
    private Integer quantity;
    private Integer minStock;
    private Integer maxStock;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String supplier;
    @TableField("batch_number")
    private String batchNumber;
    @TableField("expiry_date")
    private LocalDateTime expiryDate;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String medicineName;
}