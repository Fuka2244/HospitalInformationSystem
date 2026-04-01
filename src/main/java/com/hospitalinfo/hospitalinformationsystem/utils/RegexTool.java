package com.hospitalinfo.hospitalinformationsystem.utils;
//正则表达式用于检验一些格式是否正确

public class RegexTool {
    //用于校验手机号是否正确
    public static boolean isPhone(String phone){
        return phone.matches("^1[3456789]\\d{9}$");
    }

    //用于校验身份证是否正确
    public static boolean isIdCard(String idCard){
        return idCard.matches("^\\d{15}$") || idCard.matches("^\\d{18}$");
    }
}
