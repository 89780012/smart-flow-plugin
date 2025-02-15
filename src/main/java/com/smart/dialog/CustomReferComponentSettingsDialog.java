package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.smart.bean.*;
import com.smart.cache.PluginCache;
import com.smart.enums.*;
import com.smart.event.EventBus;
import com.smart.event.EventType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomReferComponentSettingsDialog extends CommonDialog {
    private String componentId;
    private JComboBox<DateOperationType> operationTypeComboBox;
    private JComboBox<DateFormatType> dateFormatComboBox;
   // private JTextField bindValueField;
    private JTextField inputParamField;
    private JBTable paramsTable;
    private CardLayout cardLayout;
    private JButton addRowButton;
    private JButton deleteRowButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    public CustomReferComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("自定义组件设置");
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        
        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("自定义组件设置, 提供一个bean类和方法, 运行中可传递参数调用");
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(descriptionLabel, BorderLayout.NORTH);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(createBeanPanel(true), BorderLayout.NORTH);

        // 创建参数表格面板
        JPanel paramsPanel = new JPanel(new BorderLayout());
        
        // 创建表格模型
        String[] columnNames = {"参数名称", "数据类型"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
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
        
        TableColumn typeColumn = paramsTable.getColumnModel().getColumn(1);
        typeColumn.setMinWidth(200);

        // 设置数据类型列的编辑器
        JComboBox<DataType> dataTypeComboBox = new JComboBox<>(DataType.values());
        typeColumn.setCellEditor(new DefaultCellEditor(dataTypeComboBox));

        // 设置数据类型列的渲染器
        typeColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof DataType) {
                    setText(((DataType) value).getDisplayName());
                }
                return this;
            }
        });

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        addRowButton = new JButton("添加");
        deleteRowButton = new JButton("删除");
        moveUpButton = new JButton("上移");
        moveDownButton = new JButton("下移");

        // 添加按钮事件监听器
        addRowButton.addActionListener(e -> addNewRow());
        deleteRowButton.addActionListener(e -> deleteSelectedRow());
        moveUpButton.addActionListener(e -> moveRow(-1));
        moveDownButton.addActionListener(e -> moveRow(1));

        buttonPanel.add(addRowButton);
        buttonPanel.add(deleteRowButton);
        buttonPanel.add(moveUpButton);
        buttonPanel.add(moveDownButton);

        // 创建表格滚动面板
        JScrollPane scrollPane = new JBScrollPane(paramsTable);
        scrollPane.setPreferredSize(new Dimension(-1, 150));

        // 将按钮面板和表格添加到参数面板
        paramsPanel.add(buttonPanel, BorderLayout.NORTH);
        paramsPanel.add(scrollPane, BorderLayout.CENTER);

        // 添加绑定值面板
//        JPanel bindValuePanel = new JPanel(new BorderLayout(5, 5));
//        bindValuePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
//
//        JLabel bindValueLabel = new JLabel("绑定值:");
//        bindValueField = new JTextField();
//        bindValuePanel.add(bindValueLabel, BorderLayout.WEST);
//        bindValuePanel.add(bindValueField, BorderLayout.CENTER);
        // 将面板添加到对话框
        centerPanel.add(paramsPanel, BorderLayout.CENTER);
        //centerPanel.add(bindValuePanel, BorderLayout.SOUTH);
        dialogPanel.add(centerPanel, BorderLayout.CENTER);
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        loadExistingSettings();
        
        return dialogPanel;
    }

    // 修改加载设置方法
    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);

        if (componentInfo != null && componentInfo.getComponentProp() != null) {
            ComponentProp componentProp = componentInfo.getComponentProp();
            ComponentItem item = PluginCache.componentItemMap.get(componentInfo.getType());

            if(componentProp.getBeanRef() != null){
                // beanRef 设置
                beanReferenceField.setText(componentProp.getBeanRef());
                ComponentUtils.setBeanReferenceField(componentProp.getBeanRef(), this);
            }
            if(componentProp.getMethod() != null){
                // method 设置
                methodNameField.setText(componentProp.getMethod());
                ComponentUtils.setMethodReferenceField(componentProp.getMethod(), this);
            }

            if(componentProp.getThreadType() != null){
                //线程类型
                ThreadType threadType = componentProp.getThreadType();
                threadTypeComboBox.setSelectedItem(threadType != null ? threadType : ThreadType.SYNC);
            }

//            if(componentProp.getBindKey() != null){
//                // bindKey
//                bindValueField.setText(componentProp.getBindKey());
//            }

            // 加载参数设置
            List<ParamProp> paramProps = componentProp.getParamProps();
            if (paramProps != null && !paramProps.isEmpty()) {
                DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();
                model.setRowCount(0); // 清空表格
                
                // 加载所有参数
                for (ParamProp prop : paramProps) {
                    model.addRow(new Object[]{
                        prop.getVal(),
                        prop.getDataType() != null ? prop.getDataType() : DataType.STRING
                    });
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
        //bean 验证
        String beanRef = beanReferenceField.getText();
        if (beanRef == null || beanRef.trim().isEmpty()) {
            Messages.showWarningDialog("请输入bean引用", "验证失败");
            return false;
        }

        //方法名验证
        String methodName = methodNameField.getText();
        if (methodName == null || methodName.trim().isEmpty()) {
            Messages.showWarningDialog("请输入方法名", "验证失败");
            return false;
        }

//        String bindValue = bindValueField.getText();
//        if (bindValue == null || bindValue.trim().isEmpty()) {
//            Messages.showWarningDialog("请输入绑定值", "验证失败");
//            return false;
//        }
        
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
        componentProp.setThreadType((ThreadType) threadTypeComboBox.getSelectedItem());

        // 保存参数
        List<ParamProp> props = new ArrayList<>();
        
        for (int i = 0; i < model.getRowCount(); i++) {
            ParamProp paramProp = new ParamProp();
            paramProp.setSeq(String.valueOf(i + 1));
            paramProp.setVal((String) model.getValueAt(i, 0));
            paramProp.setDataType((DataType) model.getValueAt(i, 1));
            props.add(paramProp);
        }

        //componentProp.setBindKey(bindValueField.getText());
        componentProp.setParamProps(props);
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + this.file.getPath(), componentId);
    }

    // 添加表格操作方法
    private void addNewRow() {
        DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();
        model.addRow(new Object[]{"", DataType.STRING});
    }

    private void deleteSelectedRow() {
        int selectedRow = paramsTable.getSelectedRow();
        if (selectedRow != -1) {
            DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();
            model.removeRow(selectedRow);
        }
    }

    private void moveRow(int direction) {
        int selectedRow = paramsTable.getSelectedRow();
        DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();
        
        if (selectedRow < 0 || 
            (direction < 0 && selectedRow == 0) || 
            (direction > 0 && selectedRow == model.getRowCount() - 1)) {
            return;
        }

        int targetRow = selectedRow + direction;
        
        // 保存当前行数据
        Object[] rowData = new Object[model.getColumnCount()];
        for (int i = 0; i < model.getColumnCount(); i++) {
            rowData[i] = model.getValueAt(selectedRow, i);
        }
        
        // 删除当前行
        model.removeRow(selectedRow);
        
        // 在目标位置插入
        model.insertRow(targetRow, rowData);
        
        // 更新选中行
        paramsTable.setRowSelectionInterval(targetRow, targetRow);
    }
}
