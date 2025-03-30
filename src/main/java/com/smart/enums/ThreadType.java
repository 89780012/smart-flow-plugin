package com.smart.enums;

public enum ThreadType {

    SYNC(0, "同步"),
    ASYNC(1, "异步");
    private final int value;
    private final String displayName;

    ThreadType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String getNameByValue(int value) {
        for (ThreadType type : ThreadType.values()) {
            if (type.getValue() == value) {
                return type.name();
            }
        }
        return null;
    }

    public static ThreadType getByValue(int value) {
        for (ThreadType type : ThreadType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}
