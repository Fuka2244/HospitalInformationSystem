package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class UpdateDto {
    private String username;
    private String phone;
    private String address;
    private String password;
    //private String idCard;
    //todo 目前设定无法修改身份证号码
}
