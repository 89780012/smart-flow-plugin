package com.smart.ui;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.*;

public class CustomToggleButtonUI extends BasicToggleButtonUI {
    // 使用IDEA默认的背景色
    private static final Color SELECTED_BACKGROUND = new JBColor(
            new Color(214, 214, 214), // 亮色主题
            new Color(73, 75, 77)     // 暗色主题
            );
    private static final Color NORMAL_BACKGROUND = new JBColor(
            new Color(241, 241, 241), // 亮色主题
            new Color(60, 63, 65)     // 暗色主题
            );
    private static final Color HOVER_BACKGROUND = new JBColor(
            new Color(229, 229, 229), // 亮色主题
            new Color(65, 68, 70)     // 暗色主题
            );

    @Override
    public void paint(Graphics g, JComponent c) {
        JToggleButton button = (JToggleButton) c;
        ButtonModel model = button.getModel();
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            // 使用抗锯齿
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制背景
            if (model.isSelected()) {
                g2.setColor(SELECTED_BACKGROUND);
            } else if (model.isRollover()) {
                g2.setColor(HOVER_BACKGROUND);
            } else {
                g2.setColor(NORMAL_BACKGROUND);
            }
                g2.fillRect(0, 0, c.getWidth(), c.getHeight());

            // 绘制图标
            Icon icon = button.getIcon();
            if (icon != null) {
                int x = (c.getWidth() - icon.getIconWidth()) / 2;
                int y = (c.getHeight() - icon.getIconHeight() - g2.getFontMetrics().getHeight()) / 2;
                icon.paintIcon(c, g2, x, y);
            }

            // 绘制文本
            String text = button.getText();
            if (text != null && !text.isEmpty()) {
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int x = (c.getWidth() - textWidth) / 2;
                int y = c.getHeight() - fm.getDescent() - 5;

                // 设置文本颜色
                g2.setColor(new JBColor(
                    new Color(30, 30, 30),    // 亮色主题文字颜色
                    new Color(187, 187, 187)  // 暗色主题文字颜色
                ));
                g2.drawString(text, x, y);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        // 不使用默认的按压效果
    }

    @Override
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }
}
