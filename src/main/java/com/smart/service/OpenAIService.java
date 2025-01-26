package com.smart.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.Messages;
import com.smart.bean.ChatMessage;
import com.smart.bean.Tool;
import com.smart.cache.PluginCache;
import com.smart.settings.SmartPluginSettings;
import com.smart.settings.SmartPluginSettingsConfigurable;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;

public class OpenAIService {
    private static final String API_URL = SmartPluginSettings.API_DOMAIN2 + "/v1/chat/completions";
    private static final Gson gson = new Gson();
    
    // 最大历史消息数
    private static final int MAX_HISTORY_SIZE = 10;
    
    // 使用ChatMessage存储历史消息
    private final List<ChatMessage> messageHistory = new CopyOnWriteArrayList<>();

    // 系统提示词
    private String systemPrompt;

    // 添加工具列表
    private final List<Tool> tools = new ArrayList<>();

    public interface ChatCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public OpenAIService() {
        this(null);
    }
    
    public OpenAIService(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public void sendMessage(String message, ChatCallback callback) {
        new SwingWorker<Void, String>() {
            private StringBuilder fullResponse = new StringBuilder();
            
            @Override
            protected Void doInBackground() throws Exception {
                Map<String, Object> requestBody = new HashMap<>();

                String tempAPIURL = API_URL;
                String licenseURL =  "Bearer " + SmartPluginSettings.getInstance().getLicenseKey();
                String model = SmartPluginSettings.getInstance().getOpenAIModel();
                boolean enableOpenAI = SmartPluginSettings.getInstance().isEnableOpenAI();
                if(PluginCache.isValidLicense && enableOpenAI && !SmartPluginSettings.getInstance().getOpenAIBaseUrl().isEmpty()){
                    tempAPIURL = SmartPluginSettings.getInstance().getOpenAIBaseUrl() + "/v1/chat/completions";
                    licenseURL =  SmartPluginSettings.getInstance().getOpenAIAuthKey();
                    model = SmartPluginSettings.getInstance().getOpenAIModel();
                }

                // 如果model为空，则给出提示
                if(StringUtils.isBlank(model)){
                    SwingUtilities.invokeLater(() -> {
                        int result = Messages.showDialog(
                            "未配置AI模型，请先在设置中配置模型信息。",
                            "配置提示",
                            new String[]{"前往设置", "取消"},
                            0,
                            Messages.getWarningIcon()
                        );
                        
                        if (result == 0) {
                            // 打开设置界面
                            ShowSettingsUtil.getInstance().showSettingsDialog(
                                PluginCache.project,
                                SmartPluginSettingsConfigurable.class
                            );
                        }
                    });
                    throw new RuntimeException("未配置AI模型，请先在设置中配置模型信息。");
                }

                requestBody.put("model", model);
                requestBody.put("stream", true);
                
                // 添加tools到请求体
                if (!tools.isEmpty()) {
                    requestBody.put("tools", tools);
                }

                // 构建消息列表
                List<Map<String, String>> messages = new ArrayList<>();
                
                // 添加系统提示词
                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    messages.add(createMessage("system", systemPrompt));
                }

                // 添加历史消息
                for (ChatMessage chatMessage : messageHistory) {
                    messages.add(createMessage(
                        chatMessage.isUser() ? "user" : "assistant",
                        chatMessage.getContent()
                    ));
                }

                // 添加用户新消息
                Map<String, String> userMessage = createMessage("user", message);
                messages.add(userMessage);
                
                requestBody.put("messages", messages);

                try {
                    URL url = new URL(tempAPIURL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", licenseURL);
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder aiResponse = new StringBuilder();
                        
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                
                                if (data.equals("[DONE]")) {
                                    continue;
                                }
                                
                                try {
                                    Map<String, Object> response = gson.fromJson(data, Map.class);
                                    if (response != null && response.containsKey("choices")) {
                                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                                        if (choices != null && !choices.isEmpty()) {
                                            Map<String, Object> choice = choices.get(0);
                                            
                                            // 处理工具调用
                                            if (choice.containsKey("delta")) {
                                                Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                                                
                                                // 检查是否有工具调用
//                                                if (delta.containsKey("tool_calls")) {
//                                                    List<Map<String, Object>> toolCalls =
//                                                        (List<Map<String, Object>>) delta.get("tool_calls");
//                                                    handleToolCalls(toolCalls, callback);
//                                                }
                                                
                                                String content = (String) delta.get("content");
                                                if (content != null) {
                                                    aiResponse.append(content);
                                                    publish(content);
                                                }
                                            }
                                        }
                                    }
                                } catch (JsonSyntaxException e) {
                                    System.err.println("Invalid JSON data: " + data);
                                    callback.onError("JSON解析错误: " + e.getMessage() + "\nData: " + data);
                                } catch (Exception e) {
                                    System.err.println("Error processing response: " + e.getMessage());
                                    callback.onError("处理响应错误: " + e.getMessage());
                                }
                            }
                        }
                        
                        // 将用户消息和AI响应添加到历史记录
                        addToHistory(new ChatMessage(message, true));
                        addToHistory(new ChatMessage(aiResponse.toString(), false));
                    }
                } catch (IOException e) {
                    System.err.println("Network error: " + e.getMessage());
                    callback.onError("网络错误: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Unexpected error: " + e.getMessage());
                    callback.onError("未预期的错误: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    callback.onResponse(chunk);
                }
            }
        }.execute();
    }
    
    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
    
    // 添加消息到历史记录,并控制历史记录大小
    private void addToHistory(ChatMessage message) {
        messageHistory.add(message);
        while (messageHistory.size() > MAX_HISTORY_SIZE) {
            messageHistory.remove(0);
        }
    }
    
    // 清空历史记录
    public void clearHistory() {
        messageHistory.clear();
    }
    
    // 获取当前历史记录
    public List<ChatMessage> getHistory() {
        return new ArrayList<>(messageHistory);
    }
    
    // 设置历史记录
    public void setHistory(List<ChatMessage> history) {
        messageHistory.clear();
        if (history != null) {
            messageHistory.addAll(history);
        }
    }
    
    // 添加方法用于更新系统提示词
    public void updateSystemPrompt(String newSystemPrompt) {
        this.systemPrompt = newSystemPrompt;
    }

    // 添加工具注册方法
    public void registerTool(Tool tool) {
        tools.add(tool);
    }

    // 添加工具调用处理方法
    private void handleToolCalls(List<Map<String, Object>> toolCalls, ChatCallback callback) {
        for (Map<String, Object> toolCall : toolCalls) {
            try {
                String functionName = ((Map<String, String>)toolCall.get("function")).get("name");
                String arguments = ((Map<String, String>)toolCall.get("function")).get("arguments");
                
                // 这里实现具体的工具调用逻辑
                // 可以通过函数名查找对应的处理器并执行
                // 执行结果可以通过callback返回
                
                callback.onResponse("\n[Tool Call] " + functionName + " with args: " + arguments + "\n");
            } catch (Exception e) {
                callback.onError("工具调用失败: " + e.getMessage());
            }
        }
    }
} 