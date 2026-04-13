package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prescription")
public class Prescription {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("medical_record_id")
    private Long medicalRecordId;
    @TableField("patient_id")
    private String patientId;
    @TableField("doctor_id")
    private Long doctorId;
    @TableField("prescription_date")
    private LocalDateTime prescriptionDate;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 非数据库字段 */
    @TableField(exist = false)
    private String patientName;
    @TableField(exist = false)
    private String doctorName;
}
