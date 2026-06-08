package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.dto.LoginResponseDto;
import com.hospitalinfo.hospitalinformationsystem.dto.PatientInfoVo;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.StaffLoginDto;
import com.hospitalinfo.hospitalinformationsystem.entity.Admin;
import com.hospitalinfo.hospitalinformationsystem.entity.Doctor;
import com.hospitalinfo.hospitalinformationsystem.entity.Pharmacist;
import com.hospitalinfo.hospitalinformationsystem.mapper.AdminMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.PharmacistMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IRedisSessionService;
import com.hospitalinfo.hospitalinformationsystem.service.IStaffAuthService;
import com.hospitalinfo.hospitalinformationsystem.utils.JwtTokenUtil;
import com.hospitalinfo.hospitalinformationsystem.utils.MatchPassword;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffAuthServiceImpl implements IStaffAuthService {

    private final AdminMapper adminMapper;
    private final DoctorMapper doctorMapper;
    private final PharmacistMapper pharmacistMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final IRedisSessionService redisSessionService;

    @Value("${jwt.expiration:86400}")
    private long jwtExpiration;

    @Override
    public Result loginWithJwt(StaffLoginDto loginDto) {
        String role = normalizeRole(loginDto.getRole());
        PatientInfoVo staffInfo = switch (role) {
            case "doctor" -> authenticateDoctor(loginDto);
            case "pharmacist" -> authenticatePharmacist(loginDto);
            case "admin" -> authenticateAdmin(loginDto);
            default -> null;
        };

        if (staffInfo == null) {
            return Result.fail("账号、密码或角色不正确");
        }

        String token = jwtTokenUtil.generateToken(staffInfo.getAccount(), role);
        String refreshToken = jwtTokenUtil.generateRefreshToken(staffInfo.getAccount());
        redisSessionService.saveSession(token, staffInfo, jwtExpiration);

        LoginResponseDto response = new LoginResponseDto()
                .setToken(token)
                .setRefreshToken(refreshToken)
                .setExpiresIn(jwtExpiration)
                .setPatientInfo(staffInfo);

        log.info("员工JWT登录成功: role={}, account={}", role, staffInfo.getAccount());
        return Result.ok(response);
    }

    @Override
    public Result logoutWithJwt(String token) {
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (StringUtils.hasText(token)) {
            redisSessionService.deleteSession(token);
        }
        return Result.ok("登出成功");
    }

    private PatientInfoVo authenticateDoctor(StaffLoginDto loginDto) {
        Doctor doctor = doctorMapper.selectOne(new QueryWrapper<Doctor>()
                .and(wrapper -> wrapper.eq("username", loginDto.getAccount()).or().eq("phone", loginDto.getAccount()))
                .eq("status", 1)
                .last("LIMIT 1"));
        if (doctor == null || !MatchPassword.match(loginDto.getPassword(), doctor.getPassword())) {
            return null;
        }
        PatientInfoVo vo = new PatientInfoVo();
        vo.setAccount(doctor.getId().toString());
        vo.setUsername(doctor.getUsername());
        vo.setName(doctor.getName());
        vo.setGender(doctor.getGender());
        vo.setAge(doctor.getAge());
        vo.setPhone(doctor.getPhone());
        vo.setRole("doctor");
        return vo;
    }

    private PatientInfoVo authenticatePharmacist(StaffLoginDto loginDto) {
        Pharmacist pharmacist = pharmacistMapper.selectOne(new QueryWrapper<Pharmacist>()
                .and(wrapper -> wrapper.eq("username", loginDto.getAccount()).or().eq("phone", loginDto.getAccount()))
                .eq("status", 1)
                .last("LIMIT 1"));
        if (pharmacist == null || !MatchPassword.match(loginDto.getPassword(), pharmacist.getPassword())) {
            return null;
        }
        PatientInfoVo vo = new PatientInfoVo();
        vo.setAccount(pharmacist.getId().toString());
        vo.setUsername(pharmacist.getUsername());
        vo.setName(pharmacist.getName());
        vo.setGender(pharmacist.getGender());
        vo.setPhone(pharmacist.getPhone());
        vo.setRole("pharmacist");
        return vo;
    }

    private PatientInfoVo authenticateAdmin(StaffLoginDto loginDto) {
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>()
                .eq("username", loginDto.getAccount())
                .last("LIMIT 1"));
        if (admin == null || !MatchPassword.match(loginDto.getPassword(), admin.getPassword())) {
            return null;
        }
        PatientInfoVo vo = new PatientInfoVo();
        vo.setAccount(admin.getId().toString());
        vo.setUsername(admin.getUsername());
        vo.setName(admin.getName());
        vo.setPhone(admin.getPhone());
        vo.setRole("admin");
        return vo;
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase();
    }
}
