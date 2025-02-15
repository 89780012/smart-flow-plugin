package com.smart.utils;

import com.intellij.openapi.util.IconLoader;
import com.smart.bean.ComponentItem;
import javax.swing.*;
import java.awt.Image;

public class IconUtils {

    public static Icon getIcon(String iconPath) {
        Icon icon = null;
        try {
            if (iconPath.startsWith("/")) {
                // 处理插件内置资源
                icon = IconLoader.getIcon(iconPath, ComponentItem.class);
            } else {
                // 处理实际项目资源
                ImageIcon originalIcon = new ImageIcon(iconPath);
                Image scaledImage = originalIcon.getImage()
                    .getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("Error loading icon: " + e.getMessage());
            e.printStackTrace();
        }
        return icon;
    }

}
