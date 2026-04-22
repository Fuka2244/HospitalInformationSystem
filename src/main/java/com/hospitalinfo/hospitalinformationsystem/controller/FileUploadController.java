package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.service.IFileUploadService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * 处理用户头像上传和获取
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final IFileUploadService fileUploadService;

    /**
     * 上传用户头像
     * POST /file/avatar
     */
    @PostMapping("/avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        return fileUploadService.uploadAvatar(file, session);
    }

    /**
     * 获取当前用户头像URL
     * GET /file/avatar
     */
    @GetMapping("/avatar")
    public Result getAvatar(HttpSession session) {
        return fileUploadService.getAvatar(session);
    }
}
