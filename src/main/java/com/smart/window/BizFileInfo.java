package com.smart.window;

import com.intellij.openapi.vfs.VirtualFile;

public class BizFileInfo {
    private final VirtualFile file;
    private String url = "";
    private String method = "";
    private String name = "";
    
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
} 