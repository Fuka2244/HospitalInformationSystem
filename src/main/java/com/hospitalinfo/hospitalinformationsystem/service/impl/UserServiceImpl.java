package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.User;
import com.hospitalinfo.hospitalinformationsystem.mapper.UserMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IUserService;
import com.hospitalinfo.hospitalinformationsystem.utils.EncodePassword;
import com.hospitalinfo.hospitalinformationsystem.utils.MatchPassword;
import com.hospitalinfo.hospitalinformationsystem.utils.RegexTool;
import jakarta.annotation.Resource;
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
        UserDto userDto = new UserDto();
        //userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setAccount(user.getAccount());
        userDto.setGender(user.getGender());
        userDto.setAge(user.getAge());
        userDto.setPhone(user.getPhone());
        session.setAttribute("user",userDto);
        session.setAttribute("phone",user.getPhone());
        session.setAttribute("account",user.getAccount());
        return Result.ok(userDto);
    }

    @Override
    public Result register(RegisterDto registerDto) {
        //1.查询手机号、身份证是否符合规矩
        String name = registerDto.getName();
        int age = registerDto.getAge();
        String username = registerDto.getUsername();
        String password = registerDto.getPassword();
        String confirmPassword = registerDto.getConfirmPassword();
        String gender = registerDto.getGender();
        String phone = registerDto.getPhone();
        String address = registerDto.getAddress();
        String idCard = registerDto.getIdCard();

        boolean isPhone = RegexTool.isPhone(phone);
        boolean isIdCard = RegexTool.isIdCard(idCard);
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
        user.setName(name);
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

    @Override
    public Result loginOut(HttpSession session) {
        // 移除session
        session.removeAttribute("user");
        session.invalidate();
        return Result.ok("登出成功");
    }

    @Override
    public Result info(HttpSession session) {
        // 获取session中的用户信息
        UserDto userDto = (UserDto) session.getAttribute("user");
        return Result.ok(userDto);
    }

    @Override
    public Result update(UpdateDto updateDto, HttpSession session) {
        // 获取当前用户手机号
        String phone = (String) session.getAttribute("phone");
        String password = updateDto.getPassword();
        User user = this.lambdaQuery()
                .eq(User::getPhone, phone)
                .one();
        String encodePassword = user.getPassword();
        boolean match = MatchPassword.match(password, encodePassword);
        if(!match){
            return Result.fail("密码不正确");
        }
        //3.修改信息：地址、电话、用户名（只更新非空字段）
        String address = updateDto.getAddress();
        String phone1 = updateDto.getPhone();
        String username = updateDto.getUsername();

        //4.如果新电话非空，则进行手机号校验
        if(phone1 != null && !phone1.isEmpty()){
            boolean isPhone = RegexTool.isPhone(phone1);
            if(!isPhone){
                return Result.fail("手机号格式不正确");
            }
            //5.如果新电话非空，则进行手机号校验
            long count = this.lambdaQuery()
                    .eq(User::getPhone, phone1)
                    .count();
            if(count > 0){
                return Result.fail("该手机号已被注册");
            }
        }

        // 进行修改
        this.lambdaUpdate()
                .eq(User::getPhone, phone)
                .set(address != null && !address.isEmpty(), User::getAddress, address)
                .set(phone1 != null && !phone1.isEmpty(), User::getPhone, phone1)
                .set(username != null && !username.isEmpty(), User::getUsername, username)
                .update();

        // 同步更新session中的信息
        if (phone1 != null && !phone1.isEmpty()) {
            // 修改了手机号，需要更新session中的phone和userDto.phone
            session.setAttribute("phone", phone1);
            UserDto userDto = (UserDto) session.getAttribute("user");
            if (userDto != null) {
                userDto.setPhone(phone1);
                session.setAttribute("user", userDto);
            }
        }

        if (username != null && !username.isEmpty()) {
            // 修改了用户名，需要更新session中的userDto.username
            UserDto userDto = (UserDto) session.getAttribute("user");
            if (userDto != null) {
                userDto.setUsername(username);
                session.setAttribute("user", userDto);
            }
        }

        return  Result.ok("修改成功");
    }

    @Override
    public Result updatePassword(UpdatePasswordDto updatePasswordDto, HttpSession session) {
        String phone = updatePasswordDto.getPhone();
        String newPassword = updatePasswordDto.getNewPassword();
        String confirmPassword = updatePasswordDto.getConfirmPassword();

        // 1.校验手机号格式
        if (phone == null || !RegexTool.isPhone(phone)) {
            return Result.fail("手机号格式不正确");
        }
        // 2.校验两次密码一致
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            return Result.fail("两次密码不一致");
        }
        // 3.查找用户
        User user = this.lambdaQuery().eq(User::getPhone, phone).one();
        if (user == null) {
            return Result.fail("该手机号未注册");
        }
        // 4.加密新密码并更新
        String encoded = EncodePassword.encrypt(newPassword);
        this.lambdaUpdate()
                .eq(User::getPhone, phone)
                .set(User::getPassword, encoded)
                .update();

        return Result.ok("密码修改成功");
    }
}
