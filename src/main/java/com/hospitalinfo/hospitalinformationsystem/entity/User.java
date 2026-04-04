package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("patient")
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
