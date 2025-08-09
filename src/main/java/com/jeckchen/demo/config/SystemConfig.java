package com.jeckchen.demo.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.util.Enumeration;

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
        try {
            System.out.println("本机IP地址列表：");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet6Address) continue; // 跳过IPv6地址
                    System.out.println(iface.getDisplayName() + ": " + addr.getHostAddress());
                }
            }
        } catch (Exception e) {
            System.out.println("获取IP地址失败：" + e.getMessage());
        }
        System.out.println("=========================================");
    }
}
