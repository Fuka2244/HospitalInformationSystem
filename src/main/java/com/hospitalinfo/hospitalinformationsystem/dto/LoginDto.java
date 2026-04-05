package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class LoginDto {
    String phone;
    String password;
}
