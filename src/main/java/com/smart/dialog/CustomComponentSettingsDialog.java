package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.smart.bean.*;
import com.smart.cache.PluginCache;
import com.smart.constants.Constants;
import com.smart.enums.ThreadType;
import com.smart.event.EventBus;
import com.smart.event.EventType;
import com.smart.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomComponentSettingsDialog extends CommonDialog {
    private JLabel descriptionLabel;
    private String componentId;
    private int count = 1;
    private String name;
    public CustomComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file,String name) {
        super(false); // 设置为非模态
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        this.name = name;
        init();
        setTitle(name);
    }

    protected Dialog.ModalityType getModalityType() {
        return Dialog.ModalityType.MODELESS; // 确保话框是非模态的
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    // 将配置面板创建逻辑抽离出来
    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        ComponentItem componentItem = PluginCache.componentItemMap.get(this.type);
        
        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        String description = componentItem.getDescription();
        descriptionLabel = new JLabel(description);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(descriptionLabel, BorderLayout.CENTER);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央面板来容纳Bean设置
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(createCustomBeanPanel(), BorderLayout.NORTH);
        
        // 创建参数面板
        JPanel paramsPanel = new JPanel(new BorderLayout());
        paramsTable = createParamTable();
        
        JScrollPane tableScrollPane = new JBScrollPane(paramsTable);
        tableScrollPane.setColumnHeaderView(paramsTable.getTableHeader());
        // 设置表格滚动面板的首选大小，确保有默认高度
        tableScrollPane.setPreferredSize(new Dimension(-1, 200));
        
        // 组装参数面板
        paramsPanel.add(tableScrollPane, BorderLayout.CENTER);

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

    // 创建参数面板
    public JBTable createParamTable() {
        ComponentItem componentItem = PluginCache.componentItemMap.get(this.type);
        ComponentItem.ComponentConfig config = componentItem.getConfig();
        
        // 创建表头
        List<String> headers = new ArrayList<>();
        headers.add("序号"); // 第1列固定为序号
        
        // 存储不可编辑的列索引
        List<Integer> disEditableColumns = new ArrayList<>();
        disEditableColumns.add(0); // 序号列不可编辑
        
        // 如果有配置,添加配置的列
        if (config != null && config.getHeader() != null) {
            for (ComponentItem.ColumnConfig columnConfig : config.getHeader()) {
                headers.add(columnConfig.getName());
                if (!columnConfig.isEdit()) {
                    disEditableColumns.add(headers.size() - 1);
                }
            }
        }
        
        // 创建表格模型
        paramsTableModel = new DefaultTableModel(headers.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return !disEditableColumns.contains(column);
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return super.getColumnClass(columnIndex);
            }
        };

        JBTable paramsTable = new JBTable(paramsTableModel);
        
        // 设置表格的基本属性
        paramsTable.setAutoResizeMode(JBTable.AUTO_RESIZE_ALL_COLUMNS);
        paramsTable.setRowHeight(25);
        paramsTable.getTableHeader().setDefaultRenderer(ComponentUtils.createTableHeaderRenderer());
        paramsTable.setDefaultRenderer(Object.class, new CustomCellRenderer(disEditableColumns));
        
        
        TableColumn column = paramsTable.getColumnModel().getColumn(0); // 序号列
        column.setMaxWidth(80);
        column.setMinWidth(50);
        
        // 设置配置列的宽度
        if (config != null && config.getHeader() != null) {
            for (int i = 0; i < config.getHeader().size(); i++) {
                ComponentItem.ColumnConfig columnConfig = config.getHeader().get(i);
                column = paramsTable.getColumnModel().getColumn(i + 1); // +2是因为前面有选择框和序号列
                if (columnConfig.getWidth() > 0) {
                    column.setPreferredWidth(columnConfig.getWidth());
                } else {
                    column.setMinWidth(100); // 默认最小宽度
                }
            }
        }
        
        return paramsTable;
    }


    @Override
    protected void doOKAction() {
        // 保存设置到源代码
        saveSettings();
        super.doOKAction();
    }

    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        ComponentItem componentItem = PluginCache.componentItemMap.get(this.type);

        if (componentInfo != null && componentInfo.getComponentProp() != null) {
            ComponentProp componentProp = componentInfo.getComponentProp();

            // 设置 Bean 和 Method
            beanReferenceField.setText(componentItem.getBeanRef());
            ComponentUtils.setBeanReferenceField(componentItem.getBeanRef(), this);
            methodNameField.setText(componentItem.getMethod());
            ComponentUtils.setMethodReferenceField(componentItem.getMethod(), this);

            String threadType = componentItem.getThreadType();
            if(StringUtils.isEmpty(threadType)){
                threadTypeComboBox.setSelectedItem(ThreadType.SYNC);
            }else{
                threadTypeComboBox.setSelectedItem(ThreadType.getByValue(Integer.parseInt(threadType)));
            }

            // 清空现有的表格数据
            while (paramsTableModel.getRowCount() > 0) {
                paramsTableModel.removeRow(0);
            }

            // 加载配置数据
            ComponentItem.ComponentConfig config = componentItem.getConfig();
            if (config != null && config.getHeader() != null) {
                // 获取已保存的参数值
                List<ParamProp> paramProps = componentProp.getParamProps();
                if (paramProps != null && !paramProps.isEmpty()) {
                    count = 1;
                    for (ParamProp param : paramProps) {
                        if (param != null) {
                            String[] values = param.getVal().split("\\|");
                            List<Object> rowData = new ArrayList<>();
                            rowData.add(count++); // 序号

                            // 填充每一列的值
                            for (int i = 0; i < config.getHeader().size(); i++) {
                                if (i < values.length) {
                                    rowData.add(values[i].trim());
                                } else {
                                    rowData.add(""); // 如果没有对应的值，填充空字符串
                                }
                            }
                            paramsTableModel.addRow(rowData.toArray());
                        }
                    }
                } else {
                    // 如果没有已保存的数据，但有配置的默认数据
                    if (config.getData() != null && !config.getData().isEmpty()) {
                        count = 1;
                        for (ComponentItem.RowConfig rowConfig : config.getData()) {
                            List<Object> rowData = new ArrayList<>();
                            rowData.add(count++); // 序号
                            rowData.addAll(rowConfig.getColumns());
                            paramsTableModel.addRow(rowData.toArray());
                        }
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

        // 保存基本属性
        componentProp.setBeanRef(beanReferenceField.getText());
        componentProp.setMethod(methodNameField.getText());
        componentProp.setThreadType((ThreadType) threadTypeComboBox.getSelectedItem());

        // 处理表格数据
        List<ParamProp> props = new ArrayList<>();
        int rowCount = paramsTableModel.getRowCount();
        int columnCount = paramsTableModel.getColumnCount();
        
        ComponentItem componentItem = PluginCache.componentItemMap.get(this.type);
        
        for (int i = 0; i < rowCount; i++) {
            ParamProp paramProp = new ParamProp();
            paramProp.setSeq(String.valueOf(paramsTableModel.getValueAt(i, 0))); // 序号列
            
            // 收集所有列的值
            List<String> values = new ArrayList<>();
            for (int j = 1; j < columnCount; j++) { // 从第2列开始(跳过序号)
                Object value = paramsTableModel.getValueAt(i, j);
                values.add(value != null ? value.toString() : "");
            }
            
            // 将所有值用逗号连接
            paramProp.setVal(String.join("|", values));
            props.add(paramProp);
        }

        componentProp.setParamProps(props);
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }

}
