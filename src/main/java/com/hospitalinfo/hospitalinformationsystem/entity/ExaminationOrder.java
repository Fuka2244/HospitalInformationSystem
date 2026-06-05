package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("examination_order")
public class ExaminationOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("medical_record_id")
    private Long medicalRecordId;
    @TableField("patient_id")
    private String patientId;
    @TableField("doctor_id")
    private Long doctorId;
    private String examinationName;
    private String examinationType;
    private BigDecimal price;
    private Integer status;
    private String result;
    @TableField("examination_date")
    private LocalDateTime examinationDate;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String patientName;
    @TableField(exist = false)
    private String doctorName;
}