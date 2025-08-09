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
    
    public SessionExecuteResponse() {}
    
    public SessionExecuteResponse(Boolean success, String output, String error, String sessionId, String command, Long executionTime) {
        this.success = success;
        this.output = output;
        this.error = error;
        this.sessionId = sessionId;
        this.command = command;
        this.executionTime = executionTime;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public Long getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }
}