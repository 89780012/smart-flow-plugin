package com.smart.bean;

import com.smart.bean.ComponentProp;

import java.util.List;
import java.util.Map;

public class ComponentInfo implements java.io.Serializable {
    private String name;
    private int x;
    private int y;
    private String id;
    private String type; // 组件类型
    private String description; // 组件描述


    private ComponentProp componentProp; // 组件属性

    public ComponentInfo(String id, String name, int x, int y, String type) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ComponentProp getComponentProp() {
        return componentProp;
    }

    public void setComponentProp(ComponentProp componentProp) {
        this.componentProp = componentProp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
