package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.smart.bean.*;
import com.smart.cache.PluginCache;
import com.smart.enums.DataType;
import com.smart.enums.DateFormatType;
import com.smart.enums.DateOperationType;
import com.smart.event.EventBus;
import com.smart.event.EventType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DateComponentSettingsDialog extends CommonDialog {
    private String componentId;
    private JComboBox<DateOperationType> operationTypeComboBox;
    private JComboBox<DateFormatType> dateFormatComboBox;
    private JTextField bindValueField;
    private JTextField inputParamField;
    private JBTable paramsTable;
    private CardLayout cardLayout;
    public DateComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("日期组件设置");
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        
        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("日期处理组件，注意:获取当前日期，入参不可填写");
        
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(descriptionLabel, BorderLayout.NORTH);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(createBeanPanel(false), BorderLayout.NORTH);

        // 创建参数表格面板
        JPanel paramsPanel = new JPanel(new BorderLayout());
        
        // 创建表格模型
        String[] columnNames = {"参数名称", "值"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column != 1) return false; // 只允许编辑"值"列
                
                // 如果是入参行，只有在FORMAT_DATE时才可编辑
                if (row == 2) { // 入参行
                    Object operationType = getValueAt(0, 1);
                    return operationType == DateOperationType.FORMAT_DATE;
                }
                return true;
            }
        };
        
        // 创建表格
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

        // 添加操作类型行
        tableModel.addRow(new Object[]{"操作类型", DateOperationType.GET_CURRENT_DATE});
        
        // 添加日期格式行
        tableModel.addRow(new Object[]{"日期格式", DateFormatType.YYYY_MM_DD});
        
        // 添加入参行 - 由于默认是GET_CURRENT_DATE，所以设置为"不可用"
        tableModel.addRow(new Object[]{"入参", "不可用"});
        
        // 添加绑定值行
        tableModel.addRow(new Object[]{"绑定值", ""});

        // 在添加完行之后，触发一次表格更新
        SwingUtilities.invokeLater(() -> {
            tableModel.fireTableDataChanged();
            paramsTable.repaint();
        });

        // 设置单元格编辑器
        valueColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>()) {
            private ComboBoxCellEditor operationTypeEditor;
            private ComboBoxCellEditor dateFormatEditor;
            private DefaultCellEditor textFieldEditor = new DefaultCellEditor(new JTextField());

            {
                // 初始化编辑器
                operationTypeEditor = new ComboBoxCellEditor(DateOperationType.values(), paramsTable, 0);
                dateFormatEditor = new ComboBoxCellEditor(DateFormatType.values(), paramsTable, 1);
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                                                       boolean isSelected, int row, int column) {
                switch (row) {
                    case 0: // 操作类型行
                        return operationTypeEditor.getTableCellEditorComponent(
                                table, value, isSelected, row, column);
                    case 1: // 日期格式行
                        return dateFormatEditor.getTableCellEditorComponent(
                                table, value, isSelected, row, column);
                    default: // 其他行使用文本框
                        return textFieldEditor.getTableCellEditorComponent(
                                table, value, isSelected, row, column);
                }
            }

            @Override
            public Object getCellEditorValue() {
                switch (paramsTable.getEditingRow()) {
                    case 0:
                        return operationTypeEditor.getCellEditorValue();
                    case 1:
                        return dateFormatEditor.getCellEditorValue();
                    default:
                        return textFieldEditor.getCellEditorValue();
                }
            }
        });

        // 修改单元格渲染器部分
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // 根据行号决定显示内容
                if (row == 0 && value instanceof DateOperationType) {
                    setText(((DateOperationType) value).getDisplayName());
                } else if (row == 1 && value instanceof DateFormatType) {
                    setText(((DateFormatType) value).getDisplayName());
                }
                
                // 设置IDEA风格的选中效果
                if (isSelected) {
                    setForeground(UIManager.getColor("Table.selectionForeground"));
                    setBackground(UIManager.getColor("Table.selectionBackground"));
                } else {
                    // 设置入参行的显示状态
                    if (row == 2 && column == 1) { // 入参行的值列
                        Object operationType = table.getModel().getValueAt(0, 1);
                        boolean isFormatDate = operationType == DateOperationType.FORMAT_DATE;
                        if (!isFormatDate) {
                            // 使用更柔和的禁用样式
                            setForeground(new Color(128, 128, 128));  // 更柔和的灰色文字
                            setBackground(new Color(250, 250, 250));  // 非常淡的灰色背景
                            setText("不可用");
                        } else {
                            setForeground(UIManager.getColor("Table.foreground"));
                            setBackground(UIManager.getColor("Table.background"));
                            setText(value == null ? "" : value.toString());
                        }
                    } else {
                        // 其他单元格使用默认样式
                        setForeground(UIManager.getColor("Table.foreground"));
                        setBackground(UIManager.getColor("Table.background"));
                        setText(value == null ? "" : value.toString());
                    }
                }
                
                // 设置边框
                setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
                
                return c;
            }
        };

        // 应用渲染器到所有列
        nameColumn.setCellRenderer(customRenderer);
        valueColumn.setCellRenderer(customRenderer);

        // 创建表格滚动面板
        JScrollPane scrollPane = new JBScrollPane(paramsTable);
        scrollPane.setPreferredSize(new Dimension(-1, 150));
        
        // 添加到参数面板
        paramsPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 将参数面板添加到中央面板
        centerPanel.add(paramsPanel, BorderLayout.CENTER);
        dialogPanel.add(centerPanel, BorderLayout.CENTER);
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        loadExistingSettings();
        
        return dialogPanel;
    }

    // 添加ComboBox单元格编辑器类
    private static class ComboBoxCellEditor extends DefaultCellEditor {
        private final JTable table;
        private final int row;
        
        public ComboBoxCellEditor(Object[] items, JTable table, int row) {
            super(new JComboBox<>(items));
            this.table = table;
            this.row = row;
            
            JComboBox<?> comboBox = (JComboBox<?>) getComponent();
            comboBox.addActionListener(e -> {
                // 当下拉框选择改变时立即触发��新
                if (row == 0) { // 操作类型行
                    Object selectedValue = comboBox.getSelectedItem();
                    if (selectedValue instanceof DateOperationType) {
                        DateOperationType selectedType = (DateOperationType) selectedValue;
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        
                        // 先更新模型中的值
                        model.setValueAt(selectedValue, 0, 1);
                        
                        if (selectedType == DateOperationType.GET_CURRENT_DATE) {
                            // 获取当前日期时，清空并禁用入参
                            model.setValueAt("不可用", 2, 1);
                        } else if (selectedType == DateOperationType.FORMAT_DATE) {
                            // 日期格式化时，清空并启用入参
                            model.setValueAt("", 2, 1);
                        }
                        
                        // 立即停止编辑
                        if (table.isEditing()) {
                            table.getCellEditor().stopCellEditing();
                        }
                        
                        // 使用 SwingUtilities.invokeLater 确保在 EDT 线程中执行 UI 更新
                        SwingUtilities.invokeLater(() -> {
                            // 更新表格模型
                            model.fireTableRowsUpdated(0, model.getRowCount() - 1);
                            // 重新绘制整个表格
                            table.repaint();
                            
                            // 确保入参行的编辑状态正确
                            if (selectedType == DateOperationType.GET_CURRENT_DATE) {
                                table.setValueAt("不可用", 2, 1);
                            }
                        });
                    }
                }
            });
        }
    }

    // 修改加载设置方法
    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);

        if (componentInfo != null && componentInfo.getComponentProp() != null) {
            ComponentProp componentProp = componentInfo.getComponentProp();
            ComponentItem item = PluginCache.componentItemMap.get(componentInfo.getType());

            // beanRef 设置
            beanReferenceField.setText(item.getBeanRef());
            ComponentUtils.setBeanReferenceField(item.getBeanRef(), this);
            // method 设置
            methodNameField.setText(item.getMethod());
            ComponentUtils.setMethodReferenceField(item.getMethod(), this);

            // 加载参数设置
            List<ParamProp> paramProps = componentProp.getParamProps();
            if (paramProps != null && !paramProps.isEmpty()) {
                DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();
                
                // 设置操作类型
                try {
                    model.setValueAt(DateOperationType.valueOf(paramProps.get(0).getVal()), 0, 1);
                } catch (IllegalArgumentException ignored) {}

                // 设置日期格式
                try {
                    model.setValueAt(DateFormatType.valueOf(paramProps.get(1).getVal()), 1, 1);
                } catch (IllegalArgumentException ignored) {}

                // 设置其他参数
                DateOperationType operationType = DateOperationType.valueOf(paramProps.get(0).getVal());
                if (operationType == DateOperationType.GET_CURRENT_DATE && paramProps.size() > 2) {
                    model.setValueAt(paramProps.get(2).getVal(), 3, 1); // 绑定值
                } else if (operationType == DateOperationType.FORMAT_DATE) {
                    if (paramProps.size() > 2) {
                        model.setValueAt(paramProps.get(2).getVal(), 2, 1); // 入参
                    }
                    if (paramProps.size() > 3) {
                        model.setValueAt(paramProps.get(3).getVal(), 3, 1); // 绑定值
                    }
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
        DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();
        
        // 从表格中获取操作类型
        Object operationTypeObj = model.getValueAt(0, 1);

        DateOperationType operationType = (DateOperationType) operationTypeObj;
        String bindValue = (String) model.getValueAt(3, 1); // 绑定值在第4行
        
        if (operationType == DateOperationType.GET_CURRENT_DATE) {
            if (bindValue == null || bindValue.trim().isEmpty()) {
                Messages.showWarningDialog("请输入绑定值", "验证失败");
                return false;
            }
        } else if (operationType == DateOperationType.FORMAT_DATE) {
            String inputParam = (String) model.getValueAt(2, 1); // 入参在第3行
            if (inputParam == null || inputParam.trim().isEmpty() || "不可用".equals(inputParam)) {
                Messages.showWarningDialog("请输入入参", "验证失败");
                return false;
            }
            if (bindValue == null || bindValue.trim().isEmpty()) {
                Messages.showWarningDialog("请输入绑定值", "验证失败");
                return false;
            }
        }
        return true;
    }

    public void saveSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        ComponentProp componentProp = componentInfo.getComponentProp();
        if (componentProp == null) {
            componentProp = new ComponentProp();
        }

        DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();

        // 设置基本属性
        componentProp.setBeanRef(beanReferenceField.getText());
        componentProp.setMethod(methodNameField.getText());

        // 保存参数
        List<ParamProp> props = new ArrayList<>();
        
        // 保存操作类型
        ParamProp operationProp = new ParamProp();
        operationProp.setSeq("1");
        operationProp.setVal(((DateOperationType) model.getValueAt(0, 1)).name());
        operationProp.setDataType(DataType.STRING);
        props.add(operationProp);

        // 保存日期格式
        ParamProp formatProp = new ParamProp();
        formatProp.setSeq("2");
        formatProp.setVal(((DateFormatType) model.getValueAt(1, 1)).name());
        formatProp.setDataType(DataType.STRING);
        props.add(formatProp);

        // 根据操作类型保存其他参数
        DateOperationType operationType = (DateOperationType) model.getValueAt(0, 1);
        if (operationType == DateOperationType.GET_CURRENT_DATE) {
            ParamProp bindValueProp = new ParamProp();
            bindValueProp.setSeq("3");
            bindValueProp.setVal((String) model.getValueAt(3, 1)); // 绑定值
            bindValueProp.setDataType(DataType.STRING);
            props.add(bindValueProp);
        } else if (operationType == DateOperationType.FORMAT_DATE) {
            ParamProp inputParamProp = new ParamProp();
            inputParamProp.setSeq("3");
            inputParamProp.setVal((String) model.getValueAt(2, 1)); // 入参
            inputParamProp.setDataType(DataType.STRING);
            props.add(inputParamProp);

            ParamProp bindValueProp = new ParamProp();
            bindValueProp.setSeq("4");
            bindValueProp.setVal((String) model.getValueAt(3, 1)); // 绑定值
            bindValueProp.setDataType(DataType.STRING);
            props.add(bindValueProp);
        }

        componentProp.setParamProps(props);
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }
}
