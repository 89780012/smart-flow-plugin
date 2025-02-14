package com.smart.utils;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.smart.bean.ComponentItem;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

public class IconUtils {

    public static Icon getIcon(String iconPath) {
        Icon icon = null;
        try {
            if (iconPath.startsWith("/")) {
                // 处理插件内置资源
                icon = IconLoader.getIcon(iconPath, ComponentItem.class);
            } else {
                // 统一使用IconLoader的加载方式
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(iconPath), false);
                if (virtualFile != null && virtualFile.exists()) {
                    // 转换为资源URL格式（例如：file:/C:/path/to.svg -> /C:/path/to.svg）
                    String resourcePath = virtualFile.getUrl().replaceFirst("^file:", "");
                    icon = IconLoader.getIcon(resourcePath, ComponentItem.class);
                    
                    // 如果仍然失败，尝试备用方案
                    if (icon == null) {
                        try (InputStream stream = virtualFile.getInputStream()) {
                            BufferedImage image = ImageIO.read(stream);
                            if (image != null) {
                                icon = new ImageIcon(image);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading icon: " + e.getMessage());
            e.printStackTrace();
        }
        return icon;
    }

}
