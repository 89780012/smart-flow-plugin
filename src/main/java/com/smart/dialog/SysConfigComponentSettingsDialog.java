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
import com.smart.enums.UniqueIdType;
import com.smart.event.EventBus;
import com.smart.event.EventType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SysConfigComponentSettingsDialog extends CommonDialog {
    private String componentId;
    private JComboBox<DateOperationType> operationTypeComboBox;
    private JComboBox<DateFormatType> dateFormatComboBox;
    private JTextField bindValueField;
    private JTextField inputParamField;
    private JBTable paramsTable;
    private CardLayout cardLayout;

    public SysConfigComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("系统配置组件设置");
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        
        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("系统配置组件, 返回值为字符串类型");
        
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
                return column == 1; // 只允许编辑"值"列
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

        // 添加前缀行
        tableModel.addRow(new Object[]{"入参", ""});
        tableModel.addRow(new Object[]{"绑定值", ""});

        // 在添加完行之后，触发一次表格更新
        SwingUtilities.invokeLater(() -> {
            tableModel.fireTableDataChanged();
            paramsTable.repaint();
        });

        // 修改单元格编辑器
        valueColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>()) {
            private DefaultCellEditor textFieldEditor = new DefaultCellEditor(new JTextField());

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                                                         boolean isSelected, int row, int column) {
                return textFieldEditor.getTableCellEditorComponent(
                    table, value, isSelected, row, column);
            }

            @Override
            public Object getCellEditorValue() {

                return textFieldEditor.getCellEditorValue();
            }
        });

        // 修改渲染器
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (row == 0 ) {
                    setText(String.valueOf(value));
                }
                // 设置IDEA风格的选中效果
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
                try {
                    model.setValueAt(paramProps.get(0).getVal(), 0, 1);
                } catch (IllegalArgumentException ignored) {}

                // 设置绑定值
                if (paramProps.size() > 1) {
                    model.setValueAt(paramProps.get(1).getVal(), 1, 1);
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

        // 获取入参
        String inputParam = (String) model.getValueAt(0, 1);
        if (inputParam == null || inputParam.trim().isEmpty()) {
            Messages.showWarningDialog("请输入入参", "验证失败");
            return false;
        }
        
        // 获取绑定值
        String bindValue = (String) model.getValueAt(1, 1);
        
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

        DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();

        // 设置基本属性
        componentProp.setBeanRef(beanReferenceField.getText());
        componentProp.setMethod(methodNameField.getText());

        // 保存参数
        List<ParamProp> props = new ArrayList<>();
        
        // 保存入参
        ParamProp typeProp = new ParamProp();
        typeProp.setSeq("1");
        typeProp.setVal((String) model.getValueAt(0, 1));
        typeProp.setDataType(DataType.STRING);
        props.add(typeProp);

        // 保存绑定值
        ParamProp bindValueProp = new ParamProp();
        bindValueProp.setSeq("2");
        bindValueProp.setVal((String) model.getValueAt(1, 1));
        bindValueProp.setDataType(DataType.STRING);
        props.add(bindValueProp);

        componentProp.setParamProps(props);
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }
}
