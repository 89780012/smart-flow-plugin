package com.smart.window;

import com.intellij.openapi.vfs.VirtualFile;
import com.smart.enums.DataType;
import com.smart.enums.RequireType;
import java.util.ArrayList;
import java.util.List;

public class BizFileInfo {
    private final VirtualFile file;
    private String url = "";
    private String method = "";
    private String name = "";
    private List<ParamInfo> params = new ArrayList<>();
    
    public BizFileInfo(VirtualFile file) {
        this.file = file;
    }
    
    public BizFileInfo(VirtualFile file, String url, String method, String name) {
        this.file = file;
        this.url = url;
        this.method = method;
        this.name = name;
    }
    
    // Getters and setters
    public VirtualFile getFile() { return file; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ParamInfo> getParams() { return params; }
    public void setParams(List<ParamInfo> params) { this.params = params; }
    
    // 参数信息内部类
    public static class ParamInfo {
        private String name;
        private String value;
        private DataType type;
        private RequireType required;
        private String example; // 添加示例值字段
        
        public ParamInfo(String name, String value, DataType type, RequireType required, String example) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.required = required;
            this.example = example;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public DataType getType() { return type; }
        public void setType(DataType type) { this.type = type; }
        public RequireType getRequired() { return required; }
        public void setRequired(RequireType required) { this.required = required; }
        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }
    }
} 