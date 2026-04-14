package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_report")
public class MedicalReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("patient_id")
    private String patientId;
    @TableField("medical_record_id")
    private Long medicalRecordId;
    @TableField("doctor_id")
    private Long doctorId;
    @TableField("report_type")
    private String reportType;
    private String title;
    @TableField("examination_data")
    private String examinationData;
    @TableField("ai_summary")
    private String aiSummary;
    @TableField("ai_diagnosis")
    private String aiDiagnosis;
    @TableField("ai_treatment")
    private String aiTreatment;
    @TableField("ai_recommendation")
    private String aiRecommendation;
    @TableField("pdf_path")
    private String pdfPath;
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
