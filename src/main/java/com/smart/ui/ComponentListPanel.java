package com.smart.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ComponentListPanel extends JPanel {
    private JBList<ComponentItem> componentList;
    private DefaultListModel<ComponentItem> listModel;
    private DefaultListModel<ComponentItem> filteredModel;
    private VisualLayoutPanel visualLayoutPanel;
    private SearchTextField searchField;
    private List<ComponentItem> allItems = new ArrayList<>();

    public ComponentListPanel(VisualLayoutPanel visualLayoutPanel) {
        this.visualLayoutPanel = visualLayoutPanel;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());

        // 创建顶部工具栏
        JPanel topPanel = createTopPanel();

        // 创建列表模型和列表组件
        listModel = new DefaultListModel<>();
        filteredModel = new DefaultListModel<>();
        componentList = new JBList<>(filteredModel);
        componentList.setCellRenderer(new ComponentListCellRenderer());
        componentList.setEmptyText("没有找到组件");

        // 添加双击事件监听器
        componentList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ComponentItem selectedItem = componentList.getSelectedValue();
                    if (selectedItem != null) {
                        visualLayoutPanel.centerComponent(selectedItem.getComponent());
                    }
                }
            }
        });

        // 添加滚动面板
        JBScrollPane scrollPane = new JBScrollPane(componentList);
        scrollPane.setBorder(JBUI.Borders.empty());

        // 添加组件到面板
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 初始更新组件列表
        updateComponentList();
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(JBUI.Borders.empty(5, 8));

        // 创建搜索框
        createSearchField();
        
        // 创建工具栏
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction("刷新", "刷新组件列表", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                updateComponentList();
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
            "ComponentList",
            actionGroup,
            true
        );
        toolbar.setTargetComponent(this);

        // 创建一个面板来包含搜索框和工具栏
        JPanel searchAndToolbarPanel = new JPanel(new BorderLayout());
        searchAndToolbarPanel.add(searchField, BorderLayout.CENTER);
        searchAndToolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);
        
        topPanel.add(searchAndToolbarPanel, BorderLayout.CENTER);
        return topPanel;
    }

    private void createSearchField() {
        searchField = new SearchTextField();
        searchField.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterComponents();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterComponents();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterComponents();
            }
        });
    }

    private void filterComponents() {
        String searchText = searchField.getText().toLowerCase().trim();
        filteredModel.clear();

        for (ComponentItem item : allItems) {
            if (item.toString().toLowerCase().contains(searchText)) {
                filteredModel.addElement(item);
            }
        }
    }

    // 更新组件列表
    public void updateComponentList() {
        allItems.clear();
        listModel.clear();
        filteredModel.clear();

        // 从 VisualLayoutPanel 获取所有组件
        List<Component> components = visualLayoutPanel.getAllComponents();
        
        for (Component component : components) {
            ComponentItem item = new ComponentItem(component);
            allItems.add(item);
            listModel.addElement(item);
            filteredModel.addElement(item);
        }

        // 如果没有组件，显示提示信息
        if (allItems.isEmpty()) {
            componentList.setEmptyText("当前画布没有组件");
        }
    }

    // 组件项数据类
    private static class ComponentItem {
        private final Component component;
        private final Icon icon;

        public ComponentItem(Component component) {
            this.component = component;
            // 获取组件的图标并缩小处理
            if (component instanceof JLayeredPane) {
                JLabel iconLabel = (JLabel) ((JLayeredPane) component).getClientProperty("iconLabel");
                if (iconLabel != null && iconLabel.getIcon() != null) {
                    // 缩小原始图标
                    Icon originalIcon = iconLabel.getIcon();
                    this.icon = new Icon() {
                        @Override
                        public void paintIcon(Component c, Graphics g, int x, int y) {
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            // 绘制缩小的图标
                            g2d.translate(x, y);
                            g2d.scale(0.7, 0.7); // 将图标缩小到原来的0.7倍
                            originalIcon.paintIcon(c, g2d, 0, 0);
                            g2d.dispose();
                        }

                        @Override
                        public int getIconWidth() {
                            return (int) (originalIcon.getIconWidth() * 0.7);
                        }

                        @Override
                        public int getIconHeight() {
                            return (int) (originalIcon.getIconHeight() * 0.7);
                        }
                    };
                } else {
                    this.icon = AllIcons.Nodes.Plugin;
                }
            } else {
                this.icon = AllIcons.Nodes.Plugin;
            }
        }

        public Component getComponent() {
            return component;
        }

        public Icon getIcon() {
            return icon;
        }

        @Override
        public String toString() {
            return component.getName() != null ? component.getName() : component.getClass().getSimpleName();
        }
    }

    // 自定义列表单元格渲染器
    private static class ComponentListCellRenderer extends ColoredListCellRenderer<ComponentItem> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends ComponentItem> list,
                                           ComponentItem value,
                                           int index,
                                           boolean selected,
                                           boolean hasFocus) {
            if (value != null) {
                setIcon(value.getIcon());
                append(value.toString());
                
                // 调整单元格高度
                setPreferredSize(new Dimension(list.getWidth(), 28));  // 减小高度
                
                // 调整图标和文本的间距
                setIconTextGap(6);  // 减小间距
                setIconOpaque(false);
                
                // 设置字体
                setFont(getFont().deriveFont(12f));  // 稍微调小字体
            }
            // 调整边距
            setBorder(JBUI.Borders.empty(3, 6));
        }
    }
} 