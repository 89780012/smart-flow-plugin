package com.smart.window;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class ProjectTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Color URL_COLOR = new Color(0, 128, 0);  // URL使用绿色
    private static final Color METHOD_COLOR = new Color(152, 118, 170);  // 方法名使用紫色
    private static final Color NAME_COLOR = new Color(128, 128, 128);  // 名称使用灰色

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        
        // 修改选中状态的背景色处理
        if (selected && hasFocus) {
            // 只有在选中且有焦点时才使用选中背景色
            panel.setBackground(UIManager.getColor("Tree.selectionBackground"));
            panel.setOpaque(true);
        } else {
            panel.setOpaque(false);
        }
        
        if (userObject instanceof String) {
            // 处理项目根节点
            JLabel rootLabel = new JLabel((String)userObject);
            rootLabel.setIcon(AllIcons.Nodes.Project);
            rootLabel.setFont(JBUI.Fonts.label());
            setLabelColors(rootLabel, selected);
            panel.add(rootLabel);
        } else if (userObject instanceof BizFileInfo) {
            // 处理biz文件节点
            BizFileInfo bizFileInfo = (BizFileInfo) userObject;
            
            // 创建文件名标签
            JLabel fileLabel = new JLabel(bizFileInfo.getFile().getName());
            fileLabel.setIcon(IconLoader.getIcon("/icons/biz.svg", getClass()));
            fileLabel.setFont(JBUI.Fonts.label());
            setLabelColors(fileLabel, selected);
            panel.add(fileLabel);
            
            // 显示name
            if (bizFileInfo.getName()!=null && !bizFileInfo.getName().isEmpty()) {
                JLabel nameLabel = new JLabel(" (" + bizFileInfo.getName() + ")");
                nameLabel.setFont(JBUI.Fonts.label());
                setLabelColors(nameLabel, selected, NAME_COLOR);
                panel.add(nameLabel);
            }
            
            // 显示method和protocol
            if (bizFileInfo.getMethod()!=null && !bizFileInfo.getMethod().isEmpty()) {
                JLabel methodLabel = new JLabel(" [" + bizFileInfo.getMethod() + "]");
                methodLabel.setFont(JBUI.Fonts.label());
                setLabelColors(methodLabel, selected, METHOD_COLOR);
                panel.add(methodLabel);
            }
            
            // 显示URL
            if (bizFileInfo.getUrl()!=null && !bizFileInfo.getUrl().isEmpty()) {
                JLabel urlLabel = new JLabel(" → " + bizFileInfo.getUrl());
                urlLabel.setFont(JBUI.Fonts.label());
                setLabelColors(urlLabel, selected, URL_COLOR);
                panel.add(urlLabel);
            }
            
        } else if (userObject instanceof CompressedDirectory) {
            // 处理压缩的目录节点
            CompressedDirectory compDir = (CompressedDirectory) userObject;
            JLabel dirLabel = new JLabel(compDir.getDisplayPath());
            dirLabel.setIcon(expanded ? AllIcons.Nodes.Package : AllIcons.Nodes.Package);
            dirLabel.setFont(JBUI.Fonts.label());
            setLabelColors(dirLabel, selected);
            panel.add(dirLabel);
        } else if (userObject instanceof VirtualFile) {
            // 处理目录节点
            VirtualFile file = (VirtualFile) userObject;
            JLabel dirLabel = new JLabel(file.getName());
            dirLabel.setIcon(expanded ? AllIcons.Nodes.Folder : AllIcons.Nodes.Folder);
            dirLabel.setFont(JBUI.Fonts.label());
            setLabelColors(dirLabel, selected);
            panel.add(dirLabel);
        }
        
        // 设置边距
        panel.setBorder(JBUI.Borders.empty(1, 3));
        
        return panel;
    }
    
    private void setLabelColors(JLabel label, boolean selected) {
        setLabelColors(label, selected, null);
    }
    
    private void setLabelColors(JLabel label, boolean selected, Color defaultColor) {
        if (selected) {
            // 选中状态使用高对比度的颜色
            label.setForeground(UIManager.getColor("Tree.selectionForeground"));
        } else {
            // 未选中状态使用原来的颜色
            label.setForeground(defaultColor != null ? defaultColor : UIManager.getColor("Tree.foreground"));
        }
    }
} 