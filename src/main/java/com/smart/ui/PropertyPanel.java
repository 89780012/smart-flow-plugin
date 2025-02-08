package com.smart.ui;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.smart.cache.PluginCache;
import com.smart.enums.*;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Map;
import com.smart.event.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.AsyncProcessIcon;
import com.smart.settings.SmartPluginSettings;

import javax.swing.SwingWorker;
import java.util.HashMap;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PropertyPanel extends JPanel {
    private JBTabbedPane tabbedPane;
    private Map<String, Object> propertyMap;
    private boolean isUpdating = false;
    JTextArea previewArea = new JTextArea();

    JPanel propertyPanel;
    JPanel inputParamsPanel;
    JPanel outputConfigPanel;

    VirtualFile file;

    JButton generateBtn = null;


    public PropertyPanel(Map<String, Object> propertyMap, VirtualFile virtualFile) {
        this.propertyMap = propertyMap;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));
        this.file = virtualFile;

        // 创建选项卡面板
        tabbedPane = new JBTabbedPane();
        
        // 添加基本属性页
        propertyPanel = createBasicPropertiesPanel();
        tabbedPane.addTab("基本属性", propertyPanel);
        
        // 添加入参配置页 
        inputParamsPanel = createInputParamsPanel();
        tabbedPane.addTab("入参配置", inputParamsPanel);
        
        // 添加返回值配置页
        outputConfigPanel = createOutputConfigPanel();
        tabbedPane.addTab("返回值配置", outputConfigPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // 初始化加载参数
        loadParamsFromPropertyMap();
    }

    // 添加新的辅助方法来创建属性行，并包含按钮
    private JPanel createPropertyRowWithButton(String label, JTextField textField, String buttonText, ActionListener buttonAction) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BorderLayout(5, 0));
        rowPanel.setBorder(JBUI.Borders.empty(2));

        // 创建标签
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(JBUI.scale(80), JBUI.scale(24)));
        rowPanel.add(jLabel, BorderLayout.WEST);

        // 设置组件首选大小
        textField.setPreferredSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(24)));
        rowPanel.add(textField, BorderLayout.CENTER);

        // 创建按钮
        JButton button = new JButton(buttonText);
        button.addActionListener(buttonAction);
        button.setPreferredSize(new Dimension(JBUI.scale(100), JBUI.scale(24)));
        rowPanel.add(button, BorderLayout.EAST);

        // 确保行面板的最大宽度
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(30)));

        return rowPanel;
    }

    private JPanel createBasicPropertiesPanel() {
        // 使用 BoxLayout 实现垂直布局
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField idField =  createTextField(propertyMap.get("id"));
        // ID 属性
        JPanel idPanel = createPropertyRowWithButton("ID:", idField, "生成 UUID", e -> {
            String uuid = java.util.UUID.randomUUID().toString();
            idField.setText(uuid);
            PluginCache.updateGlobalBizId(propertyMap.get("id").toString(), uuid);

        });
        panel.add(idPanel);
        panel.add(Box.createVerticalStrut(5));  // 添加垂直间距

        // 接口名称
        JTextField nameText = createTextField(propertyMap.get("name"));
        Icon icon = IconLoader.getIcon("/icons/generateName.svg", PropertyPanel.class);
        JButton generateUrlBtn = new JButton("生成URL", icon);
        JPanel nameInputPanel = new JPanel(new BorderLayout(5, 0));
        nameInputPanel.add(nameText, BorderLayout.CENTER);
        nameInputPanel.add(generateUrlBtn, BorderLayout.EAST);

        JPanel namePanel = createPropertyRow("接口名称:", nameInputPanel);
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(5));
        
        // URL
        JTextField urlText = createTextField(propertyMap.get("url"));
        generateUrlBtn.addActionListener(e -> generateUrl(nameText, urlText));
        JPanel urlPanel = createPropertyRow("请求URL:", urlText);
        panel.add(urlPanel);
        panel.add(Box.createVerticalStrut(5));
        
        // 请求方法
        JComboBox<HttpMethod> methodCombo = new JComboBox<>(HttpMethod.values());
        methodCombo.setSelectedItem(HttpMethod.fromValue(String.valueOf(propertyMap.get("method"))));
        methodCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(24)));
        JPanel methodPanel = createPropertyRow("请求方法:", methodCombo);
        panel.add(methodPanel);
        panel.add(Box.createVerticalStrut(5));
        
        // 请求协议
        JComboBox<HttpProtocol> protocolCombo = new JComboBox<>(HttpProtocol.values());
        protocolCombo.setSelectedItem(HttpProtocol.fromValue(String.valueOf(propertyMap.get("protocol"))));
        protocolCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(24)));
        JPanel protocolPanel = createPropertyRow("请求协议:", protocolCombo);
        panel.add(protocolPanel);
        panel.add(Box.createVerticalStrut(10));  // 在保存按钮前添加更大的间距
        
        // 添加保存按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("保存", AllIcons.Actions.MenuSaveall);
        saveButton.addActionListener(e -> {
            propertyMap.put("id", idField.getText());
            propertyMap.put("url", urlText.getText());
            propertyMap.put("name",nameText.getText());
            propertyMap.put("method", methodCombo.getSelectedItem());
            propertyMap.put("protocol", protocolCombo.getSelectedItem());
            // 触发保存事件
            EventBus.getInstance().post(EventType.UPDATE_SOURCE_CODE + "_" + file.getPath(), propertyMap);
            // 成功提示改为气泡提示3秒自动消失
            SwingUtilities.invokeLater(() -> {
                JBPopupFactory.getInstance()
                        .createBalloonBuilder(new JLabel("保存成功"))
                        .setFadeoutTime(3000)
                        .createBalloon()
                        .show(RelativePoint.getNorthWestOf(saveButton), Balloon.Position.above);
            });
        });
        buttonPanel.add(saveButton);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(30)));
        panel.add(buttonPanel);
        // 添加一个弹性空间填充底部
        panel.add(Box.createVerticalGlue());
        
        // 包装在滚动面板中
        JScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setBorder(null);
        // 创建一个包装面板，添加边距
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(JBUI.Borders.empty(10));
        wrapperPanel.add(scrollPane, BorderLayout.CENTER);
        
        return wrapperPanel;
    }

    // 添加新的辅助方法来创建属性行
    private JPanel createPropertyRow(String label, JComponent component) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BorderLayout(5, 0));
        rowPanel.setBorder(JBUI.Borders.empty(2));
        
        // 创建标签
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(JBUI.scale(80), JBUI.scale(24)));
        rowPanel.add(jLabel, BorderLayout.WEST);
        
        // 设置组件首选大小
        component.setPreferredSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(24)));
        rowPanel.add(component, BorderLayout.CENTER);
        
        // 确保行面板的最大宽度
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(30)));
        
        return rowPanel;
    }

    private DefaultTableModel inputModel ;
    private JPanel createInputParamsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 创建参数表格
        String[] columns = {"参数名", "参数类型", "是否必填", "举例", "描述"};
        inputModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return DataType.class;
                if (columnIndex == 2) return RequireType.class;
                return String.class;
            }
            
            // 重写setValueAt方法，确保存储正确的枚举值
            @Override
            public void setValueAt(Object value, int row, int column) {
                if (column == 1 && value instanceof String) {
                    // 处理DataType
                    try {
                        value = DataType.valueOf((String) value);
                    } catch (IllegalArgumentException e) {
                        value = DataType.STRING;
                    }
                } else if (column == 2 && value instanceof String) {
                    // 处理RequireType
                    try {
                        value = RequireType.valueOf((String) value);
                    } catch (IllegalArgumentException e) {
                        value = RequireType.no;
                    }
                }
                super.setValueAt(value, row, column);
            }
        };
        
        JBTable table = new JBTable(inputModel);
        
        // 设置参数类型列的下拉框
        TableColumn typeColumn = table.getColumnModel().getColumn(1);
        JComboBox<DataType> typeCombo = new JComboBox<>(DataType.values());
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof DataType) {
                    value = ((DataType) value).getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        typeColumn.setCellEditor(new DefaultCellEditor(typeCombo) {
            @Override
            public Object getCellEditorValue() {
                return typeCombo.getSelectedItem();
            }
        });

        // 设置是否必填列的下拉框
        TableColumn requireColumn = table.getColumnModel().getColumn(2);
        JComboBox<RequireType> requireCombo = new JComboBox<>(RequireType.values());
        requireCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof RequireType) {
                    value = ((RequireType) value).getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        requireColumn.setCellEditor(new DefaultCellEditor(requireCombo) {
            @Override
            public Object getCellEditorValue() {
                return requireCombo.getSelectedItem();
            }
        });
        
        // 设置自定义渲染器
        table.setDefaultRenderer(DataType.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof DataType) {
                    value = ((DataType) value).getDisplayName();
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        
        table.setDefaultRenderer(RequireType.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof RequireType) {
                    value = ((RequireType) value).getDisplayName();
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // 参数名
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // 参数类型
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // 是否必填
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // 默认值
        table.getColumnModel().getColumn(4).setPreferredWidth(120); // 描述
        
        // 修改工具栏布局为 FlowLayout
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // 原有的添加和删除按钮
        JButton addButton = new JButton("添加参数");
        JButton removeButton = new JButton("删除参数");
        JButton importButton = new JButton("导入参数");
        JButton saveButton = new JButton("保存");


        // 添加导入按钮的点击事件
        importButton.addActionListener(e -> showImportDialog(inputModel));
        saveButton.addActionListener(e -> {
            updateParamsToPropertyMap();
            SwingUtilities.invokeLater(() -> {
                JBPopupFactory.getInstance()
                        .createBalloonBuilder(new JLabel("保存成功"))
                        .setFadeoutTime(3000)
                        .createBalloon()
                        .show(RelativePoint.getNorthWestOf(saveButton), Balloon.Position.above);
            });
        });

        // 保持原有的添加和删除按钮事件处理
        addButton.addActionListener(e -> {
            inputModel.addRow(new Object[]{
                "",                 // 参数名
                DataType.STRING,    // 默认参数类型
                RequireType.no,     // 默认是否必填
                "",                 // 默认值
                ""                  // 描述
            });
        });
        
        removeButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                inputModel.removeRow(selectedRow);
            }
        });
        
        toolbar.add(addButton);
        toolbar.add(removeButton);
        toolbar.add(importButton);
        toolbar.add(saveButton);
        
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JBScrollPane(table), BorderLayout.CENTER);
        
        return panel;
    }


    private DefaultTableModel outputModel;
    private JComboBox<ResponseStructType> responseStructCombo;
    private JPanel createOutputConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // 创建主面板，使用 BorderLayout 而不是 BoxLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 1. 基础配置区域
        JPanel basicConfig = new JPanel(new GridBagLayout());
        basicConfig.setBorder(BorderFactory.createTitledBorder("响应结构配置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 响应结构选择
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        basicConfig.add(new JLabel("响应结构:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        responseStructCombo = new JComboBox<>(ResponseStructType.values());
        responseStructCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ResponseStructType) {
                    setText(((ResponseStructType) value).getDisplayName());
                }
                return this;
            }
        });
        basicConfig.add(responseStructCombo, gbc);
        
        // 2. 创建一个垂直分割面板，包含数据字段定义和预览区域
        JSplitPane contentSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        contentSplitPane.setBorder(null);
        contentSplitPane.setDividerSize(3);  // 设置分隔条的宽度
        
        // 数据字段定义表格面板
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("数据字段定义"));
        
        // 创建表格
        String[] columns = {"字段名", "字段类型(预览用)", "是否提级", "描述", "示例值"};
        outputModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (columnIndex == 2 && aValue instanceof String) {
                    // 将 String 转换为 StepType
                    StepType stepType = StepType.fromDisplayName((String) aValue);
                    if (stepType != null) {
                        aValue = stepType;
                    }
                }
                super.setValueAt(aValue, rowIndex, columnIndex);
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return DataType.class;
                if (columnIndex == 2) return StepType.class;
                return String.class;
            }
        };
        
        JBTable table = new JBTable(outputModel);
        
        // 字段类型 - 数据类型
        TableColumn typeColumn = table.getColumnModel().getColumn(1);
        JComboBox<DataType> typeCombo = new JComboBox<>(DataType.values());
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof DataType) {
                    value = ((DataType) value).getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        typeColumn.setCellEditor(new DefaultCellEditor(typeCombo));
        // 是否提级类别
        TableColumn stepTypeColumn = table.getColumnModel().getColumn(2);
        JComboBox<StepType> stepTypeCombo = new JComboBox<>(StepType.values());
        stepTypeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof StepType) {
                    value = ((StepType) value).getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        stepTypeColumn.setCellEditor(new StepTypeCellEditor(stepTypeCombo));

        // 设置自定义渲染器
        table.setDefaultRenderer(StepType.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof StepType) {
                    value = ((StepType) value).getDisplayName();
                }
                if (value instanceof DataType) {
                    value = ((DataType) value).getDisplayName();
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        // 工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("添加字段");
        JButton removeButton = new JButton("删除字段");
        JButton previewButton = new JButton("生成预览");
        JButton saveButton = new JButton("保存");
        
        toolbar.add(addButton);
        toolbar.add(removeButton);
        toolbar.add(previewButton);
        toolbar.add(saveButton);

        // 按钮事件处理
        addButton.addActionListener(e -> {
            outputModel.addRow(new Object[]{
                "",                 // 字段名
                DataType.STRING,    // 字段类型
                StepType.UNSTEP,    // 提级类型
                "",                 // 描述
                ""                  // 示例值
            });
        });
        
        removeButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                outputModel.removeRow(selectedRow);
            } else {
                Messages.showWarningDialog("请先选择要删除的字段", "警告");
            }
        });
        
        previewButton.addActionListener(e -> {
            updatePreview(((ResponseStructType)responseStructCombo.getSelectedItem()).getDisplayName(),
                    outputModel,
                         previewArea);
        });

        // 添加保存按钮事件处理
        saveButton.addActionListener(e -> {
            updateOutputConfigToPropertyMap();
            SwingUtilities.invokeLater(() -> {
                JBPopupFactory.getInstance()
                        .createBalloonBuilder(new JLabel("保存成功"))
                        .setFadeoutTime(3000)
                        .createBalloon()
                        .show(RelativePoint.getNorthWestOf(saveButton), Balloon.Position.above);
            });
        });

        // 设置表格滚动面板
        JScrollPane tableScrollPane = new JBScrollPane(table);
        // 设置表格的最小高度
        tableScrollPane.setPreferredSize(new Dimension(-1, 200));  // 设置默认高度
        tableScrollPane.setMinimumSize(new Dimension(-1, 150));    // 设置最小高度


        JPanel tipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tipPanel.add(new JLabel("<html><b>提示：</b>提级即将对象属性挂载到上一对象上,在这指data</html>"));
        tablePanel.add(toolbar, BorderLayout.NORTH);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        tablePanel.add(tipPanel, BorderLayout.SOUTH);


        // 预览区域
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("响应示例(提级属性无法显示)"));
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane previewScrollPane = new JBScrollPane(previewArea);
        previewScrollPane.setMinimumSize(new Dimension(-1, 100));  // 设置最小高度
        previewPanel.add(previewScrollPane, BorderLayout.CENTER);
        
        // 将表格和预览区域添加到分割面板
        contentSplitPane.setTopComponent(tablePanel);
        contentSplitPane.setBottomComponent(previewPanel);
        
        // 使用 BorderLayout 布局主面板
        mainPanel.add(basicConfig, BorderLayout.NORTH);
        mainPanel.add(contentSplitPane, BorderLayout.CENTER);
        
        // 设置初始分割位置
        SwingUtilities.invokeLater(() -> {
            contentSplitPane.setDividerLocation(0.6);
        });
        
        return mainPanel;
    }

    /**
     * 更新参数到propertyMap
     */
    private void updateParamsToPropertyMap() {
        
        try {
            DefaultTableModel model = inputModel;

            ArrayNode paramsNode = new ObjectMapper().createArrayNode();
            
            // 遍历表格数据
            for (int i = 0; i < model.getRowCount(); i++) {
                String paramName = (String) model.getValueAt(i, 0);
                if (paramName == null || paramName.trim().isEmpty()) {
                    continue;
                }

                // 获取枚举并转换为value值
                DataType paramType = (DataType) model.getValueAt(i, 1);
                RequireType requiredType = (RequireType) model.getValueAt(i, 2);
                String defaultValue = (String) model.getValueAt(i, 3);
                String description = (String) model.getValueAt(i, 4);

                ObjectNode paramNode = new ObjectMapper().createObjectNode()
                    .put("name", paramName)
                    .put("type", paramType.getValue())  // 存储value值
                    .put("required", requiredType.getValue())  // 存储value值
                    .put("defaultValue", defaultValue)
                    .put("description", description);
        
                paramsNode.add(paramNode);
            }
            
            // 更新到propertyMap
            propertyMap.put("params", paramsNode);
            
            // 触发源码更新事件
            EventBus.getInstance().post(EventType.UPDATE_SOURCE_CODE + "_" + file.getPath(), propertyMap);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从propertyMap加载参数到表格
     */
    private void loadParamsFromPropertyMap() {
        DefaultTableModel model = null;
        try {
            model = inputModel;
            
            // 清空现有数据
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }
            
            // 检查propertyMap是否包含params
            if (!propertyMap.containsKey("params")) {
                propertyMap.put("params", new ObjectMapper().createArrayNode());
                return;
            }
            
            // 从propertyMap加载数据
            Object paramsObj = propertyMap.get("params");
            if (paramsObj instanceof ArrayNode) {
                ArrayNode paramsNode = (ArrayNode) paramsObj;
                for (JsonNode paramNode : paramsNode) {
                    try {
                        // 获取必需字段，设置默认值
                        String name = paramNode.has("name") ? paramNode.get("name").asText() : "";
                        
                        // 通过value值获取枚举实例
                        DataType type = DataType.getByValue(paramNode.get("type").asInt());
                        RequireType required = RequireType.getByValue(paramNode.get("required").asInt());
                        
                        // 获取可选字段
                        String defaultValue = paramNode.has("defaultValue") ? 
                                            paramNode.get("defaultValue").asText() : "";
                        String description = paramNode.has("description") ? 
                                           paramNode.get("description").asText() : "";
                        
                        model.addRow(new Object[]{
                            name,
                            type,
                            required,
                            defaultValue,
                            description
                        });
                    } catch (Exception e) {
                        // 如果单个参数析失败，继续处理下一个
                        e.printStackTrace();
                    }
                }
            }
            // 加载返回值配置
            loadOutputConfigFromPropertyMap();
        } catch (Exception e) {
            e.printStackTrace();
            if(model!= null){
                // 发生异常时清空表格
                while (model.getRowCount() > 0) {
                    model.removeRow(0);
                }
            }

        }
    }

    private void loadOutputConfigFromPropertyMap() {
        try {
            DefaultTableModel model = outputModel;

            // 清空现有数据
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }

            // 从propertyMap加载数据
            if (propertyMap.containsKey("output")) {
                JsonNode outputConfig = (JsonNode) propertyMap.get("output");

                // 加载基础配置
                if (outputConfig.has("responseStruct")) {
                    int responseStructValue = outputConfig.get("responseStruct").asInt();
                    for (ResponseStructType type : ResponseStructType.values()) {
                        if (type.getValue() == responseStructValue) {
                            responseStructCombo.setSelectedItem(type);
                            break;
                        }
                    }
                }

                // 加载字段定义
                if (outputConfig.has("fields")) {
                    JsonNode fieldsNode = outputConfig.get("fields");
                    if (fieldsNode.isArray()) {
                        for (JsonNode fieldNode : fieldsNode) {
                            String name = fieldNode.get("name").asText();
                            DataType type = DataType.getByValue(fieldNode.get("type").asInt());
                            StepType stepType = fieldNode.get("stepType") == null ? StepType.UNSTEP : StepType.getByValue(fieldNode.get("stepType").asInt());
                            String description = fieldNode.has("description") ?
                                    fieldNode.get("description").asText() : "";
                            String example = fieldNode.has("example") ?
                                    fieldNode.get("example").asText() : "";
                            model.addRow(new Object[]{
                                    name,
                                    type,
                                    stepType,
                                    description,
                                    example
                            });
                        }
                    }
                }
            } else {
                // 如果没有配置，设置默认值
                responseStructCombo.setSelectedItem(ResponseStructType.STANDARD);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新预览区域
     * @param responseStruct 响应结构类
     * @param model 表格数据模型
     * @param previewArea 预览文本区域
     */
    private void updatePreview(String responseStruct,
                            DefaultTableModel model, JTextArea previewArea) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode; // 改用JsonNode作为根节点类型
            
            switch(responseStruct) {
                case "标准结构(code/message/data)":
                    ObjectNode standardNode = mapper.createObjectNode();
                    standardNode.put("code", 200);
                    standardNode.put("message", "success");
                    ObjectNode dataNode = standardNode.putObject("data");
                    // 从表格模型中获取字段定义
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String fieldName = (String) model.getValueAt(i, 0);
                        DataType fieldType = (DataType) model.getValueAt(i, 1);
                        String exampleValue = (String) model.getValueAt(i, 4);
                        addFieldToNode(dataNode, fieldName, fieldType, exampleValue);
                    }
                    rootNode = standardNode;
                    break;
                    
                case "简单对象":
                    ObjectNode simpleNode = mapper.createObjectNode();
                    // 非分页的简单对象
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String fieldName = (String) model.getValueAt(i, 0);
                        DataType fieldType = (DataType) model.getValueAt(i, 1);
                        String exampleValue = (String) model.getValueAt(i, 4);
                        addFieldToNode(simpleNode, fieldName, fieldType, exampleValue);
                    }
                    rootNode = simpleNode;
                    break;

                default:
                    rootNode = mapper.createObjectNode();
            }
            
            // 格式化输出JSON
            String prettyJson = mapper.writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(rootNode);
            previewArea.setText(prettyJson);
            
        } catch (Exception e) {
            previewArea.setText("生成预览失败: " + e.getMessage());
            e.printStackTrace(); // 添加错误堆栈打印，方便调试
        }
    }
        

//    private JTextField createReadOnlyTextField(Object value) {
//        JTextField textField = new JTextField();
//        textField.setText(value != null ? value.toString() : "");
//        textField.setEditable(false);
//        textField.setBackground(UIManager.getColor("TextField.background"));
//        textField.setBorder(JBUI.Borders.empty(2, 4));
//        // 设首选大小以匹配IDEA的默认文本框大小
//        textField.setPreferredSize(new Dimension(200, JBUI.scale(24)));
//        return textField;
//    }


    private JTextField createTextField(Object value) {
        JTextField textField = new JTextField();
        textField.setText(value != null ? value.toString() : "");
        textField.setBorder(JBUI.Borders.empty(2, 4));
        // 设置首选大小以匹配IDEA的默认文本框大小
        textField.setPreferredSize(new Dimension(200, JBUI.scale(24)));
        return textField;
    }

    /**
     * 将字段添加到JSON节点中
     * @param node 目标JSON节点
     * @param fieldName 字段名
     * @param fieldType 字段类型
     * @param exampleValue 示例值
     */
    private void addFieldToNode(ObjectNode node, String fieldName, DataType fieldType, String exampleValue) {
        try {
            switch (fieldType) {
                case INTEGER:
                    try {
                        node.put(fieldName, Integer.parseInt(exampleValue));
                    } catch (NumberFormatException e) {
                        node.put(fieldName, 0);
                    }
                    break;
                    
                case LONG:
                    try {
                        node.put(fieldName, Long.parseLong(exampleValue));
                    } catch (NumberFormatException e) {
                        node.put(fieldName, 0L);
                    }
                    break;
                    
                case DOUBLE:
                    try {
                        node.put(fieldName, Double.parseDouble(exampleValue));
                    } catch (NumberFormatException e) {
                        node.put(fieldName, 0.0);
                    }
                    break;
                    
                case FLOAT:
                    try {
                        node.put(fieldName, Float.parseFloat(exampleValue));
                    } catch (NumberFormatException e) {
                        node.put(fieldName, 0.0f);
                    }
                    break;

                case BOOLEAN:
                    node.put(fieldName, Boolean.parseBoolean(exampleValue));
                    break;
                    
                case ARRAY:
                    // 如果是数组类型，创建一个空数组，或者尝试解析示例值为数组
                    try {
                        if (exampleValue != null && !exampleValue.trim().isEmpty()) {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode arrayNode = mapper.readTree(exampleValue);
                            if (arrayNode.isArray()) {
                                node.set(fieldName, arrayNode);
                            } else {
                                node.putArray(fieldName);
                            }
                        } else {
                            node.putArray(fieldName);
                        }
                    } catch (Exception e) {
                        node.putArray(fieldName);
                    }
                    break;
                    
                case OBJECT:
                    // 如果是对象类型，创建一个空对象，或者尝试解析示例值为对象
                    try {
                        if (exampleValue != null && !exampleValue.trim().isEmpty()) {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode objNode = mapper.readTree(exampleValue);
                            if (objNode.isObject()) {
                                node.set(fieldName, objNode);
                            } else {
                                node.putObject(fieldName);
                            }
                        } else {
                            node.putObject(fieldName);
                        }
                    } catch (Exception e) {
                        node.putObject(fieldName);
                    }
                    break;
                    
                case DATE:
                    // 日期类型，如果示例值为空则使用当前时间
                    if (exampleValue == null || exampleValue.trim().isEmpty()) {
                        node.put(fieldName, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date()));
                    } else {
                        node.put(fieldName, exampleValue);
                    }
                    break;
                    
                case STRING:
                default:
                    // 字符串类型或未知类型，直接使用示例值或空字符串
                    node.put(fieldName, exampleValue != null ? exampleValue : "");
                    break;
            }
        } catch (Exception e) {
            // 如处理过程中出现任何异常，使用默认值
            node.put(fieldName, "");
        }
    }

    private void updateOutputConfigToPropertyMap() {
        try {
            DefaultTableModel model = outputModel;

            // 创建输出配置象
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode outputConfig = mapper.createObjectNode();
            
            // 保存基础配置
            ResponseStructType selectedStruct = (ResponseStructType)responseStructCombo.getSelectedItem();

            outputConfig.put("responseStruct", selectedStruct.getValue());

            // 保存字段定义
            ArrayNode fieldsNode = outputConfig.putArray("fields");
            for (int i = 0; i < model.getRowCount(); i++) {
                String fieldName = (String) model.getValueAt(i, 0);
                if (fieldName == null || fieldName.trim().isEmpty()) {
                    continue;
                }

                DataType fieldType = (DataType) model.getValueAt(i, 1);
                StepType stepType = (StepType) model.getValueAt(i, 2);
                String description = (String) model.getValueAt(i, 3);
                String exampleValue = (String) model.getValueAt(i,4);

                ObjectNode fieldNode = mapper.createObjectNode()
                    .put("name", fieldName)
                    .put("type", fieldType.getValue())
                    .put("stepType", stepType.getValue())
                    .put("description", description)
                    .put("example", exampleValue);

                fieldsNode.add(fieldNode);
            }

            // 更新到propertyMap
            propertyMap.put("output", outputConfig);
            
            // 触发源码更新事件
            EventBus.getInstance().post(EventType.UPDATE_SOURCE_CODE + "_" + file.getPath(), propertyMap);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 添加新方法：显示导入对话框
    private void showImportDialog(DefaultTableModel model) {
        // 获取父窗口
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog;
        
        // 根据父窗口类型创建对话框
        if (parentWindow instanceof Frame) {
            dialog = new JDialog((Frame) parentWindow, "导入参数", true);
        } else if (parentWindow instanceof Dialog) {
            dialog = new JDialog((Dialog) parentWindow, "导入参数", true);
        } else {
            // 如果找不到合适的父窗口，创建一个无模态对话框
            dialog = new JDialog();
            dialog.setTitle("导入参数");
            dialog.setModal(true);
        }
        
        dialog.setLayout(new BorderLayout());
        
        // 创建文本区域
        JTextArea textArea = new JTextArea(10, 40);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        // 创建说明标签
        JLabel tipLabel = new JLabel("<html>支持两种格式:<br>" +
                "1. JSON格式: {\"name\":\"value\", ...}<br>" +
                "2. Key-Value格式: key=value<br>" +
                "每行一个参数</html>");
        tipLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton importButton = new JButton("导入");
        JButton cancelButton = new JButton("取消");
        
        importButton.addActionListener(e -> {
            String text = textArea.getText().trim();
            if (!text.isEmpty()) {
                try {
                    if (text.startsWith("{")) {
                        // JSON格式处理
                        importFromJson(text, model);
                    } else {
                        // Key-Value格式处理
                        importFromKeyValue(text, model);
                    }
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, 
                        "导入失败: " + ex.getMessage(), 
                        "错误", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(importButton);
        buttonPanel.add(cancelButton);
        
        // 组装对话框
        dialog.add(tipLabel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // 添加新方法：从JSON导入
    private void importFromJson(String jsonText, DefaultTableModel model) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(jsonText, Map.class);
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String paramName = entry.getKey();
                Object value = entry.getValue();
                DataType dataType = inferDataType(value);
                
                model.addRow(new Object[]{
                    paramName,           // 参数名
                    dataType,           // 参数类型
                    RequireType.no,     // 默认不必填
                    value.toString(),   // 默认值
                    ""                  // 描述为空
                });
            }
            // 导入后更新到 propertyMap
            updateParamsToPropertyMap();
        } catch (Exception e) {
            throw new RuntimeException("JSON格式解析失败");
        }
    }

    // 添加新方法：从Key-Value导入
    private void importFromKeyValue(String text, DefaultTableModel model) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String paramName = parts[0].trim();
                String value = parts[1].trim();
                DataType dataType = inferDataType(value);
                
                model.addRow(new Object[]{
                    paramName,           // 参数名
                    dataType,           // 参数类型
                    RequireType.no,     // 默认不必填
                    value,              // 默认值
                    ""                  // 描述为空
                });
            }
        }
        // 导入后更新到 propertyMap
        updateParamsToPropertyMap();
    }

    // 添加新方法：推断数据类型
    private DataType inferDataType(Object value) {
        if (value == null) return DataType.STRING;
        
        if (value instanceof Boolean) return DataType.BOOLEAN;
        if (value instanceof Integer || value instanceof Long) return DataType.INTEGER;
        if (value instanceof Double || value instanceof Float) return DataType.DOUBLE;
        
        String strValue = value.toString().trim().toLowerCase();
        
        // 尝试解析数字
        try {
            if (strValue.contains(".")) {
                Double.parseDouble(strValue);
                return DataType.DOUBLE;
            } else {
                Long.parseLong(strValue);
                return DataType.INTEGER;
            }
        } catch (NumberFormatException ignored) {}
        
        // 检查布尔值
        if (strValue.equals("true") || strValue.equals("false")) {
            return DataType.BOOLEAN;
        }
        
        // 默认返回字符串类型
        return DataType.STRING;
    }

    // 修改generateUrl方法
    private void generateUrl(JTextField nameText, JTextField urlText) {
        // 首先检查VIP状态
        if (!PluginCache.isValidLicense) {
            SwingUtilities.invokeLater(() -> {
                JBPopupFactory.getInstance()
                    .createBalloonBuilder(new JLabel("该功能需要开通VIP才能使用"))
                    .setFadeoutTime(3000)
                    .createBalloon()
                    .show(RelativePoint.getNorthWestOf(nameText), 
                          Balloon.Position.above);
            });
            return;
        }

        // 验证接口名称不能为空
        String name = nameText.getText().trim();
        if(name.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                JBPopupFactory.getInstance()
                    .createBalloonBuilder(new JLabel("接口名称不能为空"))
                    .setFadeoutTime(3000)
                    .createBalloon()
                    .show(RelativePoint.getNorthWestOf(nameText), 
                          Balloon.Position.above);
            });
            return;
        }

        // 创建加载图标
        AsyncProcessIcon loadingIcon = new AsyncProcessIcon("Generating URL");
        loadingIcon.resume();
        
        // 获取或保存按钮引用
        if (generateBtn == null) {
            Container parent = nameText.getParent();
            Component[] components = parent.getComponents();
            for(Component c : components) {
                if(c instanceof JButton) {
                    generateBtn = (JButton)c;
                    break;
                }
            }
        }

        // 保存父容器引用，以便在SwingWorker中使用
        final Container parent = nameText.getParent();
        final JButton currentBtn = generateBtn;  // 保存当前按钮引用

        if(currentBtn != null) {
            currentBtn.setVisible(false);
            parent.add(loadingIcon, BorderLayout.EAST);
            parent.revalidate();
            parent.repaint();
        }

        // 异步请求
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // 构建请求参数
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("name", name);

                // 将Map转换为JSON字符串
                String jsonBody = new ObjectMapper().writeValueAsString(paramMap);

                // 发送POST请求，设置Content-Type为application/json
                String result = "";
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(SmartPluginSettings.API_DOMAIN + "/generateInterface");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                
                    // Write request body
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                
                    // Read response
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        result = response.toString();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("请求失败: " + e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
                
                // 解析JSON响应获取data字段
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(result);
                
                // 检查响应状态
                if (root.has("code") && root.get("code").asInt() != 200) {
                    throw new RuntimeException(root.has("msg") ? 
                        root.get("msg").asText() : "生成失败");
                }
                
                return root.get("data").asText();
            }

            @Override
            protected void done() {
                try {
                    String generatedUrl = get();
                    urlText.setText("/api/" + generatedUrl);
                    
                    // 显示成功提示
                    SwingUtilities.invokeLater(() -> {
                        JBPopupFactory.getInstance()
                            .createBalloonBuilder(new JLabel("URL生成成功"))
                            .setFadeoutTime(3000)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(urlText), 
                                  Balloon.Position.above);
                    });
                } catch (Exception ex) {
                    // 显示错误提示
                    SwingUtilities.invokeLater(() -> {
                        JBPopupFactory.getInstance()
                            .createBalloonBuilder(new JLabel("生成失败: " + ex.getMessage()))
                            .setFadeoutTime(3000)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(urlText), 
                                  Balloon.Position.above);
                    });
                } finally {
                    // 恢复按钮状态
                    SwingUtilities.invokeLater(() -> {
                        if(currentBtn != null) {
                            parent.remove(loadingIcon);
                            parent.add(currentBtn, BorderLayout.EAST);
                            currentBtn.setVisible(true);
                            parent.revalidate();
                            parent.repaint();
                        }
                        loadingIcon.suspend();
                    });
                }
            }
        }.execute();
    }
}
