package com.jeckchen.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SystemInfoController {

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
        return result;
    }
}