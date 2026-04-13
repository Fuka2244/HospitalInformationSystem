package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("billing")
public class Billing {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("patient_id")
    private String patientId;
    @TableField("appointment_id")
    private Long appointmentId;
    @TableField("medical_record_id")
    private Long medicalRecordId;
    @TableField("item_type")
    private String itemType;
    @TableField("item_name")
    private String itemName;
    private BigDecimal amount;
    private String description;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 非数据库字段 */
    @TableField(exist = false)
    private String patientName;
}
