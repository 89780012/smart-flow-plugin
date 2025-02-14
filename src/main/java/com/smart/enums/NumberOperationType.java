package com.smart.enums;

public enum NumberOperationType {
    ADD("加"),
    SUBTRACT("减"),
    MULTIPLY("乘"),
    DIVIDE("除");

    private final String displayName;

    NumberOperationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 