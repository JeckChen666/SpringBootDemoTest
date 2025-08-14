package com.jeckchen.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 通用API响应DTO
 */
@Data
@Schema(description = "通用API响应")
public class ApiResponse<T> {
    
    @Schema(description = "是否成功", example = "true")
    private Boolean success;
    
    @Schema(description = "响应消息", example = "Operation completed successfully")
    private String message;
    
    @Schema(description = "响应数据")
    private T data;
    
    @Schema(description = "时间戳", example = "1641024000000")
    private Long timestamp;
    
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResponse(Boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }
    
    public ApiResponse(Boolean success, String message, T data) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message);
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}