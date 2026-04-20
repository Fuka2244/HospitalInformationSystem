package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("patient")
public class Patient {
    private String username;
    private String name;
    @TableId
    private String account;//uuid随机生成
    private String password;
    private String gender;
    private int age;
    @TableField("id_card")
    private String idCard;   //身份证
    private String phone;
    private String address;
    @TableField("avatar")
    private String avatar;
}
