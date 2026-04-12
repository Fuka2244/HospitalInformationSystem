package com.hospitalinfo.hospitalinformationsystem.controller;


import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.service.IUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
        //登出逻辑
        return userService.loginOut(session);
        //return Result.fail("功能未实现");
    }

    //注册
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDto registerDto){
        //注册逻辑
        return userService.register(registerDto);
    }

    //查看个人信息
    @GetMapping("/me")
    public Result info(HttpSession session){
        return userService.info(session);
    }

    //修改个人信息
    @PutMapping("/me/update")
    public Result update(@RequestBody UpdateDto updateDto, HttpSession session){
        return userService.update(updateDto,session);
    }

    //忘记密码
    @PutMapping("/login/forget")
    public Result updatePassword(@RequestBody UpdatePasswordDto updatePasswordDto, HttpSession session){
        return userService.updatePassword(updatePasswordDto,session);
    }


}
