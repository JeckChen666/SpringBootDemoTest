package com.jeckchen.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 会话执行命令响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话执行命令响应")
public class SessionExecuteResponse {
    
    @Schema(description = "是否执行成功", example = "true")
    private Boolean success;
    
    @Schema(description = "执行结果输出")
    private String output;
    
    @Schema(description = "错误信息")
    private String error;
    
    @Schema(description = "会话ID", example = "session-123")
    private String sessionId;
    
    @Schema(description = "命令", example = "pwd")
    private String command;
    
    @Schema(description = "执行时间（毫秒）", example = "500")
    private Long executionTime;
}