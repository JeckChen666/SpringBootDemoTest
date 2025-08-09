package com.jeckchen.demo.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author JeckChen
 * @version 1.0.0
 * @className TerminalWebSocketHandler.java
 * @description 终端WebSocket处理器
 * @date 2025年01月01日
 */
@Component
public class TerminalWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 存储WebSocket会话和对应的SSH连接
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Session> sshSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> channels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        System.out.println("WebSocket连接建立: " + sessionId);
        
        // 发送连接成功消息
        sendMessage(session, "connected", "WebSocket连接成功，请发送connect指令连接终端");
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().toString();
        
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.get("type").asText();
            
            switch (type) {
                case "connect":
                    handleConnect(session, jsonNode);
                    break;
                case "command":
                    handleCommand(sessionId, jsonNode.get("data").asText());
                    break;
                case "resize":
                    handleResize(sessionId, jsonNode);
                    break;
                case "tab_completion":
                    handleTabCompletion(sessionId, jsonNode.get("data").asText());
                    break;
                default:
                    sendMessage(session, "error", "未知的消息类型: " + type);
            }
        } catch (Exception e) {
            sendMessage(session, "error", "处理消息时发生错误: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
        closeSSHConnection(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        System.out.println("WebSocket连接关闭: " + sessionId);
        
        sessions.remove(sessionId);
        sessionLocks.remove(sessionId);
        closeSSHConnection(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理连接请求
     */
    private void handleConnect(WebSocketSession session, JsonNode jsonNode) {
        String sessionId = session.getId();
        
        try {
            // 对于本地终端，我们直接连接到本地
            String host = "localhost";
            String username = System.getProperty("user.name");
            
            // 创建JSch会话
            JSch jsch = new JSch();
            Session sshSession;
            
            // 检查是否为Windows系统
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows系统，使用PowerShell
                connectLocalWindows(session, sessionId);
            } else {
                // Unix/Linux系统，使用SSH
                sshSession = jsch.getSession(username, host, 22);
                sshSession.setConfig("StrictHostKeyChecking", "no");
                sshSession.connect();
                
                sshSessions.put(sessionId, sshSession);
                
                // 创建shell通道
                ChannelShell channelShell = (ChannelShell) sshSession.openChannel("shell");
                channelShell.setPtyType("xterm");
                channelShell.setPtySize(80, 24, 640, 480);
                
                channels.put(sessionId, channelShell);
                
                // 设置输入输出流
                setupStreams(session, channelShell);
                
                channelShell.connect();
            }
            
            sendMessage(session, "connected", "终端连接成功");
            
        } catch (Exception e) {
            sendMessage(session, "error", "连接终端失败: " + e.getMessage());
        }
    }
    
    /**
     * Windows本地连接
     */
    private void connectLocalWindows(WebSocketSession session, String sessionId) {
        try {
            System.out.println("[DEBUG] 启动Windows本地PowerShell连接: sessionId=" + sessionId);
            // 启动PowerShell进程，使用系统默认编码
            ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass");
            processBuilder.environment().putAll(System.getenv());
            System.out.println("[DEBUG] ProcessBuilder配置完成，启动进程...");
            Process process = processBuilder.start();
            System.out.println("[DEBUG] PowerShell进程启动成功: " + process.isAlive());
            
            // 创建一个模拟的Channel来统一处理
            LocalProcessChannel localChannel = new LocalProcessChannel(process);
            channels.put(sessionId, localChannel);
            System.out.println("[DEBUG] LocalProcessChannel创建并存储完成");
            
            // 设置输入输出流
            System.out.println("[DEBUG] 开始设置输入输出流...");
            setupStreams(session, localChannel);
            System.out.println("[DEBUG] 输入输出流设置完成");
            
        } catch (Exception e) {
            System.out.println("[DEBUG] 启动PowerShell失败: " + e.getMessage());
            e.printStackTrace();
            sendMessage(session, "error", "启动本地PowerShell失败: " + e.getMessage());
        }
    }

    /**
     * 设置输入输出流
     */
    private void setupStreams(WebSocketSession session, Object channelObj) {
        try {
            InputStream inputStream = null;
            InputStream errorStream = null;
            OutputStream outputStream = null;
            
            // 根据类型获取输入输出流
            if (channelObj instanceof Channel) {
                Channel channel = (Channel) channelObj;
                inputStream = channel.getInputStream();
                outputStream = channel.getOutputStream();
                // SSH连接通常将错误输出合并到标准输出
                errorStream = null;
            } else if (channelObj instanceof LocalProcessChannel) {
                LocalProcessChannel localChannel = (LocalProcessChannel) channelObj;
                inputStream = localChannel.getInputStream();
                outputStream = localChannel.getOutputStream();
                errorStream = localChannel.getErrorStream();
            }
            
            if (inputStream == null || outputStream == null) {
                sendMessage(session, "error", "无法获取终端输入输出流");
                return;
            }
            
            final InputStream finalInputStream = inputStream;
            final InputStream finalErrorStream = errorStream;
            
            // 异步读取终端标准输出
            executorService.submit(() -> {
                byte[] buffer = new byte[1024];
                try {
                    boolean isConnected = true;
                    while (isConnected) {
                        // 检查连接状态
                        if (channelObj instanceof Channel) {
                            Channel channel = (Channel) channelObj;
                            isConnected = channel.isConnected() && !channel.isClosed();
                        } else if (channelObj instanceof LocalProcessChannel) {
                            LocalProcessChannel localChannel = (LocalProcessChannel) channelObj;
                            isConnected = localChannel.isConnected() && !localChannel.isClosed();
                        }
                        
                        if (!isConnected) break;
                        
                        // 使用非阻塞读取，但也尝试阻塞读取以处理交互式程序
                        try {
                            int bytesRead = 0;
                            int available = finalInputStream.available();
                            System.out.println("[DEBUG] 输出流检查: available=" + available);
                            
                            if (available > 0) {
                                // 有数据立即可用，直接读取
                                bytesRead = finalInputStream.read(buffer);
                                System.out.println("[DEBUG] 立即读取数据: bytesRead=" + bytesRead);
                            } else {
                                // 没有立即可用的数据，短暂休眠后继续
                                // 避免无限阻塞等待，特别是对于Python等可能不立即输出的程序
                                System.out.println("[DEBUG] 没有立即可用数据，短暂休眠...");
                                Thread.sleep(100);
                            }
                            
                            if (bytesRead > 0) {
                                // 根据操作系统选择合适的字符编码
                                String charset = System.getProperty("os.name").toLowerCase().contains("windows") ? "GBK" : "UTF-8";
                                String output = new String(buffer, 0, bytesRead, charset);
                                System.out.println("[DEBUG] 发送输出到前端: length=" + output.length() + ", content=" + output.replace("\n", "\\n").replace("\r", "\\r"));
                                sendMessage(session, "output", output);
                            }
                        } catch (java.io.InterruptedIOException e) {
                            System.out.println("[DEBUG] 读取超时: " + e.getMessage());
                            // 读取超时，继续循环
                            Thread.sleep(50);
                        }
                    }
                } catch (Exception e) {
                    boolean stillConnected = false;
                    if (channelObj instanceof Channel) {
                        Channel channel = (Channel) channelObj;
                        stillConnected = channel.isConnected();
                    } else if (channelObj instanceof LocalProcessChannel) {
                        LocalProcessChannel localChannel = (LocalProcessChannel) channelObj;
                        stillConnected = localChannel.isConnected();
                    }
                    
                    if (stillConnected) {
                        sendMessage(session, "error", "读取终端输出时发生错误: " + e.getMessage());
                    }
                }
            });
            
            // 如果有错误流，异步读取终端错误输出
            if (finalErrorStream != null) {
                executorService.submit(() -> {
                    byte[] buffer = new byte[1024];
                    try {
                        boolean isConnected = true;
                        while (isConnected) {
                            // 检查连接状态
                            if (channelObj instanceof LocalProcessChannel) {
                                LocalProcessChannel localChannel = (LocalProcessChannel) channelObj;
                                isConnected = localChannel.isConnected() && !localChannel.isClosed();
                            }
                            
                            if (!isConnected) break;
                            
                            // 使用非阻塞读取，但也尝试阻塞读取以处理交互式程序
                            try {
                                int bytesRead = 0;
                                if (finalErrorStream.available() > 0) {
                                    // 有数据立即可用，直接读取
                                    bytesRead = finalErrorStream.read(buffer);
                                } else {
                                    // 没有立即可用的数据，尝试短时间阻塞读取
                                    finalErrorStream.mark(1);
                                    int firstByte = finalErrorStream.read();
                                    if (firstByte != -1) {
                                        buffer[0] = (byte) firstByte;
                                        bytesRead = 1;
                                        // 使用非阻塞方式尝试读取更多数据
                                        int additionalAvailable = finalErrorStream.available();
                                        if (additionalAvailable > 0) {
                                            int additionalBytes = finalErrorStream.read(buffer, 1, Math.min(additionalAvailable, buffer.length - 1));
                                            if (additionalBytes > 0) {
                                                bytesRead += additionalBytes;
                                            }
                                        }
                                    } else {
                                        // 没有数据，短暂休眠
                                        Thread.sleep(50);
                                    }
                                }
                                
                                if (bytesRead > 0) {
                                    // 根据操作系统选择合适的字符编码
                                    String charset = System.getProperty("os.name").toLowerCase().contains("windows") ? "GBK" : "UTF-8";
                                    String errorOutput = new String(buffer, 0, bytesRead, charset);
                                    sendMessage(session, "output", errorOutput); // 错误输出也作为普通输出显示
                                }
                            } catch (java.io.InterruptedIOException e) {
                                // 读取超时，继续循环
                                // 没有数据时短暂休眠，避免CPU占用过高
                                Thread.sleep(50);
                            }
                        }
                    } catch (Exception e) {
                        if (channelObj instanceof LocalProcessChannel) {
                            LocalProcessChannel localChannel = (LocalProcessChannel) channelObj;
                            if (localChannel.isConnected()) {
                                sendMessage(session, "error", "读取终端错误输出时发生错误: " + e.getMessage());
                            }
                        }
                    }
                });
            }
            
        } catch (Exception e) {
            sendMessage(session, "error", "设置终端流时发生错误: " + e.getMessage());
        }
    }

    /**
     * 处理命令
     */
    private void handleCommand(String sessionId, String command) {
        System.out.println("[DEBUG] 处理命令: sessionId=" + sessionId + ", command=" + command.replace("\n", "\\n").replace("\r", "\\r"));
        Object channelObj = channels.get(sessionId);
        if (channelObj != null) {
            try {
                OutputStream outputStream = null;
                boolean isConnected = false;
                
                // 根据类型获取输出流和连接状态
                if (channelObj instanceof Channel) {
                    Channel channel = (Channel) channelObj;
                    System.out.println("[DEBUG] SSH Channel状态: connected=" + channel.isConnected() + ", closed=" + channel.isClosed());
                    if (channel.isConnected()) {
                        outputStream = channel.getOutputStream();
                        isConnected = true;
                    }
                } else if (channelObj instanceof LocalProcessChannel) {
                    LocalProcessChannel localChannel = (LocalProcessChannel) channelObj;
                    System.out.println("[DEBUG] Local Process状态: connected=" + localChannel.isConnected() + ", closed=" + localChannel.isClosed());
                    if (localChannel.isConnected()) {
                        outputStream = localChannel.getOutputStream();
                        isConnected = true;
                    }
                }
                
                if (isConnected && outputStream != null) {
                    System.out.println("[DEBUG] 发送命令到输出流，字节数: " + command.getBytes("UTF-8").length);
                    outputStream.write(command.getBytes("UTF-8"));
                    outputStream.flush();
                    System.out.println("[DEBUG] 命令发送完成并刷新输出流");
                } else {
                    System.out.println("[DEBUG] 无法发送命令: isConnected=" + isConnected + ", outputStream=" + (outputStream != null));
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] 发送命令时发生异常: " + e.getMessage());
                e.printStackTrace();
                WebSocketSession session = sessions.get(sessionId);
                sendMessage(session, "error", "发送命令时发生错误: " + e.getMessage());
            }
        } else {
            System.out.println("[DEBUG] 找不到对应的channel对象: sessionId=" + sessionId);
        }
    }

    /**
     * 处理终端大小调整
     */
    private void handleResize(String sessionId, JsonNode jsonNode) {
        Object channelObj = channels.get(sessionId);
        if (channelObj instanceof ChannelShell) {
            try {
                int cols = jsonNode.get("cols").asInt();
                int rows = jsonNode.get("rows").asInt();
                ((ChannelShell) channelObj).setPtySize(cols, rows, cols * 8, rows * 16);
            } catch (Exception e) {
                WebSocketSession session = sessions.get(sessionId);
                sendMessage(session, "error", "调整终端大小时发生错误: " + e.getMessage());
            }
        }
        // LocalProcessChannel 不支持动态调整大小
    }

    /**
     * 处理Tab补全请求
     */
    private void handleTabCompletion(String sessionId, String currentLine) {
        try {
            WebSocketSession session = sessions.get(sessionId);
            if (session == null) {
                return;
            }
            
            // 获取当前行的最后一个单词（需要补全的部分）
            String[] words = currentLine.trim().split("\\s+");
            String lastWord = words.length > 0 ? words[words.length - 1] : "";
            
            // 检查操作系统类型
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows系统使用PowerShell的Tab补全
                handleWindowsTabCompletion(sessionId, lastWord, currentLine);
            } else {
                // Unix/Linux系统使用bash的Tab补全
                handleUnixTabCompletion(sessionId, lastWord, currentLine);
            }
        } catch (Exception e) {
            System.err.println("处理Tab补全时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理Windows PowerShell的Tab补全
     */
    private void handleWindowsTabCompletion(String sessionId, String lastWord, String currentLine) {
        try {
            WebSocketSession session = sessions.get(sessionId);
            if (session == null) {
                return;
            }
            
            // 使用PowerShell的TabExpansion2命令进行补全
            String tabCompletionScript = String.format(
                "[System.Management.Automation.CommandCompletion]::CompleteInput('%s', %d, $null) | ConvertTo-Json",
                currentLine.replace("'", "''"), currentLine.length()
            );
            
            // 创建临时PowerShell进程执行补全命令
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", tabCompletionScript);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取补全结果
            executorService.submit(() -> {
                try (InputStream inputStream = process.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    StringBuilder result = new StringBuilder();
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        result.append(new String(buffer, 0, bytesRead, "GBK"));
                    }
                    
                    process.waitFor();
                    
                    // 解析补全结果并发送给前端
                    parseAndSendCompletionResult(session, result.toString(), lastWord);
                } catch (Exception e) {
                    System.err.println("读取PowerShell补全结果时发生错误: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("执行PowerShell Tab补全时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理Unix/Linux bash的Tab补全
     */
    private void handleUnixTabCompletion(String sessionId, String lastWord, String currentLine) {
        try {
            WebSocketSession session = sessions.get(sessionId);
            if (session == null) {
                return;
            }
            
            // 简单的文件名补全实现
            java.io.File currentDir = new java.io.File(System.getProperty("user.dir"));
            java.io.File[] files = currentDir.listFiles();
            
            java.util.List<String> completions = new java.util.ArrayList<>();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.getName().startsWith(lastWord)) {
                        completions.add(file.getName());
                    }
                }
            }
            
            // 发送补全结果
            sendMessage(session, "tab_completion_result", objectMapper.writeValueAsString(completions));
        } catch (Exception e) {
            System.err.println("执行Unix Tab补全时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 解析并发送PowerShell补全结果
     */
    private void parseAndSendCompletionResult(WebSocketSession session, String jsonResult, String lastWord) {
        try {
            // 简化处理：如果PowerShell返回复杂的JSON，我们使用简单的文件名补全作为后备
            java.io.File currentDir = new java.io.File(System.getProperty("user.dir"));
            java.io.File[] files = currentDir.listFiles();
            
            java.util.List<String> completions = new java.util.ArrayList<>();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.getName().toLowerCase().startsWith(lastWord.toLowerCase())) {
                        completions.add(file.getName());
                    }
                }
            }
            
            // 添加常用PowerShell命令补全
            String[] commonCommands = {"Get-ChildItem", "Get-Location", "Set-Location", "Get-Process", 
                                     "Get-Service", "Start-Process", "Stop-Process", "Clear-Host", "Get-Help"};
            for (String cmd : commonCommands) {
                if (cmd.toLowerCase().startsWith(lastWord.toLowerCase())) {
                    completions.add(cmd);
                }
            }
            
            sendMessage(session, "tab_completion_result", objectMapper.writeValueAsString(completions));
        } catch (Exception e) {
            System.err.println("解析PowerShell补全结果时发生错误: " + e.getMessage());
        }
    }

    /**
     * 发送消息到WebSocket客户端
     */
    private void sendMessage(WebSocketSession session, String type, String data) {
        if (session == null || !session.isOpen()) {
            return;
        }
        
        String sessionId = session.getId();
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        
        synchronized (lock) {
            try {
                if (session.isOpen()) {
                    // 清理数据中的控制字符，避免xterm.js解析错误
                    String cleanData = data.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
                    
                    // 使用ObjectMapper确保JSON格式正确
                    java.util.Map<String, String> messageMap = new java.util.HashMap<>();
                    messageMap.put("type", type);
                    messageMap.put("data", cleanData);
                    String message = objectMapper.writeValueAsString(messageMap);
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                System.err.println("发送WebSocket消息失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭SSH连接
     */
    private void closeSSHConnection(String sessionId) {
        try {
            Object channelObj = channels.remove(sessionId);
            if (channelObj != null) {
                if (channelObj instanceof Channel) {
                    Channel channel = (Channel) channelObj;
                    if (channel.isConnected()) {
                        channel.disconnect();
                    }
                } else if (channelObj instanceof LocalProcessChannel) {
                    LocalProcessChannel localChannel = (LocalProcessChannel) channelObj;
                    if (localChannel.isConnected()) {
                        localChannel.disconnect();
                    }
                }
            }
            
            Session sshSession = sshSessions.remove(sessionId);
            if (sshSession != null && sshSession.isConnected()) {
                sshSession.disconnect();
            }
        } catch (Exception e) {
            System.err.println("关闭SSH连接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 本地进程通道实现
     */
    private static class LocalProcessChannel {
        private final Process process;
        private boolean connected = true;
        
        public LocalProcessChannel(Process process) {
            this.process = process;
        }
        
        public InputStream getInputStream() throws IOException {
            return process.getInputStream();
        }
        
        public OutputStream getOutputStream() throws IOException {
            return process.getOutputStream();
        }
        
        public InputStream getErrorStream() throws IOException {
            return process.getErrorStream();
        }
        
        public boolean isConnected() {
            return connected && process.isAlive();
        }
        
        public boolean isClosed() {
            return !process.isAlive();
        }
        
        public void disconnect() {
            connected = false;
            process.destroy();
        }
        
        public int getExitStatus() { 
            return process.isAlive() ? -1 : process.exitValue(); 
        }
        
        public boolean isEOF() { 
            return !process.isAlive(); 
        }
    }
}