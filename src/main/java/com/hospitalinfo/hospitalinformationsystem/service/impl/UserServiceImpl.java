package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospitalinfo.hospitalinformationsystem.dto.LoginDto;
import com.hospitalinfo.hospitalinformationsystem.dto.RegisterDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.User;
import com.hospitalinfo.hospitalinformationsystem.mapper.UserMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IUserService;
import com.hospitalinfo.hospitalinformationsystem.utils.EncodePassword;
import com.hospitalinfo.hospitalinformationsystem.utils.MatchPassword;
import com.hospitalinfo.hospitalinformationsystem.utils.RegexTool;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result login(LoginDto loginDto, HttpSession session) {
        //1.获取手机号和密码
        String phone = loginDto.getPhone();
        String password = loginDto.getPassword();
        //2.判断手机号是否正确
        boolean isPhone = RegexTool.isPhone(phone);
        if(!isPhone){
            return Result.fail("手机号格式不正确");
        }
        //3.判断手机号是否存在
        User user = this.lambdaQuery()
                .eq(User::getPhone, phone)
                .one();
        if(user == null){
            return Result.fail("手机号不存在");
        }
        //4.判断密码是否正确
        String encodePassword = user.getPassword();
        boolean match = MatchPassword.match(password, encodePassword);
        if(!match){
            return Result.fail("密码不正确");
        }
        //5.登录成功
        return Result.ok();
    }

    @Override
    public Result register(RegisterDto registerDto) {
        //1.查询手机号、身份证是否符合规矩
        //String name = registerDto.getName();
        int age = registerDto.getAge();
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

        long count = this.lambdaQuery()
                .eq(User::getPhone, registerDto.getPhone())
                .or()
                .eq(User::getIdCard, registerDto.getIdCard())
                .count();

        if(count > 0){
            return Result.fail("该手机号或身份证已被注册");
        }

        //5查询两次密码是否一致
        if(password == null||!password.equals(confirmPassword)){
            //5.1不一致，返回错误信息
            return Result.fail("两次密码不一致");
        }
        //6.满足注册要求
        //6.1uuid随机分配一个账号
        String account = UUID.randomUUID().toString();

        //6.2保存用户信息
        //密码加密
        User user = new User();
        user.setAccount(account);
        user.setUsername(username);
        String encodePassword = EncodePassword.encrypt(password);
        user.setPassword(encodePassword);
        user.setGender(gender);
        user.setAge(age);
        user.setPhone(phone);
        user.setAddress(address);
        user.setIdCard(idCard);
        //6.3将用户信息保存到数据库
        boolean success = this.save(user);
        //6.4返回成功/失败信息
        return success ? Result.ok(user) : Result.fail("注册失败，请稍后再试");
    }
}
