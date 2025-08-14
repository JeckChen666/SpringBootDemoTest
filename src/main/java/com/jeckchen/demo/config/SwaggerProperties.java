package com.jeckchen.demo.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("swagger")
public class SwaggerProperties {
    /** 标题 */
    private String title = "";

    /** 描述 */
    private String description = "";

    /** 版本 */
    private String version = "";

    /** 许可证 */
    private String license = "";

    /** 许可证URL */
    private String licenseUrl = "";

}