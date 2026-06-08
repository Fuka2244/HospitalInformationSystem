package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.StaffLoginDto;

public interface IStaffAuthService {
    Result loginWithJwt(StaffLoginDto loginDto);

    Result logoutWithJwt(String token);
}
