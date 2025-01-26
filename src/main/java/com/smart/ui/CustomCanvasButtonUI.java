package com.smart.ui;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.*;

public class CustomCanvasButtonUI extends BasicToggleButtonUI {
    // 调亮背景色
    private static final Color SELECTED_BACKGROUND = new JBColor(
            new Color(230, 230, 230), // 亮色主题更亮
            new Color(80, 83, 85)     // 暗色主题稍微调亮
    );
    private static final Color NORMAL_BACKGROUND = new JBColor(
            new Color(250, 250, 250), // 亮色主题更亮
            new Color(68, 71, 73)     // 暗色主题稍微调亮
    );
    private static final Color HOVER_BACKGROUND = new JBColor(
            new Color(240, 240, 240), // 亮色主题
            new Color(73, 76, 78)     // 暗色主题
    );
    
    // 添加按下状态的颜色
    private static final Color PRESSED_BACKGROUND = new JBColor(
            new Color(220, 220, 220), // 亮色主题
            new Color(85, 88, 90)     // 暗色主题
    );

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            // 使用抗锯齿
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制背景 - 修改状态判断逻辑
            if (model.isPressed() || model.isSelected()) {  // 合并pressed和selected状态
                g2.setColor(PRESSED_BACKGROUND);
            } else if (model.isRollover()) {
                g2.setColor(HOVER_BACKGROUND);
            } else {
                g2.setColor(NORMAL_BACKGROUND);
            }
            
            // 绘制圆角矩形背景
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);

            // 绘制图标
            Icon icon = button.getIcon();
            if (icon != null) {
                int x = (c.getWidth() - icon.getIconWidth()) / 2;
                int y = (c.getHeight() - icon.getIconHeight()) / 2;
                
                // 选中状态时图标稍微下移1像素
                if (model.isPressed() || model.isSelected()) {
                    y += 1;
                }
                
                icon.paintIcon(c, g2, x, y);
            }

        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        // 不使用默认的按压效果,由paint方法统一处理
    }

    @Override
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }
}
