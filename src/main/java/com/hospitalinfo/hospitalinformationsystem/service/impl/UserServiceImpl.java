package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.dto.LoginDto;
import com.hospitalinfo.hospitalinformationsystem.dto.RegisterDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.User;
import com.hospitalinfo.hospitalinformationsystem.mapper.UserMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IUserService;
import com.hospitalinfo.hospitalinformationsystem.utils.RegexTool;
import jakarta.servlet.http.HttpSession;

import java.util.UUID;

public class UserServiceImpl implements IUserService {

    @Override
    public Result login(LoginDto loginDto, HttpSession session) {

        return null;
    }

    @Override
    public Result register(RegisterDto registerDto) {
        //1.查询手机号、身份证是否符合规矩
        String name = registerDto.getName();
        String username = registerDto.getUsername();
        String password = registerDto.getPassword();
        String confirmPassword = registerDto.getConfirmPassword();
        String gender = registerDto.getGender();
        String phone = registerDto.getPhone();
        String address = registerDto.getAddress();
        String idCard = registerDto.getIdCard();

        Boolean isPhone = RegexTool.isPhone(phone);
        Boolean isIdCard = RegexTool.isIdCard(idCard);
        //1.1不符合，返回错误信息
        if(!isPhone || !isIdCard){
            return Result.fail("手机号或身份证格式不正确");
        }

        //2.查询该手机号、身份证是否已经被注册
        //todo usermapper相关
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("phone", phone).or().eq("id_card", idCard));
        if(user != null){
            return Result.fail("该手机号或身份证已被注册");
        }
        //3.判断用户名是否存在
        user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if(user != null){
            return Result.fail("该用户名已被注册");
        }
        //5查询两次密码是否一致
        if(!password.equals(confirmPassword){
            //5.1不一致，返回错误信息
            return Result.fail("两次密码不一致");
        }
        //6.满足注册要求
        //6.1uuid随机分配一个账号
        String account = UUID.randomUUID().toString();
        //6.2保存用户信息
        //todo 密码加密
        User user1 = new User();
        user1.setAccount(account);
        user1.setUsername(username);
        user1.setPassword(password);
        user1.setGender(gender);
        user1.setPhone(phone);
        user1.setAddress(address);
        user1.setIdCard(idCard);
        //6.3将用户信息保存到数据库
        userMapper.insert(user1);
        //6.4返回成功信息
        return Result.ok(user1);
    }
}
