package com.smart.enums;

public enum SQLOperationType {
    QUERY("查询"),
    UPDATE("更新"),
    INSERT("新增"),
    DELETE("删除");

    private final String displayName;

    SQLOperationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SQLOperationType getByKey(String key) {
        for (SQLOperationType type : SQLOperationType.values()) {
            if (type.name().equals(key)) {
                return type;
            }
        }
        return null;
    }
} 