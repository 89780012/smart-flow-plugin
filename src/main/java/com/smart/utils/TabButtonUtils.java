package com.smart.utils;

import com.smart.ui.CustomToggleButtonUI;

import javax.swing.*;
import java.awt.*;

public class TabButtonUtils {

    public static JToggleButton createTabButton(String tooltip, Icon icon,boolean selected){
        // 创建带图标和文字的按钮
        JToggleButton button = new JToggleButton(tooltip, icon) {
            @Override
            public void updateUI() {
                super.updateUI();
                setUI(new CustomToggleButtonUI());
            }
            // 确保按钮完全不透明
            @Override
            public boolean isOpaque() {
                return true;
            }
        };

        // 设置按钮基本属性
        button.setSelected(selected);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(40, 40));
        button.setMaximumSize(new Dimension(40, 40));
        button.setFocusPainted(false);
        button.setBorderPainted(false);

        // 设置图标和文字的位置关系
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);

        // 设置文字体
        button.setFont(new Font(button.getFont().getName(), Font.PLAIN, 10));

        // 设置按钮样式
        button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // 设置默认背景色
        button.setBackground(null);
        return button;
    }
}
