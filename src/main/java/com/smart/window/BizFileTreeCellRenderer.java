package com.smart.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class BizFileTreeCellRenderer extends DefaultTreeCellRenderer {
    private final SimpleColoredComponent myComponent;
    private static final Color FILE_COLOR = new JBColor(new Color(0, 102, 204), new Color(87, 163, 219));
    private static final Color URL_COLOR = new JBColor(new Color(0, 128, 0), new Color(98, 150, 85));
    private static final Color SELECTED_TEXT_COLOR = Color.WHITE;
    private static final Color SELECTED_BG_COLOR = new JBColor(
            new Color(33, 66, 131),  // 浅色主题下的深蓝色
            new Color(33, 66, 131)   // 深色主题下保持相同的深蓝色
    );

    public BizFileTreeCellRenderer() {
        myComponent = new SimpleColoredComponent();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                boolean selected, boolean expanded,
                                                boolean leaf, int row, boolean hasFocus) {
        myComponent.clear();
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();

        if (userObject instanceof BizFileInfo) {
            // 文件节点
            BizFileInfo fileInfo = (BizFileInfo) userObject;
            
            // 设置图标
            myComponent.setIcon(IconLoader.getIcon("/icons/biz.svg", BizFileTreeCellRenderer.class));
            
            // 设置文件名
            myComponent.append(fileInfo.getFile().getName(), 
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, 
                FILE_COLOR));
            
            // 设置URL
            if (fileInfo.getUrl() != null && !fileInfo.getUrl().isEmpty()) {
                myComponent.append("  →  ", 
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, 
                        FILE_COLOR));
                myComponent.append(fileInfo.getUrl(), 
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, 
                        URL_COLOR));
            }
        } else {
            // 目录节点
            myComponent.setIcon(expanded ? AllIcons.Nodes.Folder : AllIcons.Nodes.Folder);
            myComponent.append(userObject.toString(), 
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
                JBColor.foreground()));
        }


        // 移除边框
        myComponent.setBorder(null);

        return myComponent;
    }
} 