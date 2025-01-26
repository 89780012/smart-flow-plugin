package com.smart.window;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class BizFileListCellRenderer extends JPanel implements ListCellRenderer<BizFileInfo> {
    private final SimpleColoredComponent myComponent;

    public BizFileListCellRenderer() {
        setLayout(new BorderLayout());
        myComponent = new SimpleColoredComponent();
        add(myComponent, BorderLayout.CENTER);
        setBorder(JBUI.Borders.empty(2));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends BizFileInfo> list,
                                                BizFileInfo fileInfo,
                                                int index,
                                                boolean isSelected,
                                                boolean cellHasFocus) {
        myComponent.clear();
        
        if (fileInfo.getFile() == null) {
            // 这是一个分组标题
            myComponent.append(fileInfo.getUrl(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            setBackground(list.getBackground());
            return this;
        }
        
        // 设置图标
        Icon icon = IconLoader.getIcon("/icons/biz.svg", BizFileListCellRenderer.class);
        myComponent.setIcon(icon);
        
        // 设置文件名（使用蓝色）
        myComponent.append(fileInfo.getFile().getName(), 
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new Color(0, 102, 204)));
        
        // 设置URL（如果存在，使用绿色）
        if (fileInfo.getUrl() != null && !fileInfo.getUrl().isEmpty()) {
            myComponent.append("  →  ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            myComponent.append(fileInfo.getUrl(), 
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new Color(0, 128, 0)));
        } else {
            myComponent.append("  →  [No URL]", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
        }

        // 设置选中状态
        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        
        return this;
    }
} 