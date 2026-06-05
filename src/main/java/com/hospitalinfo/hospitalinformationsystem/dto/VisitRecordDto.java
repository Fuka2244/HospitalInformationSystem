package com.hospitalinfo.hospitalinformationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VisitRecordDto {
    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;
    private String diagnosis;
    private String treatment;
    private String notes;
}