package com.smart.enums;

public enum PaginationType {
    YES("是"),
    NO("否");

    private final String displayName;

    PaginationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}