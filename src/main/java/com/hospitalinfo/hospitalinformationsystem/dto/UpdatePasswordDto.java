package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

@Data
public class UpdatePasswordDto {
    private String phone;
    private String newPassword;
    private String confirmPassword;
}
