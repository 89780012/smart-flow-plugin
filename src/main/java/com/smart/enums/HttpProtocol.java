package com.smart.enums;

public enum HttpProtocol {
    JSON("application/json"),
    // XML("application/xml"),
    FORM("application/x-www-form-urlencoded"),
    MULTIPART("multipart/form-data");
    // TEXT("text/plain"),
    // HTML("text/html");

    private final String value;

    HttpProtocol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * 根据值获取枚举
     */
    public static HttpProtocol fromValue(String value) {
        for (HttpProtocol protocol : HttpProtocol.values()) {
            if (protocol.getValue().equals(value)) {
                return protocol;
            }
        }
        return JSON; // 默认返回 JSON
    }
}
