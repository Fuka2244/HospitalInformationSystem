package com.hospitalinfo.hospitalinformationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class VisitRecordDto {
    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;
    private String chiefComplaint;
    private String presentIllness;
    private String diagnosis;
    private String treatment;
    private String notes;
    private List<PrescriptionItemDto> prescriptionItems;
}
