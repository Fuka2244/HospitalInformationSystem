package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.*;
import jakarta.servlet.http.HttpSession;

public interface IUserService {
    Result login(LoginDto loginDto, HttpSession session);

    Result register(RegisterDto registerDto);

    Result loginOut(HttpSession session);

    Result info(HttpSession session);

    Result update(UpdateDto updateDto, HttpSession session);

    Result updatePassword(UpdatePasswordDto updatePasswordDto, HttpSession session);
}
