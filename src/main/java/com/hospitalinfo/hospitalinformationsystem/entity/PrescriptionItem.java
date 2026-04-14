package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("prescription_item")
public class PrescriptionItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("prescription_id")
    private Long prescriptionId;
    @TableField("medicine_id")
    private Long medicineId;
    private String dosage;
    private Integer quantity;
    private Integer days;
    private String remark;

    /** 非数据库字段 */
    @TableField(exist = false)
    private String medicineName;
}
