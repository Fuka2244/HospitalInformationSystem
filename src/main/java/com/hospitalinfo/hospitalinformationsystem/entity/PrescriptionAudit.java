package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prescription_audit")
public class PrescriptionAudit {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("prescription_id")
    private Long prescriptionId;
    @TableField("pharmacist_id")
    private Long pharmacistId;
    private Integer auditStatus;
    private String auditRemark;
    @TableField("audit_time")
    private LocalDateTime auditTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String pharmacistName;
}