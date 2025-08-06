package com.jeckchen.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@PropertySource(value = "classpath:git.properties", ignoreResourceNotFound = true)
public class SystemInfoController {

    @Value("${app.build.time}")
    private String buildTime;
    
    @Value("${git.branch:unknown}")
    private String gitBranch;
    
    @Value("${git.commit.id.abbrev:unknown}")
    private String gitCommitId;
    
    @Value("${git.commit.time:unknown}")
    private String gitCommitTime;

    @GetMapping("/system-info")
    public String getSystemInfo(Model model) {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        model.addAttribute("osName", osBean.getName());
        model.addAttribute("osVersion", osBean.getVersion());
        model.addAttribute("availableProcessors", osBean.getAvailableProcessors());
        // Add more system info as needed
        return "system-info";
    }

    @ResponseBody
    @GetMapping("/api/system/usage")
    public Map<String, Object> getSystemUsage() {
        Map<String, Object> result = new HashMap<>();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = -1;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
        }
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        long usedHeapMemory = heapMemoryUsage.getUsed();
        long maxHeapMemory = heapMemoryUsage.getMax();
        long usedNonHeapMemory = nonHeapMemoryUsage.getUsed();
        long maxNonHeapMemory = nonHeapMemoryUsage.getMax();
        // JVM信息
        String jvmName = ManagementFactory.getRuntimeMXBean()
                                          .getVmName();
        String jvmVersion = ManagementFactory.getRuntimeMXBean()
                                             .getVmVersion();
        long startTime = ManagementFactory.getRuntimeMXBean()
                                          .getStartTime();
        long uptime = ManagementFactory.getRuntimeMXBean()
                                       .getUptime();
        int threadCount = ManagementFactory.getThreadMXBean()
                                           .getThreadCount();
        result.put("cpuLoad", cpuLoad);
        result.put("usedHeapMemory", usedHeapMemory);
        result.put("maxHeapMemory", maxHeapMemory);
        result.put("usedNonHeapMemory", usedNonHeapMemory);
        result.put("maxNonHeapMemory", maxNonHeapMemory);
        result.put("jvmName", jvmName);
        result.put("jvmVersion", jvmVersion);
        result.put("jvmStartTime", startTime);
        result.put("jvmUptime", uptime);
        result.put("threadCount", threadCount);
        // log.info("result: {}", result);
        System.out.println(result);
        return result;
    }
    
    @ResponseBody
    @GetMapping("/api/build-info")
    public Map<String, Object> getBuildInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("buildTime", buildTime);
        result.put("gitBranch", gitBranch);
        result.put("gitCommitId", gitCommitId);
        result.put("gitCommitTime", gitCommitTime);
        return result;
    }
}