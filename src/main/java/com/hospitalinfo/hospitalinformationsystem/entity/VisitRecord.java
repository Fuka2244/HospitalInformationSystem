package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visit_record")
public class VisitRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("appointment_id")
    private Long appointmentId;
    @TableField("patient_id")
    private String patientId;
    @TableField("doctor_id")
    private Long doctorId;
    @TableField("department_id")
    private Long departmentId;
    private String visitNumber;
    private Integer callStatus;
    @TableField("call_time")
    private LocalDateTime callTime;
    @TableField("check_in_time")
    private LocalDateTime checkInTime;
    @TableField("visit_start_time")
    private LocalDateTime visitStartTime;
    @TableField("visit_end_time")
    private LocalDateTime visitEndTime;
    private String diagnosis;
    private String treatment;
    private String notes;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String patientName;
    @TableField(exist = false)
    private String doctorName;
    @TableField(exist = false)
    private String departmentName;
}