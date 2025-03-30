package com.smart.window;

import com.intellij.openapi.vfs.VirtualFile;

public class CompressedDirectory {
    private final VirtualFile directory;
    private final String displayPath;
    
    public CompressedDirectory(VirtualFile directory, String displayPath) {
        this.directory = directory;
        this.displayPath = displayPath;
    }
    
    public VirtualFile getDirectory() { 
        return directory; 
    }
    
    public String getDisplayPath() { 
        return displayPath; 
    }
} 