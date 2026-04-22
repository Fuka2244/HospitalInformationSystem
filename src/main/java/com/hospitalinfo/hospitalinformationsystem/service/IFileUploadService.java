package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface IFileUploadService {

    /**
     * 上传用户头像
     * @param file 头像文件
     * @param session 当前会话
     * @return 上传结果（含头像访问URL）
     */
    Result uploadAvatar(MultipartFile file, HttpSession session);

    /**
     * 获取当前用户头像URL
     * @param session 当前会话
     * @return 头像URL
     */
    Result getAvatar(HttpSession session);
}
