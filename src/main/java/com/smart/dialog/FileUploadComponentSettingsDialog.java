package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.smart.bean.*;
import com.smart.cache.PluginCache;
import com.smart.enums.DataType;
import com.smart.event.EventBus;
import com.smart.event.EventType;
import com.smart.utils.FlowDataSourceUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUploadComponentSettingsDialog extends CommonDialog{
    private String componentId;
    private JBTable paramsTable;
    private DefaultTableModel paramsTableModel;
    private List<Map<String, String>> fileUploadConfigs = new ArrayList<>();

    public FileUploadComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("文件上传配置");
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    // 创建面板内容
    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        
        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("文件上传组件配置，用于处理文件上传功能");
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(descriptionLabel, BorderLayout.NORTH);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(createBeanPanel(false), BorderLayout.NORTH);

        // 创建参数表格面板
        JPanel paramsPanel = new JPanel(new BorderLayout());
        
        // 创建表格
        String[] columnNames = {"参数名称", "值", "说明"};
        paramsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // 只允许编辑"值"列
            }
        };
        
        // 添加参数行
        paramsTableModel.addRow(new Object[]{"绑定名称", "", "组件绑定的名称，用于在流程中引用"});
        paramsTableModel.addRow(new Object[]{"上传路径", "", "选择文件上传目标位置"});
        paramsTableModel.addRow(new Object[]{"文件名称策略", "原始文件名", "选择文件命名方式"});
        
        paramsTable = new JBTable(paramsTableModel);
        paramsTable.setRowHeight(25);
        
        // 设置表头渲染器
        paramsTable.getTableHeader().setDefaultRenderer(ComponentUtils.createTableHeaderRenderer());
        
        // 设置列宽
        TableColumn nameColumn = paramsTable.getColumnModel().getColumn(0);
        nameColumn.setMinWidth(100);
        nameColumn.setMaxWidth(150);
        
        TableColumn valueColumn = paramsTable.getColumnModel().getColumn(1);
        valueColumn.setMinWidth(150);
        
        // 为"上传路径"创建下拉框编辑器
        fileUploadConfigs = FlowDataSourceUtils.loadFileUploadConfigs(project, file);
        JComboBox<String> uploadPathComboBox = new JComboBox<>();
        for (Map<String, String> config : fileUploadConfigs) {
            uploadPathComboBox.addItem(config.get("name"));
        }
        
        // 文件名称策略下拉框
        JComboBox<String> fileNameStrategyComboBox = new JComboBox<>();
        fileNameStrategyComboBox.addItem("原始文件名");
        fileNameStrategyComboBox.addItem("时间戳命名");
        fileNameStrategyComboBox.addItem("UUID命名");
        
        // 设置单元格编辑器
        paramsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()) {
            private JTextField bindNameTF = new JTextField();
            private JComboBox<String> uploadPathCB = uploadPathComboBox;
            private JComboBox<String> fileNameStrategyCB = fileNameStrategyComboBox;
            
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                                                      boolean isSelected, int row, int column) {
                if (row == 0) { // 绑定名称行
                    bindNameTF.setText(value != null ? value.toString() : "");
                    return bindNameTF;
                } else if (row == 1) { // 上传路径行
                    return uploadPathCB;
                } else if (row == 2) { // 文件名称策略行
                    return fileNameStrategyCB;
                }
                // 默认使用文本编辑器
                return super.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
            
            @Override
            public Object getCellEditorValue() {
                if (paramsTable.getEditingRow() == 0) {
                    return bindNameTF.getText();
                } else if (paramsTable.getEditingRow() == 1) {
                    return uploadPathCB.getSelectedItem();
                } else if (paramsTable.getEditingRow() == 2) {
                    return fileNameStrategyCB.getSelectedItem();
                }
                return super.getCellEditorValue();
            }
        });
        
        TableColumn descColumn = paramsTable.getColumnModel().getColumn(2);
        descColumn.setMinWidth(150);
        descColumn.setCellRenderer(ComponentUtils.createDescRenderer());
        
        // 创建表格滚动面板
        JScrollPane scrollPane = new JBScrollPane(paramsTable);
        scrollPane.setPreferredSize(new Dimension(-1, 150));
        
        // 添加到参数面板
        paramsPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 将参数面板添加到中央面板
        centerPanel.add(paramsPanel, BorderLayout.CENTER);
        dialogPanel.add(centerPanel, BorderLayout.CENTER);
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 加载现有配置
        loadExistingSettings();
        
        return dialogPanel;
    }
    
    @Override
    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        
        if (componentInfo != null && componentInfo.getComponentProp() != null) {
            ComponentProp componentProp = componentInfo.getComponentProp();
            
            ComponentItem item = PluginCache.componentItemMap.get(componentInfo.getType());
            
            // Bean设置
            beanReferenceField.setText(item.getBeanRef());
            ComponentUtils.setBeanReferenceField(item.getBeanRef(), this);
            
            // 方法设置
            methodNameField.setText(item.getMethod());
            ComponentUtils.setMethodReferenceField(item.getMethod(), this);
            
            // 获取参数属性
            List<ParamProp> paramProps = componentProp.getParamProps();
            if (paramProps != null && !paramProps.isEmpty()) {
                for (ParamProp prop : paramProps) {
                    if (prop != null) {
                        String keyName = prop.getKeyName();
                        String val = prop.getVal();
                        
                        // 查找并更新表格中的值
                        for (int i = 0; i < paramsTableModel.getRowCount(); i++) {
                            String paramName = (String) paramsTableModel.getValueAt(i, 0);
                            if (paramName.equals(keyName)) {
                                paramsTableModel.setValueAt(val, i, 1);
                                break;
                            }
                        }
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
        // 验证绑定名称
        String bindName = (String) paramsTableModel.getValueAt(0, 1);
        if (bindName == null || bindName.trim().isEmpty()) {
            Messages.showErrorDialog("请输入绑定名称", "输入验证");
            return false;
        }
        
        // 验证上传路径
        String uploadPath = (String) paramsTableModel.getValueAt(1, 1);
        if (uploadPath == null || uploadPath.trim().isEmpty()) {
            Messages.showErrorDialog("请选择上传路径", "输入验证");
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
        
        // 设置Bean和方法
        componentProp.setBeanRef(beanReferenceField.getText());
        componentProp.setMethod(methodNameField.getText());
        
        // 设置所有参数属性
        List<ParamProp> paramProps = new ArrayList<>();
        
        // 添加表格中的参数
        for (int i = 0; i < paramsTableModel.getRowCount(); i++) {
            ParamProp paramProp = new ParamProp();
            paramProp.setKeyName((String) paramsTableModel.getValueAt(i, 0));
            paramProp.setVal((String) paramsTableModel.getValueAt(i, 1));
            paramProps.add(paramProp);
        }
        
        componentProp.setParamProps(paramProps);
        componentProp.setOperationType(null);
        componentInfo.setComponentProp(componentProp);
        
        // 保存到缓存
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        
        // 触发更新事件
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }
}
