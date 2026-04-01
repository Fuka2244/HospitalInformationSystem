package com.hospitalinfo.hospitalinformationsystem.entity;

import lombok.Data;

@Data
public class User {
    private String id;//用哈希值随机生成
    private String username;
    private String account;
    private String password;
    private String gender;
    private int age;
    private String IdCard;   //身份证
    private String phone;
    //private String email;
    private String address;

}
