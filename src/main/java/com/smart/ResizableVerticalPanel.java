package com.smart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ResizableVerticalPanel extends JPanel {
    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    
    private final JComponent targetComponent;
    private final int resizePosition;
    private int startY;
    private int startHeight;
    private final JPanel handle;
    private static final int HANDLE_HEIGHT = 1;
    
    public ResizableVerticalPanel(JComponent target, int position) {
        this.targetComponent = target;
        this.resizePosition = position;
        setLayout(new BorderLayout());
        
        // 创建拖拽手柄
        handle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 可以在这里自定义手柄的外观
            }
        };
        handle.setPreferredSize(new Dimension(-1, HANDLE_HEIGHT));
        handle.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
        
        // 添加鼠标事件监听
        handle.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startY = e.getYOnScreen();
                startHeight = targetComponent.getHeight();
            }
        });
        
        handle.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int diff = e.getYOnScreen() - startY;
                int newHeight;
                if (resizePosition == TOP) {
                    newHeight = startHeight - diff;
                } else {
                    newHeight = startHeight + diff;
                }
                onResized(newHeight);
            }
        });
        
        // 根据位置添加组件
        if (resizePosition == TOP) {
            add(handle, BorderLayout.NORTH);
            add(targetComponent, BorderLayout.CENTER);
        } else {
            add(targetComponent, BorderLayout.CENTER);
            add(handle, BorderLayout.SOUTH);
        }
    }
    
    // 添加这个方法供子类重写
    protected void onResized(int newHeight) {
        // 默认实现
        targetComponent.setPreferredSize(new Dimension(targetComponent.getWidth(), newHeight));
        targetComponent.revalidate();
    }
}