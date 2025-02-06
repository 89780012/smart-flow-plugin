package com.smart.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.smart.ResizablePanel;
import com.intellij.openapi.vfs.VirtualFile;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import com.intellij.ide.util.PropertiesComponent;

public class SidebarPanel extends JPanel {
    private static final String SIDEBAR_WIDTH_KEY = "smart.sidebar.width";
    private CardLayout cardLayout = new CardLayout();
    private JPanel contentPanel;
    private boolean isPanelVisible = false;
    private static final int PANEL_WIDTH = 450;
    private static final int MAX_PANEL_WIDTH = 800;
    private static final int MIN_PANEL_WIDTH = 40;
    private static int NEW_WIDTH = 450;
    private VisualLayoutPanel visualLayoutPanel;
    //前端属性配置
    private Map<String,Object> propertyMap;
    private String bizId;
    private JPanel centerPanel;
    private VirtualFile file;
    private JPanel sidebarButtonsPanel;
    private JToggleButton selectedButton;
    private ResizablePanel resizablePanel;

    public SidebarPanel(Map<String,Object> propertyMap, VisualLayoutPanel visualLayoutPanel ,JPanel centerPanel, VirtualFile file) {
        this.file = file;
        this.centerPanel = centerPanel;
        this.propertyMap = propertyMap;
        this.bizId = propertyMap.get("id").toString();
        this.visualLayoutPanel = visualLayoutPanel;
        
        // 读取保存的宽度
        NEW_WIDTH = PropertiesComponent.getInstance().getInt(SIDEBAR_WIDTH_KEY, PANEL_WIDTH);
        // 创建主面板内容
        JPanel mainContent = createMainContent();
        resizablePanel = new ResizablePanel(mainContent, ResizablePanel.LEFT, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
        // 初始化时使用最小宽度，但不影响后续拖拽
        resizablePanel.setPreferredSize(new Dimension(MIN_PANEL_WIDTH, -1));
        // 修改宽度变化监听
        resizablePanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (isPanelVisible) {
                    int currentWidth = resizablePanel.getWidth();
                    // 只在实际拖拽时(宽度大于最小值)才处理
                    if (currentWidth > MIN_PANEL_WIDTH) {
                        if (currentWidth > MAX_PANEL_WIDTH) {
                            currentWidth = MAX_PANEL_WIDTH;
                            resizablePanel.setPreferredSize(new Dimension(currentWidth, -1));
                            resizablePanel.revalidate();
                        }
                        // 保存当前宽度
                        NEW_WIDTH = currentWidth;
                        PropertiesComponent.getInstance().setValue(SIDEBAR_WIDTH_KEY, currentWidth, PANEL_WIDTH);
                    }
                }
            }
        });
        
        // 添加左侧边框，因为是从右向左展开
        resizablePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new JBColor(Gray._200, Gray._80)),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        
        // 将可调整大小的面板添加到centerPanel的东部
        centerPanel.add(resizablePanel, BorderLayout.EAST);
        centerPanel.revalidate(); // 确保初始布局正确
    }

    /**
     * 获取面板是否显示
     */
    public boolean isPanelVisible() {
        return isPanelVisible;
    }


    /**
     * 设置面板显示状态
     */
    public void setPanelVisible(boolean visible) {
        contentPanel.setVisible(visible);
        if (!visible) {
            resizablePanel.setPreferredSize(new Dimension(MIN_PANEL_WIDTH, -1));
        } else {
            // 显示时使用保存的宽度
            int width = Math.max(NEW_WIDTH, MIN_PANEL_WIDTH);
            width = Math.min(width, MAX_PANEL_WIDTH);
            resizablePanel.setPreferredSize(new Dimension(width, -1));
        }
        
        SwingUtilities.invokeLater(() -> {
            resizablePanel.revalidate();
            centerPanel.revalidate();
            centerPanel.repaint();
        });
        isPanelVisible = visible;
    }

    // 创建属性标签页
    private JPanel createPropertiesPanel() {
        return new PropertyPanel(propertyMap,file);
    }

    // 创建组件标签页
    private JPanel createComponentsPanel() {
        ComponentListPanel componentListPanel = new ComponentListPanel(visualLayoutPanel);
        return componentListPanel;
    }

    // 创建数据标签页
    private JPanel createDataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(null);
        
        // 创建说明标签
        JLabel instructionLabel = new JLabel("选择画布中的组件以查看其配置");
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        instructionLabel.setBorder(null);
        
        panel.add(instructionLabel, BorderLayout.CENTER);
        
        // 添加组件选择监听
        if (visualLayoutPanel != null) {
            visualLayoutPanel.addComponentSelectionListener(componentId -> {
                panel.removeAll();
                if (componentId != null) {
                    ComponentConfigPanel configPanel = new ComponentConfigPanel(
                        componentId,
                        bizId,
                        visualLayoutPanel.getProject(),
                        visualLayoutPanel.getCanvasPanel(),
                        visualLayoutPanel.getCurrentFile()
                    );
                    panel.add(configPanel, BorderLayout.CENTER);
                } else {
                    panel.add(instructionLabel, BorderLayout.CENTER);
                }
                panel.revalidate();
                panel.repaint();
            });
        }
        
        return panel;
    }

    // 添加助方法来查找组件
    private Component findComponentByName(Container container, String name) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof ComponentListPanel) {
                return comp;
            }
            if (comp instanceof Container) {
                Component found = findComponentByName((Container) comp, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // 创建主要内容的方法
    private JPanel createMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(null);

        // 使用初始化的 cardLayout
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBorder(null);
        
        // 添加各个内容面板
        contentPanel.add(createPropertiesPanel(), "属性");
        contentPanel.add(createComponentsPanel(), "组件");
        contentPanel.add(createDataPanel(), "数据");
        
        // 创建右侧按钮面板
        sidebarButtonsPanel = createSidebarButtons();

        // 内容面板在左侧，按钮面板在右侧
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(sidebarButtonsPanel, BorderLayout.EAST);
        // 默认隐藏内容
        contentPanel.setVisible(false);
        return mainPanel;
    }

    private JPanel createSidebarButtons() {
        JPanel tabPanel = new JPanel();
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
        tabPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, JBColor.border()));
        tabPanel.setPreferredSize(new Dimension(40, -1));

        JToggleButton btnProperties = createTabButton("属性", AllIcons.Nodes.Property, false);
        JToggleButton btnComp = createTabButton("组件", AllIcons.Nodes.Plugin, false);
        JToggleButton btnData = createTabButton("数据", AllIcons.Nodes.DataSchema, false);

        tabPanel.add(btnProperties);
        tabPanel.add(btnComp);
        tabPanel.add(btnData);
        tabPanel.add(Box.createVerticalGlue());
        return tabPanel;
    }

    private JToggleButton createTabButton(String tooltip, Icon icon, boolean selected) {
        // 创建按钮
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

        // 设置按钮属性
        button.setLayout(new BorderLayout());
        button.setSelected(selected);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(40, 40));
        button.setMaximumSize(new Dimension(40, 40));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        // 设置文字体
        button.setFont(new Font(button.getFont().getName(), Font.PLAIN, 10));
        button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        button.setBackground(null);
        // 设置图标和文字的位置关系
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        
        // 重构点击事件处理
        button.addActionListener(e -> {
            if (selectedButton == button) {
                // 切换当前面板的显示/隐藏状态
                boolean newVisible = !isPanelVisible();
                button.setSelected(newVisible);
                updateButtonAppearance(button, newVisible);
                setPanelVisible(newVisible);
                
                if(newVisible) {
                    // 显示对应的卡片
                    cardLayout.show(contentPanel, tooltip);
                }
            } else {
                // 切换到新的面板
                if (selectedButton != null) {
                    selectedButton.setSelected(false);
                    updateButtonAppearance(selectedButton, false);
                }
                
                selectedButton = button;
                button.setSelected(true);
                updateButtonAppearance(button, true);
                
                // 确保面板显示并切换到对应的卡片
                setPanelVisible(true);
                cardLayout.show(contentPanel, tooltip);
                
                // 特殊处理组件面板
                if (tooltip.equals("组件")) {
                    ComponentListPanel componentListPanel = (ComponentListPanel) findComponentByName(contentPanel, "ComponentListPanel");
                    if (componentListPanel != null) {
                        componentListPanel.updateComponentList();
                    }
                }
            }
            
            // 强制重绘以确保UI更新
            contentPanel.revalidate();
            contentPanel.repaint();
        });
        
        // 添加鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.isSelected()) {
                    button.setBackground(new Color(0, 120, 215, 10));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!button.isSelected()) {
                    button.setBackground(null);
                }
            }
        });
        
        return button;
    }

    private void updateButtonAppearance(JToggleButton button, boolean selected) {
        if (selected) {
            button.setBackground(new Color(0, 120, 215, 20));
        } else {
            button.setBackground(null);
        }
    }
}
