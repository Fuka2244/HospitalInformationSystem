package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

@Data
public class RegisterDto {
    String name;
    String username;
    String password;
    String confirmPassword;
    String gender;
    String phone;
    String address;
    String idCard;
}
