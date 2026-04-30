package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.IPatientService;
import com.hospitalinfo.hospitalinformationsystem.utils.EncodePassword;
import com.hospitalinfo.hospitalinformationsystem.utils.MatchPassword;
import com.hospitalinfo.hospitalinformationsystem.utils.RegexTool;
import com.hospitalinfo.hospitalinformationsystem.utils.VerificationCodeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements IPatientService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final DepartmentMapper departmentMapper;
    private final DoctorMapper doctorMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final MedicineMapper medicineMapper;
    private final VerificationCodeService verificationCodeService;

    // ==================== 账户管理 ====================

    @Override
    public Result login(LoginDto loginDto, HttpSession session) {
        String phone = loginDto.getPhone();
        String password = loginDto.getPassword();
        boolean isPhone = RegexTool.isPhone(phone);
        if (!isPhone) {
            return Result.fail("手机号格式不正确");
        }
        Patient patient = this.lambdaQuery()
                .eq(Patient::getPhone, phone)
                .one();
        if (patient == null) {
            return Result.fail("手机号不存在");
        }
        String encodePassword = patient.getPassword();
        boolean match = MatchPassword.match(password, encodePassword);
        if (!match) {
            return Result.fail("密码不正确");
        }
        // 登录成功，构建PatientInfoVo存入session
        PatientInfoVo vo = buildPatientInfoVo(patient);
        session.setAttribute("patient", vo);
        session.setAttribute("phone", patient.getPhone());
        session.setAttribute("account", patient.getAccount());
        session.setAttribute("role", "patient");
        return Result.ok(vo);
    }

    @Override
    public Result register(RegisterDto registerDto) {
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
        if (!isPhone || !isIdCard) {
            return Result.fail("手机号或身份证格式不正确");
        }

        long count = this.lambdaQuery()
                .eq(Patient::getPhone, registerDto.getPhone())
                .or()
                .eq(Patient::getIdCard, registerDto.getIdCard())
                .count();

        if (count > 0) {
            return Result.fail("该手机号或身份证已被注册");
        }

        if (password == null || !password.equals(confirmPassword)) {
            return Result.fail("两次密码不一致");
        }

        String account = UUID.randomUUID().toString();

        Patient patient = new Patient();
        patient.setAccount(account);
        patient.setUsername(username);
        patient.setName(name);
        String encodePassword = EncodePassword.encrypt(password);
        patient.setPassword(encodePassword);
        patient.setGender(gender);
        patient.setAge(age);
        patient.setPhone(phone);
        patient.setAddress(address);
        patient.setIdCard(idCard);

        boolean success = this.save(patient);
        if (success) {
            PatientInfoVo vo = buildPatientInfoVo(patient);
            return Result.ok(vo);
        }
        return Result.fail("注册失败，请稍后再试");
    }

    @Override
    public Result loginOut(HttpSession session) {
        session.removeAttribute("patient");
        session.invalidate();
        return Result.ok("登出成功");
    }

    @Override
    public Result info(HttpSession session) {
        PatientInfoVo vo = (PatientInfoVo) session.getAttribute("patient");
        if (vo != null) {
            // 从数据库重新查询最新信息（含就诊统计）
            String account = (String) session.getAttribute("account");
            if (account != null) {
                Patient patient = this.getById(account);
                if (patient != null) {
                    vo = buildPatientInfoVo(patient);
                    session.setAttribute("patient", vo);
                }
            }
            return Result.ok(vo);
        }
        return Result.ok(null);
    }

    @Override
    public Result update(UpdateDto updateDto, HttpSession session) {
        String phone = (String) session.getAttribute("phone");
        String password = updateDto.getPassword();
        Patient patient = this.lambdaQuery()
                .eq(Patient::getPhone, phone)
                .one();
        String encodePassword = patient.getPassword();
        boolean match = MatchPassword.match(password, encodePassword);
        if (!match) {
            return Result.fail("密码不正确");
        }

        String address = updateDto.getAddress();
        String phone1 = updateDto.getPhone();
        String username = updateDto.getUsername();

        if (phone1 != null && !phone1.isEmpty()) {
            boolean isPhone = RegexTool.isPhone(phone1);
            if (!isPhone) {
                return Result.fail("手机号格式不正确");
            }
            long count = this.lambdaQuery()
                    .eq(Patient::getPhone, phone1)
                    .count();
            if (count > 0) {
                return Result.fail("该手机号已被注册");
            }
        }

        this.lambdaUpdate()
                .eq(Patient::getPhone, phone)
                .set(address != null && !address.isEmpty(), Patient::getAddress, address)
                .set(phone1 != null && !phone1.isEmpty(), Patient::getPhone, phone1)
                .set(username != null && !username.isEmpty(), Patient::getUsername, username)
                .update();

        // 同步更新session
        if (phone1 != null && !phone1.isEmpty()) {
            session.setAttribute("phone", phone1);
            PatientInfoVo vo = (PatientInfoVo) session.getAttribute("patient");
            if (vo != null) {
                vo.setPhone(phone1);
                session.setAttribute("patient", vo);
            }
        }

        if (username != null && !username.isEmpty()) {
            PatientInfoVo vo = (PatientInfoVo) session.getAttribute("patient");
            if (vo != null) {
                vo.setUsername(username);
                session.setAttribute("patient", vo);
            }
        }

        return Result.ok("修改成功");
    }

    @Override
    public Result sendVerificationCode(String phone) {
        if (phone == null || !RegexTool.isPhone(phone)) {
            return Result.fail("手机号格式不正确");
        }
        Patient patient = this.lambdaQuery().eq(Patient::getPhone, phone).one();
        if (patient == null) {
            return Result.fail("该手机号未注册");
        }
        String code = verificationCodeService.generateCode(phone);
        if (code == null) {
            return Result.fail("验证码发送过于频繁，请60秒后重试");
        }
        // 验证码通过短信服务发送，不再明文返回
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("message", "验证码已发送");
        return Result.ok(data);
    }

    @Override
    public Result updatePassword(UpdatePasswordDto updatePasswordDto, HttpSession session) {
        String phone = updatePasswordDto.getPhone();
        String newPassword = updatePasswordDto.getNewPassword();
        String confirmPassword = updatePasswordDto.getConfirmPassword();
        String verificationCode = updatePasswordDto.getVerificationCode();

        if (phone == null || !RegexTool.isPhone(phone)) {
            return Result.fail("手机号格式不正确");
        }
        if (verificationCode == null || verificationCode.isEmpty()) {
            return Result.fail("请输入验证码");
        }
        if (!verificationCodeService.verifyCode(phone, verificationCode)) {
            return Result.fail("验证码错误或已过期，请重新获取");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            return Result.fail("两次密码不一致");
        }
        Patient patient = this.lambdaQuery().eq(Patient::getPhone, phone).one();
        if (patient == null) {
            return Result.fail("该手机号未注册");
        }
        String encoded = EncodePassword.encrypt(newPassword);
        this.lambdaUpdate()
                .eq(Patient::getPhone, phone)
                .set(Patient::getPassword, encoded)
                .update();

        return Result.ok("密码修改成功");
    }

    // ==================== 患者业务数据 ====================

    @Override
    public Result getPatientInfo(String patientId, HttpSession session) {
        // 权限验证：患者只能查看自己的信息，医生/药师/管理员可以查看
        Object role = session.getAttribute("role");
        String currentAccount = (String) session.getAttribute("account");
        
        if ("patient".equals(role) && !patientId.equals(currentAccount)) {
            return Result.fail("无权查看其他患者的信息");
        }
        
        Patient patient = this.getById(patientId);
        if (patient == null) {
            return Result.fail("患者不存在");
        }
        PatientInfoVo vo = buildPatientInfoVo(patient);
        return Result.ok(vo);
    }

    @Override
    public Result listPatients(String keyword, Integer page, Integer size, HttpSession session) {
        // 权限验证：只有管理员和医生可以查询患者列表
        Object role = session.getAttribute("role");
        if (role == null || (!"admin".equals(role) && !"doctor".equals(role) && !"pharmacist".equals(role))) {
            return Result.fail("无权限查询患者列表");
        }
        
        Page<Patient> pageParam = new Page<>(page, size);
        QueryWrapper<Patient> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("name", keyword)
                    .or().like("phone", keyword)
                    .or().like("id_card", keyword));
        }
        Page<Patient> result = this.page(pageParam, wrapper);
        List<PatientInfoVo> voList = result.getRecords().stream()
                .map(this::buildPatientInfoVo)
                .collect(Collectors.toList());
        return Result.ok(voList, result.getTotal());
    }

    @Override
    public Result getMedicalRecords(String patientId, Integer page, Integer size, HttpSession session) {
        // 权限验证：只允许患者本人、医生、药师、管理员访问
        Object role = session.getAttribute("role");
        String currentAccount = (String) session.getAttribute("account");
        String currentPhone = (String) session.getAttribute("phone");
        
        // 如果是患者，只能查看自己的病历
        if ("patient".equals(role) && !patientId.equals(currentAccount)) {
            return Result.fail("无权访问其他患者的病历");
        }
        
        // 如果是医生，需要验证是否有业务关联（接诊过该患者）
        if ("doctor".equals(role)) {
            Long doctorId = (Long) session.getAttribute("doctorId");
            boolean hasAccess = medicalRecordMapper.selectCount(
                    new QueryWrapper<MedicalRecord>()
                            .eq("patient_id", patientId)
                            .eq("doctor_id", doctorId)
                            .eq("status", 1)
            ) > 0;
            if (!hasAccess) {
                return Result.fail("您未接诊过该患者，无权访问其病历");
            }
        }
        
        Page<MedicalRecord> pageParam = new Page<>(page, size);
        QueryWrapper<MedicalRecord> wrapper = new QueryWrapper<MedicalRecord>()
                .eq("patient_id", patientId)
                .eq("status", 1)
                .orderByDesc("visit_date");
        Page<MedicalRecord> result = medicalRecordMapper.selectPage(pageParam, wrapper);
        fillMedicalRecordNames(result.getRecords());
        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    public Result getMedicalRecordDetail(Long recordId, HttpSession session) {
        MedicalRecord record = medicalRecordMapper.selectById(recordId);
        if (record == null) {
            return Result.fail("病历不存在");
        }
        
        // 权限验证
        Object role = session.getAttribute("role");
        String currentAccount = (String) session.getAttribute("account");
        
        // 如果是患者，只能查看自己的病历
        if ("patient".equals(role) && !record.getPatientId().equals(currentAccount)) {
            return Result.fail("无权访问其他患者的病历");
        }
        
        // 如果是医生，需要验证是否是该病历的接诊医生
        if ("doctor".equals(role)) {
            Long doctorId = (Long) session.getAttribute("doctorId");
            if (!record.getDoctorId().equals(doctorId)) {
                return Result.fail("您不是该病历的接诊医生，无权访问");
            }
        }
        
        fillMedicalRecordNames(List.of(record));

        Prescription prescription = prescriptionMapper.selectOne(
                new QueryWrapper<Prescription>()
                        .eq("medical_record_id", recordId)
                        .last("LIMIT 1"));

        Map<String, Object> detail = new HashMap<>();
        detail.put("record", record);
        if (prescription != null) {
            List<PrescriptionItem> items = prescriptionItemMapper.selectList(
                    new QueryWrapper<PrescriptionItem>().eq("prescription_id", prescription.getId()));
            for (PrescriptionItem item : items) {
                Medicine med = medicineMapper.selectById(item.getMedicineId());
                if (med != null) {
                    item.setMedicineName(med.getName());
                }
            }
            detail.put("prescription", prescription);
            detail.put("prescriptionItems", items);
        }

        return Result.ok(detail);
    }

    @Override
    public Result getVisitHistory(String patientId, Long departmentId, Long doctorId,
                                   String startDate, String endDate, Integer page, Integer size,
                                   HttpSession session) {
        // 权限验证
        Object role = session.getAttribute("role");
        String currentAccount = (String) session.getAttribute("account");
        
        // 如果是患者，只能查看自己的就诊历史
        if ("patient".equals(role) && !patientId.equals(currentAccount)) {
            return Result.fail("无权查看其他患者的就诊历史");
        }
        
        // 如果是医生，只能查看自己接诊过的患者
        if ("doctor".equals(role)) {
            Long currentDoctorId = (Long) session.getAttribute("doctorId");
            boolean hasAccess = medicalRecordMapper.selectCount(
                    new QueryWrapper<MedicalRecord>()
                            .eq("patient_id", patientId)
                            .eq("doctor_id", currentDoctorId)
                            .eq("status", 1)
            ) > 0;
            if (!hasAccess) {
                return Result.fail("您未接诊过该患者，无权访问其就诊历史");
            }
        }
        
        Page<MedicalRecord> pageParam = new Page<>(page, size);
        QueryWrapper<MedicalRecord> wrapper = new QueryWrapper<MedicalRecord>()
                .eq("patient_id", patientId)
                .eq("status", 1);

        if (departmentId != null) {
            wrapper.eq("department_id", departmentId);
        }
        if (doctorId != null) {
            wrapper.eq("doctor_id", doctorId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("visit_date", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("visit_date", endDate + " 23:59:59");
        }
        wrapper.orderByDesc("visit_date");

        Page<MedicalRecord> result = medicalRecordMapper.selectPage(pageParam, wrapper);
        fillMedicalRecordNames(result.getRecords());

        List<VisitRecordVo> voList = result.getRecords().stream().map(record -> {
            VisitRecordVo vo = new VisitRecordVo();
            vo.setRecordId(record.getId());
            vo.setVisitDate(record.getVisitDate());
            vo.setDoctorName(record.getDoctorName());
            vo.setDepartmentName(record.getDepartmentName());
            vo.setDiagnosis(record.getDiagnosis());
            Prescription p = prescriptionMapper.selectOne(
                    new QueryWrapper<Prescription>().eq("medical_record_id", record.getId()).last("LIMIT 1"));
            if (p != null) {
                List<PrescriptionItem> items = prescriptionItemMapper.selectList(
                        new QueryWrapper<PrescriptionItem>().eq("prescription_id", p.getId()));
                String summary = items.stream().map(item -> {
                    Medicine med = medicineMapper.selectById(item.getMedicineId());
                    return med != null ? med.getName() : "未知药品";
                }).collect(Collectors.joining(", "));
                vo.setPrescriptionSummary(summary);
            }
            return vo;
        }).toList();

        return Result.ok(voList, result.getTotal());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建PatientInfoVo（含就诊统计）
     */
    private PatientInfoVo buildPatientInfoVo(Patient patient) {
        return buildPatientInfoVo(patient, false);
    }

    /**
     * 构建PatientInfoVo（含就诊统计）
     * @param patient 患者实体
     * @param showFullIdCard 是否显示完整身份证
     */
    private PatientInfoVo buildPatientInfoVo(Patient patient, boolean showFullIdCard) {
        PatientInfoVo vo = new PatientInfoVo();
        vo.setAccount(patient.getAccount());
        vo.setUsername(patient.getUsername());
        vo.setName(patient.getName());
        vo.setGender(patient.getGender());
        vo.setAge(patient.getAge());
        // 手机号脱敏：默认只显示前3位+****+后4位
        vo.setPhone(maskPhone(patient.getPhone()));
        vo.setPhoneMasked(true);
        vo.setAddress(patient.getAddress());
        vo.setAvatar(patient.getAvatar());
        vo.setIdCardVerified(showFullIdCard);

        // 身份证脱敏处理：默认只显示前3位+****+后4位
        if (showFullIdCard) {
            vo.setIdCard(patient.getIdCard());
        } else {
            vo.setIdCard(maskIdCard(patient.getIdCard()));
        }

        // 统计就诊次数
        Long totalVisits = medicalRecordMapper.selectCount(
                new QueryWrapper<MedicalRecord>().eq("patient_id", patient.getAccount()));
        vo.setTotalVisits(totalVisits);

        // 最近就诊时间
        MedicalRecord latestRecord = medicalRecordMapper.selectOne(
                new QueryWrapper<MedicalRecord>()
                        .eq("patient_id", patient.getAccount())
                        .orderByDesc("visit_date")
                        .last("LIMIT 1"));
        if (latestRecord != null) {
            vo.setLastVisitDate(latestRecord.getVisitDate());
        }

        return vo;
    }

    /**
     * 填充病历中的关联名称
     */
    private void fillMedicalRecordNames(List<MedicalRecord> records) {
        for (MedicalRecord record : records) {
            Patient patient = this.getById(record.getPatientId());
            if (patient != null) {
                record.setPatientName(patient.getName());
            }
            Doctor doctor = doctorMapper.selectById(record.getDoctorId());
            if (doctor != null) {
                record.setDoctorName(doctor.getName());
            }
            if (record.getDepartmentId() != null) {
                Department dept = departmentMapper.selectById(record.getDepartmentId());
                if (dept != null) {
                    record.setDepartmentName(dept.getName());
                }
            }
        }
    }

    /**
     * 身份证脱敏：前3位 + **** + 后4位
     * 如 110101200001011234 → 110***********1234
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 7) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 手机号脱敏：前3位 + **** + 后4位
     * 如 13800001234 → 138****1234
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    @Override
    public Result getIdCard(String password, HttpSession session) {
        String account = (String) session.getAttribute("account");
        if (account == null) {
            return Result.fail("请先登录");
        }
        Patient patient = this.getById(account);
        if (patient == null) {
            return Result.fail("用户不存在");
        }

        // 验证密码
        boolean match = MatchPassword.match(password, patient.getPassword());
        if (!match) {
            return Result.fail("密码不正确");
        }

        // 返回完整身份证
        Map<String, Object> data = new HashMap<>();
        data.put("idCard", patient.getIdCard());
        return Result.ok(data);
    }
}
