package com.hospitalinfo.hospitalinformationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemConfigDto {
    @NotNull(message = "配置ID不能为空")
    private Long id;
    @NotBlank(message = "配置键不能为空")
    private String configKey;
    @NotBlank(message = "配置值不能为空")
    private String configValue;
    private String configType;
    private String description;
}