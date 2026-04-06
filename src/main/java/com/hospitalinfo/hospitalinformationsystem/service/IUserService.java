package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.LoginDto;
import com.hospitalinfo.hospitalinformationsystem.dto.RegisterDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import jakarta.servlet.http.HttpSession;

public interface IUserService {
    Result login(LoginDto loginDto, HttpSession session);

    Result register(RegisterDto registerDto);

    Result loginOut(HttpSession session);

    Result info(HttpSession session);
}
