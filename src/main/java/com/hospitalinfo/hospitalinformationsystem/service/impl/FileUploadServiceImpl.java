package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.hospitalinfo.hospitalinformationsystem.dto.PatientInfoVo;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.Patient;
import com.hospitalinfo.hospitalinformationsystem.mapper.PatientMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IFileUploadService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements IFileUploadService {

    private final PatientMapper patientMapper;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.avatar-max-size:5242880}")
    private long avatarMaxSize; // 默认5MB

    @Override
    public Result uploadAvatar(MultipartFile file, HttpSession session) {
        // 1. 验证登录状态
        String account = (String) session.getAttribute("account");
        if (account == null) {
            return Result.fail("请先登录");
        }

        // 2. 验证文件
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }

        // 3. 验证文件大小
        if (file.getSize() > avatarMaxSize) {
            return Result.fail("头像文件大小不能超过5MB");
        }

        // 4. 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail("只能上传图片文件");
        }

        // 允许的图片格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.fail("文件名不能为空");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
            return Result.fail("仅支持 jpg、jpeg、png、gif、webp、bmp 格式的图片");
        }

        // 5. 保存文件
        String avatarDir = uploadDir + File.separator + "avatars";
        File dir = new File(avatarDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("创建头像目录失败: {}", dir.getAbsolutePath());
            return Result.fail("服务器存储异常，请稍后再试");
        }

        // 删除该用户所有旧头像文件（可能格式不同，如之前是.jpg现在换成了.png）
        File[] oldFiles = dir.listFiles((d, name) -> name.startsWith(account + "."));
        if (oldFiles != null) {
            for (File old : oldFiles) {
                old.delete();
            }
        }

        // 使用 account 作为文件名，方便管理
        String newFilename = account + extension;
        File dest = new File(dir.getAbsolutePath(), newFilename);

        try {
            file.transferTo(dest.toPath());
            log.info("头像保存成功: {}", dest.getAbsolutePath());
        } catch (IOException e) {
            log.error("头像上传失败，目标路径: {}", dest.getAbsolutePath(), e);
            return Result.fail("头像上传失败，请稍后再试");
        }

        // 6. 更新数据库中的头像路径
        String avatarPath = "/uploads/avatars/" + newFilename;
        Patient patient = new Patient();
        patient.setAccount(account);
        patient.setAvatar(avatarPath);
        patientMapper.updateById(patient);

        // 7. 同步更新session中的头像信息
        PatientInfoVo vo = (PatientInfoVo) session.getAttribute("patient");
        if (vo != null) {
            vo.setAvatar(avatarPath);
            session.setAttribute("patient", vo);
        }

        return Result.ok(avatarPath);
    }

    @Override
    public Result getAvatar(HttpSession session) {
        String account = (String) session.getAttribute("account");
        if (account == null) {
            return Result.fail("请先登录");
        }

        Patient patient = patientMapper.selectById(account);
        if (patient == null) {
            return Result.fail("用户不存在");
        }

        return Result.ok(patient.getAvatar());
    }
}
