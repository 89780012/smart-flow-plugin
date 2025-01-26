package com.smart.bean;

import com.smart.bean.Function;

import java.util.Map;

public class Tool {
    private String type = "function";
    private Function function;

    public Tool(Function function) {
        this.function = function;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public Function getFunction() {
        return function;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFunction(Function function) {
        this.function = function;
    }
}

