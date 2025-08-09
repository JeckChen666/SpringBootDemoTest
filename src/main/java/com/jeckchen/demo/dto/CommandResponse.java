package com.jeckchen.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 命令执行响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "命令执行响应")
public class CommandResponse {
    
    @Schema(description = "是否执行成功", example = "true")
    private Boolean success;
    
    @Schema(description = "执行结果输出")
    private String output;
    
    @Schema(description = "错误信息")
    private String error;
    
    @Schema(description = "退出码", example = "0")
    private Integer exitCode;
    
    @Schema(description = "执行时间（毫秒）", example = "1500")
    private Long executionTime;
}