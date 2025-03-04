package com.smart.enums;

public enum BasicDataType {
    STRING("字符串"),
    INTEGER("整数"),
    LONG("长整数"),
    DOUBLE("双精度浮点数"),
    BIGDECIMAL("高精度数值"),
    BOOLEAN("布尔值");

    private final String displayName;

    BasicDataType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 