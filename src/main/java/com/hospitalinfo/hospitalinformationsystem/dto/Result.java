package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Result<T> {
    private Boolean success;
    private String errorMsg;
    private T data;
    private Long total;

    public static <T> Result<T> ok(){
        Result<T> r = new Result<>();
        r.setSuccess(true);
        return r;
    }
    public static <T> Result<T> ok(T data){
        Result<T> r = new Result<>();
        r.setSuccess(true);
        r.setData(data);
        return r;
    }
    public static <T> Result<T> ok(List<?> data, Long total){
        Result<T> r = new Result<>();
        r.setSuccess(true);
        r.setData((T) data);
        r.setTotal(total);
        return r;
    }
    public static <T> Result<T> fail(String errorMsg){
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setErrorMsg(errorMsg);
        return r;
    }
    public static <T> Result<T> error(String errorMsg){
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setErrorMsg(errorMsg);
        return r;
    }
}
