package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.StaffLoginDto;
import com.hospitalinfo.hospitalinformationsystem.service.IStaffAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffAuthController {

    private final IStaffAuthService staffAuthService;

    @PostMapping("/login/jwt")
    public Result loginWithJwt(@RequestBody @Valid StaffLoginDto loginDto) {
        return staffAuthService.loginWithJwt(loginDto);
    }

    @PostMapping("/loginout/jwt")
    public Result logoutWithJwt(@RequestHeader(value = "Authorization", required = false) String token) {
        return staffAuthService.logoutWithJwt(token);
    }
}
