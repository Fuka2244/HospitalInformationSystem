package com.hospitalinfo.hospitalinformationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FrontDeskRegistrationDto extends AppointmentCreateDto {
    private Long appointmentId;

    @NotBlank(message = "患者ID不能为空")
    private String patientId;

    private String location;
}
