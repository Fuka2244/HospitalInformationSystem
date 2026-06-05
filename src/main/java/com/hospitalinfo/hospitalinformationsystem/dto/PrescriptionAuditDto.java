package com.hospitalinfo.hospitalinformationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrescriptionAuditDto {
    @NotNull(message = "处方ID不能为空")
    private Long prescriptionId;
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;
    private String auditRemark;
}