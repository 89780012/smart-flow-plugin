package com.smart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ResizablePanel extends JPanel {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    private final int resizeHandle;
    private final int handleWidth = 5;
    private boolean isResizing = false;
    private int lastMouseX;
    private int minWidth = 220;
    private int maxWidth = 800;

    public ResizablePanel(JComponent content, int resizeHandle, int minWidth, int maxWidth) {
        this(content, resizeHandle);
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public ResizablePanel(JComponent content, int resizeHandle) {
        this.resizeHandle = resizeHandle;
        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);

        MouseAdapter resizeAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isResizing = true;
                lastMouseX = e.getXOnScreen();
                System.out.println("鼠标按下，开始调整大小");
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isResizing = false;
                System.out.println("鼠标释放，结束调整大小");
                setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isResizing) {
                    int dx = e.getXOnScreen() - lastMouseX;
                    int newWidth;
                    
                    if (resizeHandle == LEFT) {
                        newWidth = getPreferredSize().width - dx;
                    } else {
                        newWidth = getPreferredSize().width + dx;
                    }
                    
                    newWidth = Math.max(minWidth, Math.min(maxWidth, newWidth));
                    setPreferredSize(new Dimension(newWidth, getPreferredSize().height));
                    revalidate();
                    lastMouseX = e.getXOnScreen();
                    System.out.println("正在调整大小，新宽度：" + newWidth);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                updateCursor(e.getPoint());
                //System.out.println("鼠标进入组件");
            }
        
            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                //System.out.println("鼠标离开组件");
            }
        
            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getPoint());
            }
        
            private void updateCursor(Point p) {
                if (isOverResizeHandle(p)) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    //System.out.println("鼠标移动到调整大小区域");
                } else {
                    setCursor(Cursor.getDefaultCursor());
                    //System.out.println("鼠标移出调整大小区域");
                }
            }
        };

        addMouseListener(resizeAdapter);
        addMouseMotionListener(resizeAdapter);
    }

    private boolean isOverResizeHandle(Point p) {
        if (resizeHandle == LEFT) {
            return p.x <= handleWidth;
        } else {
            return p.x >= getWidth() - handleWidth;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 绘制一个细线作为调整大小的视觉提示
        g.setColor(UIManager.getColor("Separator.foreground"));
        if (resizeHandle == LEFT) {
            g.drawLine(0, 0, 0, getHeight());
        } else {
            g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        }
    }
}
