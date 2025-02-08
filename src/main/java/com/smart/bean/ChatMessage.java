package com.smart.bean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    private String content;
    private boolean isUser;
    private LocalDateTime timestamp;
    private List<ComponentItem> generatedComponents;

    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = LocalDateTime.now();
        this.generatedComponents = new ArrayList<>();
    }

    public ChatMessage(boolean isUser, String content) {
        this(content, isUser);
    }

    // Getters and setters
    public String getContent() { return content; }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isUser() {
        return isUser;
    }
}