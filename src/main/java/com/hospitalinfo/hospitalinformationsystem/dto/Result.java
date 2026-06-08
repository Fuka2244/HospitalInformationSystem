package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.hospitalinfo.hospitalinformationsystem.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class Result<T> {
    private Boolean success;
    private String code;
    private String errorMsg;
    private T data;
    private Long total;
    private String path;
    private String traceId;
    private LocalDateTime timestamp;

    public static <T> Result<T> ok(){
        Result<T> r = new Result<>();
        r.setSuccess(true);
        r.setCode(ErrorCode.SUCCESS.getCode());
        r.setTimestamp(LocalDateTime.now());
        return r;
    }
    public static <T> Result<T> ok(T data){
        Result<T> r = new Result<>();
        r.setSuccess(true);
        r.setCode(ErrorCode.SUCCESS.getCode());
        r.setData(data);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }
    public static <T> Result<T> ok(List<?> data, Long total){
        Result<T> r = new Result<>();
        r.setSuccess(true);
        r.setCode(ErrorCode.SUCCESS.getCode());
        r.setData((T) data);
        r.setTotal(total);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }
    public static <T> Result<T> fail(String errorMsg){
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setCode(ErrorCode.BUSINESS_ERROR.getCode());
        r.setErrorMsg(errorMsg);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }
    public static <T> Result<T> error(String errorMsg){
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setCode(ErrorCode.SYSTEM_ERROR.getCode());
        r.setErrorMsg(errorMsg);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String errorMsg) {
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setCode(errorCode.getCode());
        r.setErrorMsg(errorMsg);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static <T> Result<T> error(ErrorCode errorCode, String errorMsg) {
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setCode(errorCode.getCode());
        r.setErrorMsg(errorMsg);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }
}
