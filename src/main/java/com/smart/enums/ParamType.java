package com.smart.enums;

public enum ParamType {
    PARAM(0, "入参"),
    RESULT(1, "返回");


    private final int value;
    private final String displayName;

    ParamType(int value, String displayName) {
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
        for (ParamType type : ParamType.values()) {
            if (type.getValue() == value) {
                return type.name();
            }
        }
        return null;
    }

    public static ParamType getByValue(int value) {
        for (ParamType type : ParamType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }

}
