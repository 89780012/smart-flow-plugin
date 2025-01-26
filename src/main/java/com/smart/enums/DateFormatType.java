package com.smart.enums;

public enum DateFormatType {
    YYYY_MM_DD(1, "yyyy-MM-dd", "年-月-日"),
    YYYY_MM_DD_HH_MM_SS(2, "yyyy-MM-dd HH:mm:ss", "年-月-日 时:分:秒"),
    YYYY_MM_DD_HH_MM(3, "yyyy-MM-dd HH:mm", "年-月-日 时:分"),
    YYYYMMDD(4, "yyyyMMdd", "年月日"),
    YYYYMMDDHHMMSS(5, "yyyyMMddHHmmss", "年月日时分秒");

    private final int value;
    private final String format;
    private final String displayName;

    DateFormatType(int value, String format, String displayName) {
        this.value = value;
        this.format = format;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getFormat() {
        return format;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}