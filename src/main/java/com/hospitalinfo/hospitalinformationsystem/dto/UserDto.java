package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

@Data
public class UserDto {
    private String username;
    private String account;
    private String gender;
    private int age;
    private String phone;
}
