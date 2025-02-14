package com.smart.enums;

public enum RandomType {
    NUMBER("数字"),
    STRING("字符串");

    private final String displayName;

    RandomType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 