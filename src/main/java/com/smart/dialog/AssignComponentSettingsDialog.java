package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.smart.bean.*;
import com.smart.cache.PluginCache;
import com.smart.constants.Column;
import com.smart.constants.Constants;
import com.smart.enums.DataType;
import com.smart.enums.ParamType;
import com.smart.enums.ThreadType;
import com.smart.enums.ValueCategory;
import com.smart.event.EventBus;
import com.smart.event.EventType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


public class AssignComponentSettingsDialog extends CommonDialog {
    private JLabel descriptionLabel;
    private String componentId;

    public AssignComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false); // 设置为非模态
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("赋值组件设置");
    }

    protected Dialog.ModalityType getModalityType() {
        return Dialog.ModalityType.MODELESS;
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        
        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        descriptionLabel = new JLabel("赋值组件, 常量: 将右边赋值给左边变量,常用于变量初始化 \n 变量: 将右边变量的值赋值给左边变量,常用于变量赋值");
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(descriptionLabel, BorderLayout.CENTER);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央面板来容纳Bean设置
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(createBeanPanel(false), BorderLayout.NORTH);
        
        // 创建参数面板
        JPanel paramsPanel = new JPanel(new BorderLayout());
        paramsTable = createParamTable();
        
        // 使用新的JScrollPane并设置其填充父容器
        JScrollPane tableScrollPane = new JBScrollPane(paramsTable);
        tableScrollPane.setColumnHeaderView(paramsTable.getTableHeader());
        // 设置表格滚动面板的首选大小，确保有默认高度
        tableScrollPane.setPreferredSize(new Dimension(-1, 200));
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addRowButton = new JButton("添加行");
        JButton deleteRowButton = new JButton("删除行");
        
        addRowButton.addActionListener(e -> addDefaultValue());
        deleteRowButton.addActionListener(e -> deleteSelectedRow());
        
        buttonPanel.add(addRowButton);
        buttonPanel.add(deleteRowButton);
        
        // 组装参数面板
        paramsPanel.add(tableScrollPane, BorderLayout.CENTER);
        paramsPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 将参数面板添加到中央面板
        centerPanel.add(paramsPanel, BorderLayout.CENTER);
        
        // 添加中央面板到主面板
        dialogPanel.add(centerPanel, BorderLayout.CENTER);
        
        // 设置边距
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 加载现有设置
        loadExistingSettings();
        
        return dialogPanel;
    }

    public JBTable createParamTable() {
        List<Integer> disEditableColumns = new ArrayList<>();

        //{"选择", "左值", "数据类型", "右值", "值类型"};
        paramsTableModel = new DefaultTableModel(Constants.ASSIGN_TABLE_HEADERS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 0) return true; // 选择框列可编辑
                return !disEditableColumns.contains(column);
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
        };
        
        JBTable paramsTable = new JBTable(paramsTableModel);
        
        // 设置表格的自动调整模式
        paramsTable.setAutoResizeMode(JBTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // 设置表格的行高
        paramsTable.setRowHeight(25);
        
        // 设置表头渲染器
        paramsTable.getTableHeader().setDefaultRenderer(ComponentUtils.createTableHeaderRenderer());
        paramsTable.setDefaultRenderer(Object.class, new CustomCellRenderer(disEditableColumns));
        
        // 设置列宽比例
        TableColumn column = paramsTable.getColumnModel().getColumn(0); // 选择框列
        column.setMaxWidth(50);
        column.setMinWidth(30);
        
        // 左值列
        column = paramsTable.getColumnModel().getColumn(1);
        column.setMinWidth(100);
        
        // 数据类型列
        paramsTable.getColumnModel().getColumn(2).setCellEditor(ComponentUtils.createDataTypeComboBox());
        paramsTable.getColumnModel().getColumn(2).setCellRenderer(ComponentUtils.createDataTypeRenderer());
        column = paramsTable.getColumnModel().getColumn(2);
        column.setMinWidth(100);
        
        // 右值列
        column = paramsTable.getColumnModel().getColumn(3);
        column.setMinWidth(100);
        
        // 值类型列
        paramsTable.getColumnModel().getColumn(4).setCellEditor(ComponentUtils.createValueTypeComboBox());
        paramsTable.getColumnModel().getColumn(4).setCellRenderer(ComponentUtils.createValueTypeRenderer());
        column = paramsTable.getColumnModel().getColumn(4);
        column.setMinWidth(100);
        
        return paramsTable;
    }

    private void addDefaultValue() {
        paramsTableModel.addRow(new Object[] {
                false,      // 选择框默认不选中
                "",        // 值
                DataType.STRING, // 数据类型
                "",        // 值
                ValueCategory.CONSTANT, // 数据类型
        });
    }

    @Override
    protected void doOKAction() {
        // 保存设置到源代码
        saveSettings();
        super.doOKAction();
    }

    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);

        if (componentInfo != null && componentInfo.getComponentProp() != null) {
            ComponentProp componentProp = componentInfo.getComponentProp();

            ComponentItem item = PluginCache.componentItemMap.get(componentInfo.getType());

            //beanRef 设置
            beanReferenceField.setText(item.getBeanRef());
            ComponentUtils.setBeanReferenceField(item.getBeanRef(), this);
            //method 设置
            methodNameField.setText(item.getMethod());
            ComponentUtils.setMethodReferenceField(item.getMethod(), this);

            // 设置线程调用类型
//            ThreadType threadType = componentProp.getThreadType();
//            threadTypeComboBox.setSelectedItem(threadType != null ? threadType : ThreadType.SYNC);

            // 清空现有的参数表格数据
            while (paramsTableModel.getRowCount() > 0) {
                paramsTableModel.removeRow(0);
            }
            // 添加参数到表格
            List<ParamProp> paramProps = componentProp.getParamProps();
            if (paramProps != null && !paramProps.isEmpty()) {
                for (ParamProp param : paramProps) {
                    if (param != null) {
                        paramsTableModel.addRow(new Object[] {
                                false,  // 选择框默认不选中
                                param.getVal(),
                                param.getDataType(),
                                param.getVal2(),
                                param.getValueCategory()
                        });
                    }
                }
            }
        }

    }

    // 保存设置到源代码
    public void saveSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        ComponentProp componentProp = componentInfo.getComponentProp();
        if (componentProp == null) {
            componentProp = new ComponentProp();
        }

        // 设置基本属性
        componentProp.setBeanRef(beanReferenceField.getText());
        componentProp.setMethod(methodNameField.getText());
//        componentProp.setThreadType(ThreadType.valueOf(String.valueOf(threadTypeComboBox.getSelectedItem())));

        // 处理表格中的所有行
        List<ParamProp> props = new ArrayList<>();
        int rowCount = paramsTableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            ParamProp paramProp = new ParamProp();
            paramProp.setVal(String.valueOf(paramsTableModel.getValueAt(i, 1))); // 值列现在是第2列
            paramProp.setDataType(DataType.valueOf(String.valueOf(paramsTableModel.getValueAt(i, 2)))); // 数据类型列现在是第3列
            paramProp.setVal2(String.valueOf(paramsTableModel.getValueAt(i, 3))); // 值列现在是第2列
            paramProp.setValueCategory(ValueCategory.valueOf(String.valueOf(paramsTableModel.getValueAt(i, 4)))); // 数据类型列现在是第3列
            props.add(paramProp);
        }

        componentProp.setParamProps(props);
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }

    // 添加删除行方法
    private void deleteSelectedRow() {
        List<Integer> selectedRows = new ArrayList<>();
        for (int i = paramsTableModel.getRowCount() - 1; i >= 0; i--) {
            Boolean isSelected = (Boolean) paramsTableModel.getValueAt(i, 0);
            if (isSelected != null && isSelected) {
                selectedRows.add(i);
            }
        }

        if (selectedRows.isEmpty()) {
            Messages.showWarningDialog("请先选择要删除的字段", "警告");
            return;
        }

        // 从后往前删除，避免索引变化
        for (int i = selectedRows.size() - 1; i >= 0; i--) {
            paramsTableModel.removeRow(selectedRows.get(i));
        }

    }

}
