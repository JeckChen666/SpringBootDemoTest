package com.jeckchen.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 命令执行请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "命令执行请求")
public class CommandRequest {
    
    @Schema(description = "要执行的命令", example = "ls -la", required = true)
    private String command;

}