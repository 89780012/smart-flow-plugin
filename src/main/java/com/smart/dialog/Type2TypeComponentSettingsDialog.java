package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.smart.bean.*;
import com.smart.cache.PluginCache;
import com.smart.enums.BasicDataType;
import com.smart.event.EventBus;
import com.smart.event.EventType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Type2TypeComponentSettingsDialog extends CommonDialog {
    private String componentId;
    private JBTable paramsTable;
    private DefaultTableModel tableModel;

    public Type2TypeComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("类型转换组件设置");
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        
        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("类型转换组件，将输入值转换为目标类型");
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(descriptionLabel, BorderLayout.NORTH);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(createBeanPanel(false), BorderLayout.NORTH);

        // 创建参数表格面板
        JPanel paramsPanel = new JPanel(new BorderLayout());
        
        // 创建表格
        createParamsTable();
        
        // 创建表格滚动面板
        JBScrollPane scrollPane = new JBScrollPane(paramsTable);
        scrollPane.setPreferredSize(new Dimension(-1, 150));
        
        paramsPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(paramsPanel, BorderLayout.CENTER);
        dialogPanel.add(centerPanel, BorderLayout.CENTER);
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        loadExistingSettings();
        
        return dialogPanel;
    }

    private void createParamsTable() {
        // 创建表格模型
        String[] columnNames = {"参数名称", "值"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };
        
        paramsTable = new JBTable(tableModel);
        paramsTable.setRowHeight(25);
        
        // 设置表头渲染器
        paramsTable.getTableHeader().setDefaultRenderer(ComponentUtils.createTableHeaderRenderer());
        
        // 设置列宽
        TableColumn nameColumn = paramsTable.getColumnModel().getColumn(0);
        nameColumn.setMinWidth(100);
        nameColumn.setMaxWidth(150);
        
        TableColumn valueColumn = paramsTable.getColumnModel().getColumn(1);
        valueColumn.setMinWidth(200);

        // 添加初始行
        tableModel.addRow(new Object[]{"输入值", ""});
        tableModel.addRow(new Object[]{"目标类型", BasicDataType.STRING});
        tableModel.addRow(new Object[]{"绑定值", ""});

        // 设置单元格渲��器
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (row == 1 && value instanceof BasicDataType) {
                    setText(((BasicDataType) value).getDisplayName());
                }
                
                if (isSelected) {
                    setForeground(UIManager.getColor("Table.selectionForeground"));
                    setBackground(UIManager.getColor("Table.selectionBackground"));
                } else {
                    setForeground(UIManager.getColor("Table.foreground"));
                    setBackground(UIManager.getColor("Table.background"));
                }
                
                return c;
            }
        };

        // 设置单元格编辑器
        valueColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>()) {
            private final JComboBox<BasicDataType> typeComboBox = new JComboBox<>(BasicDataType.values());
            private final java.util.Map<Integer, JTextField> textFields = new java.util.HashMap<>();

            private JTextField getTextField(int row) {
                return textFields.computeIfAbsent(row, k -> new JTextField());
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                                                       boolean isSelected, int row, int column) {
                if (row == 1) {
                    return typeComboBox;
                } else {
                    JTextField textField = getTextField(row);
                    textField.setText(value != null ? value.toString() : "");
                    return textField;
                }
            }

            @Override
            public Object getCellEditorValue() {
                if (paramsTable.getEditingRow() == 1) {
                    return typeComboBox.getSelectedItem();
                }
                return getTextField(paramsTable.getEditingRow()).getText();
            }
        });

        // 应用渲染器
        nameColumn.setCellRenderer(customRenderer);
        valueColumn.setCellRenderer(customRenderer);
    }

    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);

        if (componentInfo != null && componentInfo.getComponentProp() != null) {
            ComponentProp componentProp = componentInfo.getComponentProp();
            ComponentItem item = PluginCache.componentItemMap.get(componentInfo.getType());

            if (item != null) {
                beanReferenceField.setText(item.getBeanRef());
                ComponentUtils.setBeanReferenceField(item.getBeanRef(), this);
                methodNameField.setText(item.getMethod());
                ComponentUtils.setMethodReferenceField(item.getMethod(), this);
            }

            List<ParamProp> paramProps = componentProp.getParamProps();
            if (paramProps != null && !paramProps.isEmpty()) {
                if (paramProps.size() > 0) {
                    tableModel.setValueAt(paramProps.get(0).getVal(), 0, 1);
                }
                if (paramProps.size() > 1) {
                    try {
                        tableModel.setValueAt(BasicDataType.valueOf(paramProps.get(1).getVal()), 1, 1);
                    } catch (IllegalArgumentException ignored) {}
                }
                if (paramProps.size() > 2) {
                    tableModel.setValueAt(paramProps.get(2).getVal(), 2, 1);
                }
            }
        }
    }

    @Override
    protected void doOKAction() {
        if (validateInput()) {
            saveSettings();
            super.doOKAction();
        }
    }

    private boolean validateInput() {
        String inputValue = (String) tableModel.getValueAt(0, 1);
        String bindValue = (String) tableModel.getValueAt(2, 1);
        
        if (inputValue == null || inputValue.trim().isEmpty()) {
            Messages.showWarningDialog("请输入待转换的值", "验证失败");
            return false;
        }
        
        if (bindValue == null || bindValue.trim().isEmpty()) {
            Messages.showWarningDialog("请输入绑定值", "验证失败");
            return false;
        }
        return true;
    }

    public void saveSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        ComponentProp componentProp = componentInfo.getComponentProp();
        if (componentProp == null) {
            componentProp = new ComponentProp();
        }

        componentProp.setBeanRef(beanReferenceField.getText());
        componentProp.setMethod(methodNameField.getText());

        List<ParamProp> props = new ArrayList<>();
        
        // 保存输入值
        ParamProp inputValueProp = new ParamProp();
        inputValueProp.setSeq("1");
        inputValueProp.setVal((String) tableModel.getValueAt(0, 1));
        props.add(inputValueProp);

        // 保存目标类型
        ParamProp targetTypeProp = new ParamProp();
        targetTypeProp.setSeq("2");
        targetTypeProp.setVal(((BasicDataType) tableModel.getValueAt(1, 1)).name());
        props.add(targetTypeProp);

        // 保存绑定值
        ParamProp bindValueProp = new ParamProp();
        bindValueProp.setSeq("3");
        bindValueProp.setVal((String) tableModel.getValueAt(2, 1));
        props.add(bindValueProp);

        componentProp.setParamProps(props);
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }
}
