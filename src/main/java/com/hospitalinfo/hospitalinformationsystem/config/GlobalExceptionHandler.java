package com.hospitalinfo.hospitalinformationsystem.config;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.exception.BusinessException;
import com.hospitalinfo.hospitalinformationsystem.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return buildResult(e.getErrorCode(), safeMessage(e.getMessage(), e.getErrorCode().getMessage()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("请求体参数校验失败: {}", message);
        return buildResult(ErrorCode.VALIDATION_ERROR, safeMessage(message, ErrorCode.VALIDATION_ERROR.getMessage()), request);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("请求参数绑定失败: {}", message);
        return buildResult(ErrorCode.VALIDATION_ERROR, safeMessage(message, ErrorCode.VALIDATION_ERROR.getMessage()), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", message);
        return buildResult(ErrorCode.VALIDATION_ERROR, safeMessage(message, ErrorCode.VALIDATION_ERROR.getMessage()), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = "缺少必要参数: " + e.getParameterName();
        log.warn(message);
        return buildResult(ErrorCode.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "参数类型错误: " + e.getName();
        log.warn("{} value={}", message, e.getValue());
        return buildResult(ErrorCode.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return buildResult(ErrorCode.BAD_REQUEST, "请求体格式错误，请检查JSON格式", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String message = "不支持的请求方法: " + e.getMethod();
        log.warn(message);
        return buildResult(ErrorCode.METHOD_NOT_ALLOWED, message, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<?> handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("资源不存在: {}", e.getRequestURL());
        return buildResult(ErrorCode.NOT_FOUND, "请求资源不存在", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("上传文件过大: {}", e.getMessage());
        return buildResult(ErrorCode.FILE_ERROR, "上传文件过大，请压缩后重试", request);
    }

    @ExceptionHandler(MultipartException.class)
    public Result<?> handleMultipartException(MultipartException e, HttpServletRequest request) {
        log.warn("文件上传异常: {}", e.getMessage());
        return buildResult(ErrorCode.FILE_ERROR, "文件上传失败，请检查文件格式", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据完整性冲突: {}", e.getMessage());
        return buildResult(ErrorCode.DATA_CONFLICT, "数据已存在或违反唯一性约束", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public Result<?> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        log.error("数据库异常: {}", e.getMessage(), e);
        return buildResult(ErrorCode.DATABASE_ERROR, ErrorCode.DATABASE_ERROR.getMessage(), request);
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public Result<?> handleRedisConnectionFailure(RedisConnectionFailureException e, HttpServletRequest request) {
        log.error("Redis连接异常: {}", e.getMessage(), e);
        return buildResult(ErrorCode.CACHE_ERROR, "缓存服务暂时不可用，请稍后重试", request);
    }

    @ExceptionHandler(KafkaException.class)
    public Result<?> handleKafkaException(KafkaException e, HttpServletRequest request) {
        log.error("Kafka异常: {}", e.getMessage(), e);
        return buildResult(ErrorCode.MESSAGE_QUEUE_ERROR, "异步任务服务暂时不可用，请稍后重试", request);
    }

    @ExceptionHandler(JwtException.class)
    public Result<?> handleJwtException(JwtException e, HttpServletRequest request) {
        log.warn("JWT异常: {}", e.getMessage());
        return buildResult(ErrorCode.UNAUTHORIZED, "认证令牌无效或已过期", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: {}", e.getMessage());
        return buildResult(ErrorCode.BAD_REQUEST, safeMessage(e.getMessage(), ErrorCode.BAD_REQUEST.getMessage()), request);
    }

    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: {}", e.getMessage(), e);
        return buildResult(ErrorCode.SYSTEM_ERROR, "数据处理异常，请稍后重试", request);
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return buildResult(ErrorCode.SYSTEM_ERROR, "操作失败，请稍后重试", request);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("未捕获异常: {}", e.getMessage(), e);
        return buildResult(ErrorCode.SYSTEM_ERROR, "服务器内部错误，请稍后重试", request);
    }

    private Result<?> buildResult(ErrorCode errorCode, String message, HttpServletRequest request) {
        Result<?> result = Result.error(errorCode, message);
        result.setPath(request.getRequestURI());
        result.setTraceId(getOrCreateTraceId(request));
        return result;
    }

    private String getOrCreateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String safeMessage(String message, String fallback) {
        return StringUtils.hasText(message) ? message : fallback;
    }
}
