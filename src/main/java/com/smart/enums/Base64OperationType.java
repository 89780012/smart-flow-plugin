package com.smart.enums;

public enum Base64OperationType {
    ENCODE("编码"),
    DECODE("解码");

    private final String displayName;

    Base64OperationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 