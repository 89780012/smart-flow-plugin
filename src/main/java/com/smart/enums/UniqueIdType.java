package com.smart.enums;

public enum UniqueIdType {
    UUID("UUID"),
    SNOWFLAKE("雪花算法");

    private final String displayName;

    UniqueIdType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 