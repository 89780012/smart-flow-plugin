package com.smart.bean;

import java.util.List;

public class DemoItem {
    private String name;
    private String type;
    private String id;
    private List<DemoItem> children;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<DemoItem> getChildren() {
        return children;
    }

    public void setChildren(List<DemoItem> children) {
        this.children = children;
    }
}