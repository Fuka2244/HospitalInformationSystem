package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medicine_stock_log")
public class MedicineStockLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("medicine_id")
    private Long medicineId;
    @TableField("inventory_id")
    private Long inventoryId;
    private String operationType;
    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;
    private BigDecimal unitPrice;
    private String operator;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String medicineName;
}