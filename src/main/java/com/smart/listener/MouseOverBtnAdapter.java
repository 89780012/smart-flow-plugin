package com.smart.listener;

import com.smart.ui.VisualLayoutPanel;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

// 鼠标悬停在画布上的按钮时  交互效果
public class MouseOverBtnAdapter extends MouseAdapter {

    private JToggleButton button;
    public MouseOverBtnAdapter(JToggleButton button){
        this.button = button;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (!button.isSelected()) {
            button.setBackground(new Color(0, 120, 215, 10));
            button.setBorder(new RoundedBorder(4, new Color(0, 120, 215, 20)));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (!button.isSelected()) {
            button.setBackground(new Color(255, 255, 255, 0));
            button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }
    }

    class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width-1, height-1, radius, radius));
            g2.dispose();
        }
    }

}
