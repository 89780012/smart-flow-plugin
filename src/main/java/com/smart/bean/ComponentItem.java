package com.smart.bean;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ComponentItem implements Serializable {
    private String name;
    private transient Icon icon;  // 使用 transient 关键字,因为 Icon 可能不是可序列化的
    private String iconPath;  // 新增字段

    private String type;

    private boolean isBeanEdit;
    private String beanRef;
    private String method;
    private String description; //组件描述，用于自定义组件
    private String threadType; //线程类型

    private ComponentConfig config; // 新增config字段

    public ComponentItem(String name, String iconPath,String type,
                         boolean isBeanEdit,String beanRef, String method) {
        this.name = name;
        this.iconPath = iconPath;
        this.type = type;
        this.isBeanEdit = isBeanEdit;
        this.beanRef = beanRef;
        this.method = method;
    }

    public ComponentItem(String name, String iconPath,String type,
                         boolean isBeanEdit,String beanRef, String method,String description) {
        this.name = name;
        this.iconPath = iconPath;
        this.type = type;
        this.isBeanEdit = isBeanEdit;
        this.beanRef = beanRef;
        this.method = method;
        this.description = description;
    }

    public ComponentItem(String name, String iconPath, String type,
                       boolean isBeanEdit, String beanRef, String method, 
                       String description, ComponentConfig config,String threadType) {
        this.name = name;
        this.iconPath = iconPath;
        this.type = type;
        this.isBeanEdit = isBeanEdit;
        this.beanRef = beanRef;
        this.method = method;
        this.description = description;
        this.config = config;
        this.threadType = threadType;
    }

    public String getThreadType() {
        return threadType;
    }

    public void setThreadType(String threadType) {
        this.threadType = threadType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        if (icon == null && iconPath != null) {
            //System.out.println("Attempting to load icon from path: " + iconPath);
            icon = IconLoader.getIcon(iconPath, ComponentItem.class);
            //System.out.println("Icon loaded: " + (icon != null ? "success" : "failed"));
        }
        return icon;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public String getIconPath() {
        return iconPath;
    }

    // 添加这个方法来确保在序列化后可以重新加载图标
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        System.out.println("ComponentItem deserialized: " + name + ", iconPath: " + iconPath);
        if (iconPath != null) {
            icon = IconLoader.getIcon(iconPath, ComponentItem.class);
            System.out.println("Icon reloaded after deserialization: " + (icon != null ? "success" : "failed"));
        }
    }

    public boolean isBeanEdit() {
        return isBeanEdit;
    }

    public void setBeanEdit(boolean beanEdit) {
        isBeanEdit = beanEdit;
    }

    public String getBeanRef() {
        return beanRef;
    }

    public void setBeanRef(String beanRef) {
        this.beanRef = beanRef;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ComponentConfig getConfig() {
        return config;
    }

    public void setConfig(ComponentConfig config) {
        this.config = config;
    }

    public static class ComponentConfig implements Serializable {
        private List<ColumnConfig> header;
        private List<RowConfig> data;

        public ComponentConfig() {
            this.header = new ArrayList<>();
            this.data = new ArrayList<>();
        }

        public ComponentConfig(String threadType) {
            this.header = new ArrayList<>();
            this.data = new ArrayList<>();
        }

        public List<ColumnConfig> getHeader() {
            return header;
        }

        public List<RowConfig> getData() {
            return data;
        }
    }

    public static class ColumnConfig implements Serializable {
        private String name;
        private boolean isEdit;
        private int width;

        public ColumnConfig(String name, boolean isEdit, int width) {
            this.name = name;
            this.isEdit = isEdit;
            this.width = width;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEdit() {
            return isEdit;
        }

        public void setEdit(boolean isEdit) {
            this.isEdit = isEdit;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }
    }

    public static class RowConfig implements Serializable {
        private List<String> columns;

        public RowConfig() {
            this.columns = new ArrayList<>();
        }

        public List<String> getColumns() {
            return columns;
        }
    }
}
