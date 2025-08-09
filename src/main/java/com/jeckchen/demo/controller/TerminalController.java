package com.jeckchen.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * @author JeckChen
 * @version 1.0.0
 * @className TerminalController.java
 * @description 终端命令执行控制器
 * @date 2025年01月01日
 */
@RestController
@RequestMapping("/api/terminal")
public class TerminalController {

    // 存储持久化的shell会话
    private static final ConcurrentHashMap<String, ShellSession> sessions = new ConcurrentHashMap<>();
    
    // 会话超时时间（30分钟）
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000L;
    
    // 定期清理过期会话
    static {
        // 每5分钟清理一次过期会话
        java.util.concurrent.Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            try {
                sessions.entrySet().removeIf(entry -> {
                    ShellSession session = entry.getValue();
                    if (!session.isAlive() || session.isExpired()) {
                        session.close();
                        return true;
                    }
                    return false;
                });
            } catch (Exception e) {
                // 忽略清理过程中的错误
            }
        }, 5, 5, java.util.concurrent.TimeUnit.MINUTES);
    }

    /**
     * 执行系统命令（SSE流式输出）
     * @param request 要执行的命令
     * @return SSE流
     */
    @PostMapping("/execute-stream")
    public SseEmitter executeCommandStream(@RequestBody Map<String, String> request) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        String command = request.get("command");
        
        if (command == null || command.trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("命令不能为空"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        // 异步执行命令
        CompletableFuture.runAsync(() -> {
            try {
                // 根据操作系统选择命令执行方式
                ProcessBuilder processBuilder;
                String os = System.getProperty("os.name").toLowerCase();
                
                if (os.contains("win")) {
                    // Windows系统 - 使用PowerShell，设置执行策略和环境
                    processBuilder = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-Command", command);
                } else {
                    // Unix/Linux系统
                    processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
                }
                
                processBuilder.redirectErrorStream(true);
                // 继承系统环境变量，确保PATH中的命令可以被找到
                processBuilder.environment().putAll(System.getenv());
                Process process = processBuilder.start();
                
                // 发送开始事件
                emitter.send(SseEmitter.event().name("start").data("命令开始执行: " + command));
                
                // 实时读取输出
                String encoding = os.contains("win") ? "UTF-8" : "UTF-8";
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), encoding))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        emitter.send(SseEmitter.event().name("output").data(line));
                    }
                }
                
                // 等待进程结束
                int exitCode = process.waitFor();
                
                // 发送结束事件
                Map<String, Object> endData = new HashMap<>();
                endData.put("exitCode", exitCode);
                endData.put("success", exitCode == 0);
                emitter.send(SseEmitter.event().name("end").data(endData));
                emitter.complete();
                
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("执行命令时发生错误: " + e.getMessage()));
                } catch (Exception ex) {
                    // 忽略发送错误
                }
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * 执行系统命令（传统方式，保持兼容性）
     * @param request 要执行的命令
     * @return 执行结果
     */
    @PostMapping("/execute")
    public Map<String, Object> executeCommand(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String command = request.get("command");
        
        if (command == null || command.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "命令不能为空");
            return result;
        }
        
        try {
            // 根据操作系统选择命令执行方式
            ProcessBuilder processBuilder;
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Windows系统 - 使用PowerShell，设置执行策略和环境
                processBuilder = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-Command", command);
            } else {
                // Unix/Linux系统
                processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            }
            
            processBuilder.redirectErrorStream(true);
            // 继承系统环境变量，确保PATH中的命令可以被找到
            processBuilder.environment().putAll(System.getenv());
            Process process = processBuilder.start();
            
            // 设置超时时间为30秒
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                result.put("success", false);
                result.put("error", "命令执行超时（30秒）");
                return result;
            }
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            String encoding = "UTF-8"; // PowerShell使用UTF-8编码
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), encoding))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.exitValue();
            result.put("success", exitCode == 0);
            result.put("exitCode", exitCode);
            result.put("output", output.toString());
            result.put("command", command);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "执行命令时发生错误: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取当前工作目录
     * @return 当前工作目录
     */
    @GetMapping("/pwd")
    public Map<String, Object> getCurrentDirectory() {
        Map<String, Object> result = new HashMap<>();
        try {
            String currentDir = System.getProperty("user.dir");
            result.put("success", true);
            result.put("currentDirectory", currentDir);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "获取当前目录失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取系统信息
     * @return 系统信息
     */
    @GetMapping("/sysinfo")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("osName", System.getProperty("os.name"));
            result.put("osVersion", System.getProperty("os.version"));
            result.put("osArch", System.getProperty("os.arch"));
            result.put("javaVersion", System.getProperty("java.version"));
            result.put("userHome", System.getProperty("user.home"));
            result.put("userDir", System.getProperty("user.dir"));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "获取系统信息失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 创建新的shell会话
     * @return 会话信息
     */
    @PostMapping("/session/create")
    public Map<String, Object> createSession() {
        Map<String, Object> result = new HashMap<>();
        try {
            String sessionId = UUID.randomUUID().toString();
            ShellSession session = new ShellSession(sessionId);
            sessions.put(sessionId, session);
            
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("message", "Shell会话创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "创建会话失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 在指定会话中执行命令
     * @param sessionId 会话ID
     * @param request 请求参数
     * @return 执行结果
     */
    @PostMapping("/session/{sessionId}/execute")
    public Map<String, Object> executeInSession(@PathVariable String sessionId, @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        ShellSession session = sessions.get(sessionId);
        if (session == null || session.isExpired()) {
            if (session != null && session.isExpired()) {
                sessions.remove(sessionId);
            }
            result.put("success", false);
            result.put("error", "会话不存在或已过期");
            return result;
        }
        
        String command = (String) request.get("command");
        Boolean waitForOutput = (Boolean) request.getOrDefault("waitForOutput", true);
        Integer timeoutSeconds = (Integer) request.getOrDefault("timeoutSeconds", 10);
        
        if (command == null || command.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "命令不能为空");
            return result;
        }
        
        try {
            String output = session.executeCommand(command, waitForOutput, timeoutSeconds);
            result.put("success", true);
            result.put("output", output);
            result.put("command", command);
            result.put("sessionId", sessionId);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "执行命令失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取会话状态
     * @param sessionId 会话ID
     * @return 会话状态
     */
    @GetMapping("/session/{sessionId}/status")
    public Map<String, Object> getSessionStatus(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        
        ShellSession session = sessions.get(sessionId);
        if (session == null) {
            result.put("success", false);
            result.put("error", "会话不存在");
            return result;
        }
        
        result.put("success", true);
        result.put("sessionId", sessionId);
        result.put("isAlive", session.isAlive());
        result.put("isExpired", session.isExpired());
        result.put("createdTime", session.getCreatedTime());
        result.put("lastAccessTime", session.getLastAccessTime());
        
        return result;
    }
    
    /**
     * 关闭会话
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> closeSession(@PathVariable String sessionId) {
        Map<String, Object> result = new HashMap<>();
        
        ShellSession session = sessions.remove(sessionId);
        if (session == null) {
            result.put("success", false);
            result.put("error", "会话不存在");
            return result;
        }
        
        try {
            session.close();
            result.put("success", true);
            result.put("message", "会话已关闭");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "关闭会话失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取所有活跃会话
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public Map<String, Object> getAllSessions() {
        Map<String, Object> result = new HashMap<>();
        
        // 清理过期会话
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        java.util.List<Map<String, Object>> sessionList = new java.util.ArrayList<>();
        for (Map.Entry<String, ShellSession> entry : sessions.entrySet()) {
            ShellSession session = entry.getValue();
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", entry.getKey());
            sessionInfo.put("alive", session.isAlive());
            sessionInfo.put("createdTime", session.getCreatedTime());
            sessionInfo.put("lastAccessTime", session.getLastAccessTime());
            sessionList.add(sessionInfo);
        }
        
        result.put("success", true);
        result.put("sessions", sessionList);
        result.put("count", sessionList.size());
        
        return result;
    }
    
    /**
     * Shell会话类，维护持久化的shell进程
     */
    private static class ShellSession {
        private final String sessionId;
        private final Process process;
        private final OutputStream outputStream;
        private final InputStream inputStream;
        private final InputStream errorStream;
        private final long createdTime;
        private volatile long lastAccessTime;
        private volatile boolean closed = false;
        
        public ShellSession(String sessionId) throws IOException {
            this.sessionId = sessionId;
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = this.createdTime;
            
            // 根据操作系统创建shell进程
            ProcessBuilder processBuilder;
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Windows系统 - 使用PowerShell
                processBuilder = new ProcessBuilder("powershell.exe", "-NoExit", "-Command", "-");
            } else {
                // Unix/Linux系统 - 使用bash
                processBuilder = new ProcessBuilder("/bin/bash", "-i");
            }
            
            processBuilder.redirectErrorStream(false);
            processBuilder.environment().putAll(System.getenv());
            
            this.process = processBuilder.start();
            this.outputStream = process.getOutputStream();
            this.inputStream = process.getInputStream();
            this.errorStream = process.getErrorStream();
        }
        
        public String executeCommand(String command, boolean waitForOutput, int timeoutSeconds) throws IOException, InterruptedException {
            if (closed || !process.isAlive()) {
                throw new IOException("Shell会话已关闭或进程已终止");
            }
            
            updateLastAccessTime();
            
            // 发送命令到shell
            outputStream.write((command + "\n").getBytes("UTF-8"));
            outputStream.flush();
            
            if (!waitForOutput) {
                return "命令已发送，不等待输出";
            }
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;
            
            // 使用非阻塞方式读取输出
            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                // 读取标准输出
                if (inputStream.available() > 0) {
                    byte[] buffer = new byte[inputStream.available()];
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        String charset = System.getProperty("os.name").toLowerCase().contains("win") ? "UTF-8" : "UTF-8";
                        output.append(new String(buffer, 0, bytesRead, charset));
                    }
                }
                
                // 读取错误输出
                if (errorStream.available() > 0) {
                    byte[] buffer = new byte[errorStream.available()];
                    int bytesRead = errorStream.read(buffer);
                    if (bytesRead > 0) {
                        String charset = System.getProperty("os.name").toLowerCase().contains("win") ? "UTF-8" : "UTF-8";
                        errorOutput.append(new String(buffer, 0, bytesRead, charset));
                    }
                }
                
                // 短暂休眠避免CPU占用过高
                Thread.sleep(50);
            }
            
            String result = output.toString();
            if (errorOutput.length() > 0) {
                result += "\n[STDERR]\n" + errorOutput.toString();
            }
            
            return result;
        }
        
        public boolean isAlive() {
            return !closed && process.isAlive();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > SESSION_TIMEOUT;
        }
        
        public long getCreatedTime() {
            return createdTime;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        private void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public void close() {
            if (!closed) {
                closed = true;
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                    if (errorStream != null) errorStream.close();
                } catch (IOException e) {
                    // 忽略关闭流时的错误
                }
                
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }
    }
}