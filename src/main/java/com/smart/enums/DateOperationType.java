package com.smart.enums;

public enum DateOperationType {
    GET_CURRENT_DATE(1, "获取当前日期"),
    FORMAT_DATE(2, "日期格式化");

    private final int value;
    private final String displayName;

    DateOperationType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}