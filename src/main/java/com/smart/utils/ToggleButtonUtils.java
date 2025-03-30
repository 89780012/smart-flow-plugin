package com.smart.utils;

import com.smart.ui.CustomCanvasButtonUI;
import com.smart.ui.CustomToggleButtonUI;

import javax.swing.*;
import java.awt.*;

public class ToggleButtonUtils {

    //带文字
    public static JToggleButton createToggleButton(String tooltip, Icon icon,boolean selected){
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


    //不带文字
    public static JToggleButton createToggleButtonNoText(String tooltip, Icon icon){
        // 使用JToggleButton替代JButton
        JToggleButton button = new JToggleButton();
        button.setUI(new CustomCanvasButtonUI());
        button.setIcon(icon);
        button.setToolTipText(tooltip);

        // 确保按钮不显示任何文本
        button.setText("");
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.CENTER);

        // 设置按钮大小和外观
        button.setPreferredSize(new Dimension(40, 40));
        button.setMaximumSize(new Dimension(40, 40));
        button.setMinimumSize(new Dimension(40, 40));

        // 移除所有默认的按钮装饰
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setMargin(new Insets(0, 0, 0, 0));

        // 设置按钮样式
        button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        button.setBackground(null);
        return button;
    }

    public static void reset(JToggleButton button){
        button.setSelected(false);
        button.setBackground(new Color(255, 255, 255, 0));
        button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }
}
