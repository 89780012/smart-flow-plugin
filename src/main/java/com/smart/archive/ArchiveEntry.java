package com.smart.archive;

public class ArchiveEntry {
    private String id;
    private String filePath;
    private String description;

    private String type ; // 1. 本地  2: 远程
    private String time;
    private String storedUrl;
    
    public ArchiveEntry(String id, String filePath, String description, String time,String type) {
        this.id = id;
        this.filePath = filePath;
        this.description = description;
        this.time = time;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getStoredUrl() {
        return storedUrl;
    }

    public void setStoredUrl(String storedUrl) {
        this.storedUrl = storedUrl;
    }
} 