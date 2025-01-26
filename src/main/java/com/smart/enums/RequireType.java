package com.smart.enums;

public enum RequireType {
    no(0, "否"),
    yes(1, "是");
  
    private final int value;
    private final String displayName;

    RequireType(int value, String displayName) {
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
        for (RequireType type : RequireType.values()) {
            if (type.getValue() == value) {
                return type.name();
            }
        }
        return null;
    }

    public static RequireType getByValue(int value) {
        for (RequireType type : RequireType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}
