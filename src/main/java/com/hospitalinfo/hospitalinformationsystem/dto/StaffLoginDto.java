package com.hospitalinfo.hospitalinformationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffLoginDto {
    @NotBlank(message = "account must not be blank")
    private String account;

    @NotBlank(message = "password must not be blank")
    private String password;

    @NotBlank(message = "role must not be blank")
    private String role;
}
