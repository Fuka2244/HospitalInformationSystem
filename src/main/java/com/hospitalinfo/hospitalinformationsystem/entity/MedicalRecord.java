package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_record")
public class MedicalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("patient_id")
    private String patientId;
    @TableField("doctor_id")
    private Long doctorId;
    @TableField("department_id")
    private Long departmentId;
    @TableField("chief_complaint")
    private String chiefComplaint;
    @TableField("present_illness")
    private String presentIllness;
    private String diagnosis;
    @TableField("treatment_plan")
    private String treatmentPlan;
    @TableField("visit_date")
    private LocalDateTime visitDate;
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
    @TableField(exist = false)
    private String departmentName;
}
