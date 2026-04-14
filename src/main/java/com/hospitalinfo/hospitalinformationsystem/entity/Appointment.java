package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("patient_id")
    private String patientId;
    @TableField("doctor_id")
    private Long doctorId;
    @TableField("department_id")
    private Long departmentId;
    @TableField("appointment_type")
    private String appointmentType;
    @TableField("examination_type")
    private String examinationType;
    @TableField("appointment_date")
    private LocalDate appointmentDate;
    @TableField("time_slot")
    private String timeSlot;
    private Integer status;
    @TableField("cancel_reason")
    private String cancelReason;
    @TableField("ai_recommended")
    private Integer aiRecommended;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 非数据库字段 */
    @TableField(exist = false)
    private String patientName;
    @TableField(exist = false)
    private String doctorName;
    @TableField(exist = false)
    private String departmentName;
}
