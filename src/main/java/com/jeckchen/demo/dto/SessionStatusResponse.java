package com.jeckchen.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 会话状态响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话状态响应")
public class SessionStatusResponse {
    
    @Schema(description = "会话ID", example = "session-123")
    private String sessionId;
    
    @Schema(description = "会话状态", example = "ACTIVE")
    private String status;
    
    @Schema(description = "创建时间戳", example = "1641024000000")
    private Long createdTime;
    
    @Schema(description = "最后活动时间戳", example = "1641024300000")
    private Long lastActiveTime;
    
    @Schema(description = "当前工作目录", example = "/home/user")
    private String currentDirectory;
    
    public SessionStatusResponse() {}
    
    public SessionStatusResponse(String sessionId, String status, Long createdTime, Long lastActiveTime, String currentDirectory) {
        this.sessionId = sessionId;
        this.status = status;
        this.createdTime = createdTime;
        this.lastActiveTime = lastActiveTime;
        this.currentDirectory = currentDirectory;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }
    
    public Long getLastActiveTime() {
        return lastActiveTime;
    }
    
    public void setLastActiveTime(Long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    public String getCurrentDirectory() {
        return currentDirectory;
    }
    
    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }
}