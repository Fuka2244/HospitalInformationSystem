package com.hospitalinfo.hospitalinformationsystem.controller;


import com.hospitalinfo.hospitalinformationsystem.dto.LoginDto;
import com.hospitalinfo.hospitalinformationsystem.dto.RegisterDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.UserDto;
import com.hospitalinfo.hospitalinformationsystem.service.IUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private IUserService userService;

    //登录
    @PostMapping("/login")
    public Result login(@RequestBody LoginDto loginDto, HttpSession session){
        // 登录逻辑
        return userService.login(loginDto,session);
    }

    //登出
    @PostMapping("/loginout")
    public Result loginOut(HttpSession session){
        //todo 登出逻辑
        return Result.fail("功能未实现");
    }

    //注册
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDto registerDto){
        //注册逻辑
        return userService.register(registerDto);
    }


}
