package com.hospitalinfo.hospitalinformationsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.Department;
import com.hospitalinfo.hospitalinformationsystem.entity.Doctor;
import com.hospitalinfo.hospitalinformationsystem.mapper.DepartmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科室信息控制器
 */
@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentMapper departmentMapper;
    private final DoctorMapper doctorMapper;

    /**
     * 获取所有科室列表
     * GET /department/list
     */
    @GetMapping("/list")
    public Result listDepartments() {
        List<Department> departments = departmentMapper.selectList(
                new QueryWrapper<Department>().eq("status", 1).orderByAsc("id"));
        return Result.ok(departments);
    }

    /**
     * 获取科室详情
     * GET /department/{id}
     */
    @GetMapping("/{id}")
    public Result getDepartmentDetail(@PathVariable Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            return Result.fail("科室不存在");
        }
        return Result.ok(department);
    }

    /**
     * 获取科室下的医生列表
     * GET /department/{id}/doctors
     */
    @GetMapping("/{id}/doctors")
    public Result getDepartmentDoctors(@PathVariable Long id) {
        List<Doctor> doctors = doctorMapper.selectList(
                new QueryWrapper<Doctor>().eq("department_id", id).eq("status", 1));
        return Result.ok(doctors);
    }
}
