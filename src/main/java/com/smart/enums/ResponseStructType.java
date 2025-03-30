package com.smart.enums;

public enum ResponseStructType {
    STANDARD(1, "标准结构(code/message/data)"),
    SIMPLE_OBJECT(2, "简单对象");

    private final int value;
    private final String displayName;

    ResponseStructType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }
}