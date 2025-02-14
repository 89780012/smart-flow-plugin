package com.smart.enums;

import java.math.BigDecimal;

public enum DataType {
    STRING(0, "String"),
    INTEGER(1, "Integer"),

    FLOAT(2,"Float"),
    DOUBLE(3, "Double"),
    BOOLEAN(4, "Boolean"),
    DATE(5, "Date"),

    LONG(6, "Long"),

    ARRAY(7, "Array"),
    OBJECT(8,"Object"),
    BigDecimal(9, "BigDecimal");

    private final int value;
    private final String displayName;

    DataType(int value, String displayName) {
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
        for (DataType type : DataType.values()) {
            if (type.getValue() == value) {
                return type.name();
            }
        }
        return null;
    }

    public static DataType getByValue(int value) {
        for (DataType type : DataType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}
