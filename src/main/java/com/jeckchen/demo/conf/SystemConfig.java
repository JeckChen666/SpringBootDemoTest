package com.jeckchen.demo.conf;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author JeckChen
 * @version 1.0.0
 * @className SystemConfig.java
 * @description 系统配置类
 * @date 2025年08月06日 00:14
 */
@Component
public class SystemConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("=========================================");
        System.out.println("Spring Boot 应用启动完成！");
        System.out.println("=========================================");
    }
}
