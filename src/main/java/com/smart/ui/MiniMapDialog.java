package com.smart.ui;

import com.intellij.ui.JBColor;
import com.smart.bean.Connection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class MiniMapDialog extends JDialog {
    private Point initialClick;
    private final VisualLayoutPanel visualLayoutPanel;
    private final JPanel miniMapPanel;
    private static final Color TITLE_BAR_COLOR = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(218, 220, 224);
    private static final int CORNER_RADIUS = 8;

    public MiniMapDialog(Frame owner, VisualLayoutPanel visualLayoutPanel) {
        super(owner, false);
        this.visualLayoutPanel = visualLayoutPanel;

        // 获取画布的宽高比
        JPanel canvasPanel = visualLayoutPanel.getCanvasPanel();
        double aspectRatio = (double) canvasPanel.getWidth() / canvasPanel.getHeight();
        
        // 设置窗口大小
        int titleBarHeight = 30; // 标题栏高度
        int contentWidth = 280;  // 内容区域宽度
        int contentHeight = (int)(contentWidth / aspectRatio); // 内容区域高度
        
        // 窗口总高度需要加上标题栏高度
        setSize(contentWidth, contentHeight + titleBarHeight);
        
        // 设置窗口样式
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(CORNER_RADIUS),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // 创建标题栏
        JPanel titleBar = createTitleBar();

        // 创建小地图面板并设置首选大小
        miniMapPanel = createMiniMapPanel();
        miniMapPanel.setPreferredSize(new Dimension(contentWidth, contentHeight));
        miniMapPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 布局
        setLayout(new BorderLayout());
        add(titleBar, BorderLayout.NORTH);
        add(miniMapPanel, BorderLayout.CENTER);

        // 添加画布变化监听
        setupViewportListener();
        
        // 定时刷新小地图
        Timer timer = new Timer(100, e -> miniMapPanel.repaint());
        timer.start();
    }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制标题栏背景
                GradientPaint gradient = new GradientPaint(
                    0, 0, TITLE_BAR_COLOR,
                    0, getHeight(), new Color(240, 240, 240)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight() * 2, CORNER_RADIUS, CORNER_RADIUS);
                
                // 绘制底部分隔线
                g2d.setColor(BORDER_COLOR);
                g2d.drawLine(5, getHeight() - 1, getWidth() - 5, getHeight() - 1);
            }
        };
        titleBar.setPreferredSize(new Dimension(0, 30));
        titleBar.setBorder(new EmptyBorder(0, 8, 0, 8));

        // 标题文本
        JLabel titleLabel = new JLabel("画布预览");
        titleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(50, 50, 50));
        
        // 关闭按钮
        JButton closeButton = createCloseButton();

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(closeButton, BorderLayout.EAST);

        // 添加拖动功能
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - initialClick.x, p.y + e.getY() - initialClick.y);
            }
        });

        return titleBar;
    }

    private JButton createCloseButton() {
        JButton closeButton = new JButton("×") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2d.setColor(new Color(229, 71, 71));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(240, 71, 71));
                } else {
                    g2d.setColor(new Color(130, 130, 130));
                }
                
                g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth("×")) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString("×", x, y);
            }
        };
        
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setBorder(null);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        closeButton.addActionListener(e -> {
            setVisible(false);
            if (visualLayoutPanel != null) {
                visualLayoutPanel.toggleMiniMap(false);
            }
        });
        
        return closeButton;
    }

    private JPanel createMiniMapPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (visualLayoutPanel != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    JPanel canvasPanel = visualLayoutPanel.getCanvasPanel();

                    if (canvasPanel != null) {
                        // 设置渲染品质
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        
                        double scale = (double) getWidth() / canvasPanel.getWidth();
                        int yOffset = (getHeight() - (int)(canvasPanel.getHeight() * scale)) / 2;

                        // 绘制背景
                        g2d.setColor(Color.WHITE);
                        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                        // 保存状态
                        AffineTransform oldTransform = g2d.getTransform();
                        g2d.translate(0, yOffset);
                        g2d.scale(scale, scale);

                        // 创建缓冲图像
                        BufferedImage image = new BufferedImage(
                            canvasPanel.getWidth(),
                            canvasPanel.getHeight(),
                            BufferedImage.TYPE_INT_ARGB
                        );
                        Graphics2D imageG2d = image.createGraphics();
                        setupGraphics(imageG2d);

                        // 绘制组件
                        drawComponents(imageG2d, canvasPanel);
                        
                        // 绘制连接线
                        drawConnections(imageG2d);

                        imageG2d.dispose();
                        g2d.drawImage(image, 0, 0, null);
                        g2d.setTransform(oldTransform);

                        // 绘制视口
                        drawViewport(g2d, scale, yOffset, canvasPanel);
                    }
                }
            }
        };
    }

    private void setupGraphics(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(new Color(250, 250, 250));
        g2d.fillRect(0, 0, 3000, 3000);
    }

    private void drawComponents(Graphics2D g2d, JPanel canvasPanel) {
        for (Component comp : canvasPanel.getComponents()) {
            if (comp instanceof JLayeredPane) {
                Rectangle bounds = comp.getBounds();
                // 绘制组件阴影
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRoundRect(bounds.x + 6, bounds.y + 6, bounds.width - 8, bounds.height - 8, 6, 6);
                // 绘制组件本体
                g2d.setColor(new Color(0, 120, 215));
                g2d.fillRoundRect(bounds.x + 5, bounds.y + 5, bounds.width - 10, bounds.height - 10, 6, 6);
            }
        }
    }

    private void drawConnections(Graphics2D g2d) {
        g2d.setColor(new Color(0, 120, 215));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Connection conn : visualLayoutPanel.getConnections()) {
            g2d.drawLine(conn.startPoint.x, conn.startPoint.y, conn.endPoint.x, conn.endPoint.y);
        }
    }

    private void drawViewport(Graphics2D g2d, double scale, int yOffset, JPanel canvasPanel) {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
            JScrollPane.class,
            canvasPanel
        );

        if (scrollPane != null) {
            Rectangle viewRect = scrollPane.getViewport().getViewRect();
            g2d.setColor(new Color(255, 0, 0, 60));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRect(
                (int)(viewRect.x * scale),
                (int)(viewRect.y * scale) + yOffset,
                (int)(viewRect.width * scale),
                (int)(viewRect.height * scale)
            );
        }
    }

    private void setupViewportListener() {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
            JScrollPane.class,
            visualLayoutPanel.getCanvasPanel()
        );

        if (scrollPane != null) {
            scrollPane.getViewport().addChangeListener(e -> miniMapPanel.repaint());
        }
    }

    // 自定义阴影边框
    private static class ShadowBorder extends javax.swing.border.AbstractBorder {
        private final int radius;

        public ShadowBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制阴影
            for (int i = 0; i < 4; i++) {
                g2d.setColor(new Color(0, 0, 0, 10 - i * 2));
                g2d.draw(new RoundRectangle2D.Float(x + i, y + i, width - 1 - i*2, height - 1 - i*2, radius, radius));
            }

            // 绘制边框
            g2d.setColor(BORDER_COLOR);
            g2d.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));

            g2d.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 4, 4, 4);
        }
    }
}