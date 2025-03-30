package com.smart.enums;

public enum ValueCategory {
    VARIABLE(0, "变量"),
    CONSTANT(1, "常量");
    //EXPRESSION(2, "表达式");

    private final int value;
    private final String displayName;

    ValueCategory(int value, String displayName) {
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
        for (ValueCategory type : ValueCategory.values()) {
            if (type.getValue() == value) {
                return type.name();
            }
        }
        return null;
    }

    public static ValueCategory getByValue(int value) {
        for (ValueCategory type : ValueCategory.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}