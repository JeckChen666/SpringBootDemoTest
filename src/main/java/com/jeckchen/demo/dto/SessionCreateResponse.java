package com.jeckchen.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 会话创建响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话创建响应")
public class SessionCreateResponse {
    
    @Schema(description = "是否创建成功", example = "true")
    private Boolean success;
    
    @Schema(description = "会话ID", example = "session-123")
    private String sessionId;
    
    @Schema(description = "消息", example = "Session created successfully")
    private String message;
    
    @Schema(description = "创建时间戳", example = "1641024000000")
    private Long createdTime;
}