package com.smart.enums;

public enum StepType {
    UNSTEP(0, "不提级"),
    STEP(1, "提级");

    private final int value;
    private final String displayName;

    StepType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static StepType fromDisplayName(String displayName) {
        for (StepType stepType : StepType.values()) {
            if (stepType.getDisplayName().equals(displayName)) {
                return stepType;
            }
        }
        return null;
    }

    public static StepType getByValue(int value) {
        for (StepType type : StepType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }

}
