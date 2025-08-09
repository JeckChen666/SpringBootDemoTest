package com.jeckchen.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 会话执行命令请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话执行命令请求")
public class SessionExecuteRequest {
    
    @Schema(description = "要执行的命令", example = "pwd", required = true)
    private String command;
    
    @Schema(description = "是否等待输出", example = "true", defaultValue = "true")
    private Boolean waitForOutput = true;
    
    @Schema(description = "超时时间（秒）", example = "10", defaultValue = "10")
    private Integer timeoutSeconds = 10;
    
    public SessionExecuteRequest(String command) {
        this.command = command;
    }
}