package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.smart.bean.ComponentInfo;
import com.smart.bean.ComponentItem;
import com.smart.bean.ComponentProp;
import com.smart.bean.ParamProp;
import com.smart.cache.PluginCache;
import com.smart.enums.ThreadType;
import com.smart.event.EventBus;
import com.smart.event.EventType;
import com.smart.utils.FlowDataSourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpComponentSettingsDialog extends CommonDialog{
    private Project project;
    private String componentId;
    private JComboBox<String> httpServiceComboBox;
    private JComboBox<String> methodComboBox;
    private JTextField urlField;
    private JTable headersTable;
    private DefaultTableModel headersTableModel;
    private JTable paramsTable;
    private DefaultTableModel paramsTableModel;
    private JTextArea requestBodyArea;
    private JComboBox<String> contentTypeComboBox;
    private JPanel dialogPanel;
    private JTextField bindKeyTextField;
    private JComboBox<ThreadType> threadTypeComboBox;

    public HttpComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("HTTP请求");
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    // 创建面板内容
    public JPanel createConfigurationPanel() {
        dialogPanel = new JPanel(new BorderLayout(10, 10));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("HTTP请求组件，用于发送HTTP/HTTPS请求并处理响应");
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(descriptionLabel, BorderLayout.NORTH);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央面板
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        
        // 创建HTTP服务和URL面板
        JPanel httpServicePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // HTTP服务选择
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        httpServicePanel.add(new JLabel("HTTP服务:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        httpServiceComboBox = new JComboBox<>();
        // 添加一个自定义选项
        httpServiceComboBox.addItem("自定义");
        
        // 从配置加载HTTP服务
        List<Map<String, String>> httpServices = loadHttpServices();
        for (Map<String, String> service : httpServices) {
            httpServiceComboBox.addItem(service.get("name") + " (" + service.get("id") + ")");
        }
        
        httpServicePanel.add(httpServiceComboBox, gbc);
        
        // HTTP请求方法
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        httpServicePanel.add(new JLabel("请求方法:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        methodComboBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE"});
        httpServicePanel.add(methodComboBox, gbc);
        
        // URL输入
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        httpServicePanel.add(new JLabel("请求URL:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        urlField = new JTextField();
        httpServicePanel.add(urlField, gbc);
        
        // 添加HTTP服务选择变更监听器
        httpServiceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUrlField();
            }
        });

        // 创建请求头和参数表格面板
        JTabbedPane requestTabs = new JTabbedPane();
        
        // 创建请求头表格
        JPanel headersPanel = new JPanel(new BorderLayout(5, 5));
        headersPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        String[] headerColumnNames = {"名称", "值", "描述"};
        headersTableModel = new DefaultTableModel(headerColumnNames, 0);
        headersTable = new JBTable(headersTableModel);
        headersTable.setRowHeight(25);
        
        // 设置列宽
        TableColumn nameColumn = headersTable.getColumnModel().getColumn(0);
        nameColumn.setMinWidth(150);
        nameColumn.setPreferredWidth(150);
        
        TableColumn valueColumn = headersTable.getColumnModel().getColumn(1);
        valueColumn.setMinWidth(200);
        valueColumn.setPreferredWidth(200);
        
        // 描述列
        TableColumn descColumn = headersTable.getColumnModel().getColumn(2);
        descColumn.setMinWidth(200);
        descColumn.setPreferredWidth(200);
        descColumn.setCellRenderer(ComponentUtils.createDescRenderer());
        
        // 添加滚动面板
        JScrollPane headersScrollPane = new JBScrollPane(headersTable);
        headersPanel.add(headersScrollPane, BorderLayout.CENTER);
        
        // 添加按钮面板
        JPanel headerButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addHeaderButton = new JButton("添加请求头");
        JButton removeHeaderButton = new JButton("移除请求头");
        
        addHeaderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                headersTableModel.addRow(new Object[]{"", "", ""});
            }
        });
        
        removeHeaderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = headersTable.getSelectedRow();
                if (selectedRow != -1) {
                    headersTableModel.removeRow(selectedRow);
                } else {
                    Messages.showWarningDialog("请先选择要删除的行", "警告");
                }
            }
        });
        
        headerButtonsPanel.add(addHeaderButton);
        headerButtonsPanel.add(removeHeaderButton);
        headersPanel.add(headerButtonsPanel, BorderLayout.SOUTH);
        
        // 添加常用请求头
        addCommonHeaders();
        
        // 将表格添加到选项卡
        requestTabs.addTab("请求头", headersPanel);
        
        // 创建请求参数表格
        JPanel paramsPanel = new JPanel(new BorderLayout(5, 5));
        paramsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        String[] paramColumnNames = {"参数名", "参数值", "描述"};
        paramsTableModel = new DefaultTableModel(paramColumnNames, 0);
        paramsTable = new JBTable(paramsTableModel);
        paramsTable.setRowHeight(25);
        
        // 设置列宽
        TableColumn paramNameColumn = paramsTable.getColumnModel().getColumn(0);
        paramNameColumn.setMinWidth(150);
        paramNameColumn.setPreferredWidth(150);
        
        TableColumn paramValueColumn = paramsTable.getColumnModel().getColumn(1);
        paramValueColumn.setMinWidth(200);
        paramValueColumn.setPreferredWidth(200);
        
        // 参数描述列
        TableColumn paramDescColumn = paramsTable.getColumnModel().getColumn(2);
        paramDescColumn.setMinWidth(200);
        paramDescColumn.setPreferredWidth(200);
        paramDescColumn.setCellRenderer(ComponentUtils.createDescRenderer());
        
        // 添加滚动面板
        JScrollPane paramsScrollPane = new JBScrollPane(paramsTable);
        paramsPanel.add(paramsScrollPane, BorderLayout.CENTER);
        
        // 添加按钮面板
        JPanel paramButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addParamButton = new JButton("添加参数");
        JButton removeParamButton = new JButton("移除参数");
        
        addParamButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paramsTableModel.addRow(new Object[]{"", "", ""});
            }
        });
        
        removeParamButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = paramsTable.getSelectedRow();
                if (selectedRow != -1) {
                    paramsTableModel.removeRow(selectedRow);
                } else {
                    Messages.showWarningDialog("请先选择要删除的行", "警告");
                }
            }
        });
        
        paramButtonsPanel.add(addParamButton);
        paramButtonsPanel.add(removeParamButton);
        paramsPanel.add(paramButtonsPanel, BorderLayout.SOUTH);
        
        // 将参数表格添加到选项卡
        requestTabs.addTab("URL参数", paramsPanel);
        
        // 创建请求体编辑区域
        JPanel requestBodyPanel = new JPanel(new BorderLayout(5, 5));
        requestBodyPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 添加内容类型选择器
        JPanel contentTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentTypePanel.add(new JLabel("内容类型:"));
        contentTypeComboBox = new JComboBox<>(new String[]{
            "application/json",
            "application/x-www-form-urlencoded",
            "multipart/form-data",
            "text/plain",
            "application/xml"
        });
        contentTypePanel.add(contentTypeComboBox);
        
        // 内容类型变更监听器
        contentTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedContentType = (String) contentTypeComboBox.getSelectedItem();
                // 根据选择的内容类型更新请求头
                updateContentTypeHeader(selectedContentType);
            }
        });
        
        requestBodyPanel.add(contentTypePanel, BorderLayout.NORTH);
        
        // 创建请求体文本区域
        requestBodyArea = new JTextArea();
        requestBodyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        requestBodyArea.setTabSize(2);
        requestBodyArea.setText("{\n  \n}");
        
        JScrollPane bodyScrollPane = new JBScrollPane(requestBodyArea);
        bodyScrollPane.setPreferredSize(new Dimension(-1, 200));
        requestBodyPanel.add(bodyScrollPane, BorderLayout.CENTER);
        
        // 添加格式化按钮
        JPanel bodyButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton formatJsonButton = new JButton("格式化JSON");
        formatJsonButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formatJsonRequestBody();
            }
        });
        bodyButtonsPanel.add(formatJsonButton);
        requestBodyPanel.add(bodyButtonsPanel, BorderLayout.SOUTH);
        
        // 将请求体面板添加到选项卡
        requestTabs.addTab("请求体", requestBodyPanel);
        
        // 禁用GET方法的请求体
        methodComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String method = (String) methodComboBox.getSelectedItem();
                boolean enableBody = !"GET".equals(method);
                requestBodyArea.setEnabled(enableBody);
                contentTypeComboBox.setEnabled(enableBody);
                formatJsonButton.setEnabled(enableBody);
            }
        });
        
        // 将表格和编辑区添加到中央面板
        centerPanel.add(httpServicePanel, BorderLayout.NORTH);
        centerPanel.add(requestTabs, BorderLayout.CENTER);
        
        // 创建绑定参数和线程类型面板
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        GridBagConstraints bottomGbc = new GridBagConstraints();
        bottomGbc.insets = new Insets(5, 5, 5, 5);
        bottomGbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 绑定参数
        bottomGbc.gridx = 0;
        bottomGbc.gridy = 0;
        bottomGbc.weightx = 0.0;
        bottomPanel.add(new JLabel("绑定参数:"), bottomGbc);
        
        bottomGbc.gridx = 1;
        bottomGbc.weightx = 0.5;
        bindKeyTextField = new JTextField();
        bottomPanel.add(bindKeyTextField, bottomGbc);
        
        // 线程调用类型
        bottomGbc.gridx = 2;
        bottomGbc.weightx = 0.0;
        bottomPanel.add(new JLabel("线程调用类型:"), bottomGbc);
        
        bottomGbc.gridx = 3;
        bottomGbc.weightx = 0.5;
        threadTypeComboBox = new JComboBox<>(ThreadType.values());
        threadTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ThreadType) {
                    setText(((ThreadType) value).getDisplayName());
                }
                return this;
            }
        });
        bottomPanel.add(threadTypeComboBox, bottomGbc);
        
        // 添加线程类型帮助图标
        bottomGbc.gridx = 4;
        bottomGbc.weightx = 0.0;
        bottomGbc.insets = new Insets(5, 2, 5, 5); // 调整左边距使图标更靠近下拉框
        JLabel helpIcon = new JLabel(UIManager.getIcon("OptionPane.questionIcon"));
        helpIcon.setToolTipText("<html><b>线程调用类型说明：</b><br>" +
                "- 同步：等待运行，阻塞当前线程<br>" +
                "- 异步：用于异步发送，不阻塞当前线程, 返回值不生效<br>" +
                "选择合适的线程类型可以优化程序性能和响应性</html>");
        bottomPanel.add(helpIcon, bottomGbc);
        
        centerPanel.add(bottomPanel, BorderLayout.SOUTH);
        dialogPanel.add(centerPanel, BorderLayout.CENTER);
        
        // 加载现有设置
        loadExistingSettings();
        
        // 设置首选大小
        dialogPanel.setPreferredSize(new Dimension(800, 600));
        
        return dialogPanel;
    }
    
    // 根据选择的HTTP服务更新URL字段
    private void updateUrlField() {
        String selected = (String) httpServiceComboBox.getSelectedItem();
        if (selected == null || "自定义".equals(selected)) {
            urlField.setText("");
            urlField.setEditable(true);
            return;
        }
        
        // 解析选中的服务 - 格式为: "名称 (ID)"
        String serviceId = selected.substring(selected.lastIndexOf("(") + 1, selected.lastIndexOf(")"));
        
        List<Map<String, String>> services = loadHttpServices();
        for (Map<String, String> service : services) {
            if (service.get("id").equals(serviceId)) {
                urlField.setText(service.get("url"));
                break;
            }
        }
    }
    
    // 加载现有设置
    @Override
    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        if (componentInfo == null || componentInfo.getComponentProp() == null) {
            // 设置默认值
            methodComboBox.setSelectedItem("GET");
            threadTypeComboBox.setSelectedItem(ThreadType.SYNC);
            return;
        }
        
        ComponentProp componentProp = componentInfo.getComponentProp();
        
        // 加载Bean和方法设置
        ComponentItem item = PluginCache.componentItemMap.get(componentInfo.getType());
        if (item != null) {
            // Bean设置
            beanReferenceField.setText(item.getBeanRef());
            ComponentUtils.setBeanReferenceField(item.getBeanRef(), this);
            
            // 方法设置
            methodNameField.setText(item.getMethod());
            ComponentUtils.setMethodReferenceField(item.getMethod(), this);
        }
        
        // 加载HTTP服务
        if (componentProp.getParamProps() != null) {
            for (ParamProp paramProp : componentProp.getParamProps()) {
                if (paramProp == null) continue;
                
                String keyName = paramProp.getKeyName();
                String value = paramProp.getVal();
                
                // 根据参数名加载不同设置
                switch (keyName) {
                    case "httpService":
                        selectHttpService(value);
                        break;
                    case "method":
                        methodComboBox.setSelectedItem(value);
                        break;
                    case "url":
                        urlField.setText(value);
                        break;
                    case "requestBody":
                        requestBodyArea.setText(value);
                        break;
                    case "contentType":
                        contentTypeComboBox.setSelectedItem(value);
                        updateContentTypeHeader(value);
                        break;
                    case "headers":
                        loadHeadersFromJson(value);
                        break;
                    case "params":
                        loadParamsFromJson(value);
                        break;
                }
            }
        }
        
        // 加载绑定参数
        if (componentProp.getBindKey() != null) {
            bindKeyTextField.setText(componentProp.getBindKey());
        }
        
        // 加载线程类型
        if (componentProp.getThreadType() != null) {
            threadTypeComboBox.setSelectedItem(componentProp.getThreadType());
        } else {
            threadTypeComboBox.setSelectedItem(ThreadType.SYNC);
        }
    }
    
    // 根据名称或ID选择HTTP服务
    private void selectHttpService(String serviceNameOrId) {
        if (serviceNameOrId == null || serviceNameOrId.isEmpty()) {
            httpServiceComboBox.setSelectedItem("自定义");
            return;
        }
        
        // 尝试找到匹配的HTTP服务
        for (int i = 0; i < httpServiceComboBox.getItemCount(); i++) {
            String item = (String) httpServiceComboBox.getItemAt(i);
            if (item.contains(serviceNameOrId)) {
                httpServiceComboBox.setSelectedIndex(i);
                return;
            }
        }
        
        // 如果没找到，选择自定义
        httpServiceComboBox.setSelectedItem("自定义");
    }
    
    // 从JSON字符串加载请求头
    private void loadHeadersFromJson(String headersJson) {
        try {
            if (headersJson == null || headersJson.isEmpty()) {
                return;
            }
            
            // 清空现有请求头
            while (headersTableModel.getRowCount() > 0) {
                headersTableModel.removeRow(0);
            }
            
            // 简单解析JSON，格式为 [{name: "xxx", value: "yyy", desc: "zzz"}, ...]
            String[] headers = headersJson.split("},\\s*\\{");
            for (String header : headers) {
                header = header.replaceAll("[{\\[\\]}]", "").trim();
                if (header.isEmpty()) continue;
                
                String name = "";
                String value = "";
                String desc = "";
                
                String[] parts = header.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("\"name\":") || part.startsWith("name:")) {
                        name = part.substring(part.indexOf(":") + 1).replaceAll("\"", "").trim();
                    } else if (part.startsWith("\"value\":") || part.startsWith("value:")) {
                        value = part.substring(part.indexOf(":") + 1).replaceAll("\"", "").trim();
                    } else if (part.startsWith("\"desc\":") || part.startsWith("desc:")) {
                        desc = part.substring(part.indexOf(":") + 1).replaceAll("\"", "").trim();
                    }
                }
                
                headersTableModel.addRow(new Object[]{name, value, desc});
            }
        } catch (Exception e) {
            Messages.showErrorDialog("加载请求头失败: " + e.getMessage(), "错误");
        }
    }
    
    // 从JSON字符串加载URL参数
    private void loadParamsFromJson(String paramsJson) {
        try {
            if (paramsJson == null || paramsJson.isEmpty()) {
                return;
            }
            
            // 清空现有参数
            while (paramsTableModel.getRowCount() > 0) {
                paramsTableModel.removeRow(0);
            }
            
            // 简单解析JSON，格式为 [{name: "xxx", value: "yyy", desc: "zzz"}, ...]
            String[] params = paramsJson.split("},\\s*\\{");
            for (String param : params) {
                param = param.replaceAll("[{\\[\\]}]", "").trim();
                if (param.isEmpty()) continue;
                
                String name = "";
                String value = "";
                String desc = "";
                
                String[] parts = param.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("\"name\":") || part.startsWith("name:")) {
                        name = part.substring(part.indexOf(":") + 1).replaceAll("\"", "").trim();
                    } else if (part.startsWith("\"value\":") || part.startsWith("value:")) {
                        value = part.substring(part.indexOf(":") + 1).replaceAll("\"", "").trim();
                    } else if (part.startsWith("\"desc\":") || part.startsWith("desc:")) {
                        desc = part.substring(part.indexOf(":") + 1).replaceAll("\"", "").trim();
                    }
                }
                
                paramsTableModel.addRow(new Object[]{name, value, desc});
            }
        } catch (Exception e) {
            Messages.showErrorDialog("加载URL参数失败: " + e.getMessage(), "错误");
        }
    }
    
    // 保存设置
    public void saveSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        if (componentInfo == null) {
            return;
        }
        
        ComponentProp componentProp = componentInfo.getComponentProp();
        if (componentProp == null) {
            componentProp = new ComponentProp();
            componentInfo.setComponentProp(componentProp);
        }
        
        // 保存Bean和方法设置
        ComponentItem item = PluginCache.componentItemMap.get(type);
        if (item != null) {
            componentProp.setBeanRef(item.getBeanRef());
            componentProp.setMethod(item.getMethod());
        }
        
        // 保存其他参数
        List<ParamProp> paramProps = new ArrayList<>();
        
        // HTTP服务
        String httpService = (String) httpServiceComboBox.getSelectedItem();
        if (httpService != null && !"自定义".equals(httpService)) {
            String serviceId = httpService.substring(httpService.lastIndexOf("(") + 1, httpService.lastIndexOf(")"));
            ParamProp httpServiceProp = new ParamProp();
            httpServiceProp.setKeyName("httpService");
            httpServiceProp.setVal(serviceId);
            paramProps.add(httpServiceProp);
        }
        
        // 请求方法
        String method = (String) methodComboBox.getSelectedItem();
        ParamProp methodProp = new ParamProp();
        methodProp.setKeyName("method");
        methodProp.setVal(method);
        paramProps.add(methodProp);
        
        // URL
        String url = urlField.getText();
        ParamProp urlProp = new ParamProp();
        urlProp.setKeyName("url");
        urlProp.setVal(url);
        paramProps.add(urlProp);
        
        // 内容类型
        String contentType = (String) contentTypeComboBox.getSelectedItem();
        ParamProp contentTypeProp = new ParamProp();
        contentTypeProp.setKeyName("contentType");
        contentTypeProp.setVal(contentType);
        paramProps.add(contentTypeProp);
        
        // 请求体 (仅在非GET方法时保存)
        if (!"GET".equals(method)) {
            String requestBody = requestBodyArea.getText();
            ParamProp bodyProp = new ParamProp();
            bodyProp.setKeyName("requestBody");
            bodyProp.setVal(requestBody);
            paramProps.add(bodyProp);
        }
        
        // 请求头
        String headersJson = convertHeadersToJson();
        ParamProp headersProp = new ParamProp();
        headersProp.setKeyName("headers");
        headersProp.setVal(headersJson);
        paramProps.add(headersProp);
        
        // URL参数
        String paramsJson = convertParamsToJson();
        ParamProp paramsProp = new ParamProp();
        paramsProp.setKeyName("params");
        paramsProp.setVal(paramsJson);
        paramProps.add(paramsProp);
        
        // 设置参数列表
        componentProp.setParamProps(paramProps);
        
        // 保存绑定参数
        componentProp.setBindKey(bindKeyTextField.getText());
        
        // 保存线程类型
        componentProp.setThreadType((ThreadType) threadTypeComboBox.getSelectedItem());
        
        // 更新组件信息
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        
        // 触发更新事件
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);

    }
    
    // 将请求头转换为JSON字符串
    private String convertHeadersToJson() {
        StringBuilder jsonBuilder = new StringBuilder("[");
        
        for (int i = 0; i < headersTableModel.getRowCount(); i++) {
            String name = (String) headersTableModel.getValueAt(i, 0);
            String value = (String) headersTableModel.getValueAt(i, 1);
            String desc = (String) headersTableModel.getValueAt(i, 2);
            
            if (name == null || name.isEmpty()) continue;
            
            if (i > 0) jsonBuilder.append(", ");
            
            jsonBuilder.append("{");
            jsonBuilder.append("\"name\": \"").append(name).append("\", ");
            jsonBuilder.append("\"value\": \"").append(value != null ? value : "").append("\", ");
            jsonBuilder.append("\"desc\": \"").append(desc != null ? desc : "").append("\"");
            jsonBuilder.append("}");
        }
        
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }
    
    // 将URL参数转换为JSON字符串
    private String convertParamsToJson() {
        StringBuilder jsonBuilder = new StringBuilder("[");
        
        for (int i = 0; i < paramsTableModel.getRowCount(); i++) {
            String name = (String) paramsTableModel.getValueAt(i, 0);
            String value = (String) paramsTableModel.getValueAt(i, 1);
            String desc = (String) paramsTableModel.getValueAt(i, 2);
            
            if (name == null || name.isEmpty()) continue;
            
            if (i > 0) jsonBuilder.append(", ");
            
            jsonBuilder.append("{");
            jsonBuilder.append("\"name\": \"").append(name).append("\", ");
            jsonBuilder.append("\"value\": \"").append(value != null ? value : "").append("\", ");
            jsonBuilder.append("\"desc\": \"").append(desc != null ? desc : "").append("\"");
            jsonBuilder.append("}");
        }
        
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }
    
    // 更新Content-Type请求头
    private void updateContentTypeHeader(String contentType) {
        // 检查是否已存在Content-Type请求头
        for (int i = 0; i < headersTableModel.getRowCount(); i++) {
            String headerName = (String) headersTableModel.getValueAt(i, 0);
            if ("Content-Type".equals(headerName)) {
                headersTableModel.setValueAt(contentType, i, 1);
                return;
            }
        }
        
        // 如果不存在，添加新的Content-Type请求头
        headersTableModel.addRow(new Object[]{"Content-Type", contentType, "指定请求体的媒体类型"});
    }
    
    // 格式化JSON请求体
    private void formatJsonRequestBody() {
        try {
            String jsonText = requestBodyArea.getText();
            if (jsonText == null || jsonText.trim().isEmpty()) {
                return;
            }
            
            // 简单的JSON格式化逻辑
            StringBuilder formatted = new StringBuilder();
            int indentLevel = 0;
            boolean inQuotes = false;
            
            for (char c : jsonText.toCharArray()) {
                switch (c) {
                    case '{':
                    case '[':
                        formatted.append(c).append("\n").append("  ".repeat(++indentLevel));
                        break;
                    case '}':
                    case ']':
                        formatted.append("\n").append("  ".repeat(--indentLevel)).append(c);
                        break;
                    case '"':
                        inQuotes = !inQuotes;
                        formatted.append(c);
                        break;
                    case ',':
                        formatted.append(c);
                        if (!inQuotes) {
                            formatted.append("\n").append("  ".repeat(indentLevel));
                        }
                        break;
                    case ':':
                        formatted.append(c).append(" ");
                        break;
                    default:
                        formatted.append(c);
                }
            }
            
            requestBodyArea.setText(formatted.toString());
        } catch (Exception e) {
            Messages.showErrorDialog("格式化JSON失败: " + e.getMessage(), "错误");
        }
    }
    
    // 添加常用请求头
    private void addCommonHeaders() {
        headersTableModel.addRow(new Object[]{"Content-Type", "application/json", "指定请求体的媒体类型"});
        headersTableModel.addRow(new Object[]{"Authorization", "", "身份验证信息，如Bearer token"});
        headersTableModel.addRow(new Object[]{"Accept", "application/json", "指定客户端能够处理的响应类型"});
    }
    
    // 从flow.xml加载HTTP服务列表
    private List<Map<String, String>> loadHttpServices() {
        List<Map<String, String>> httpServices = new ArrayList<>();
        try {
            File configFile = findFlowXmlFile();
            
            if (configFile != null && configFile.exists()) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(configFile);
                doc.getDocumentElement().normalize();

                NodeList httpNodes = doc.getElementsByTagName("http");
                for (int i = 0; i < httpNodes.getLength(); i++) {
                    Element httpElement = (Element) httpNodes.item(i);
                    String id = httpElement.getAttribute("id");
                    String name = httpElement.getAttribute("name");
                    String url = httpElement.getTextContent();

                    Map<String, String> serviceMap = new HashMap<>();
                    serviceMap.put("id", id);
                    serviceMap.put("name", name);
                    serviceMap.put("url", url);
                    httpServices.add(serviceMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Messages.showErrorDialog("加载HTTP服务配置失败: " + e.getMessage(), "错误");
        }
        return httpServices;
    }
    
    // 获取flow.xml文件
    private File findFlowXmlFile() {
        return FlowDataSourceUtils.findFlowXmlFile(project, file);
    }
    
    // 验证设置
    private boolean validateSettings() {
        // 验证URL不能为空
        String url = urlField.getText();
        if (url == null || url.trim().isEmpty()) {
            Messages.showErrorDialog("请求URL不能为空", "验证错误");
            return false;
        }
        
        // 如果不是GET请求，验证请求体（仅针对JSON内容类型）
        String method = (String) methodComboBox.getSelectedItem();
        String contentType = (String) contentTypeComboBox.getSelectedItem();
        if (!"GET".equals(method) && "application/json".equals(contentType)) {
            String requestBody = requestBodyArea.getText();
            if (requestBody == null || requestBody.trim().isEmpty()) {
                Messages.showWarningDialog("请求体为空，是否继续?", "警告");
                // 允许用户继续，但给出警告
            } else {
                // 尝试验证JSON格式
                try {
                    // 简单验证JSON括号匹配
                    int braceCount = 0;
                    int bracketCount = 0;
                    for (char c : requestBody.toCharArray()) {
                        if (c == '{') braceCount++;
                        else if (c == '}') braceCount--;
                        else if (c == '[') bracketCount++;
                        else if (c == ']') bracketCount--;
                        
                        if (braceCount < 0 || bracketCount < 0) {
                            Messages.showErrorDialog("JSON格式错误: 括号不匹配", "验证错误");
                            return false;
                        }
                    }
                    
                    if (braceCount != 0 || bracketCount != 0) {
                        Messages.showErrorDialog("JSON格式错误: 括号不匹配", "验证错误");
                        return false;
                    }
                } catch (Exception e) {
                    Messages.showErrorDialog("JSON格式错误: " + e.getMessage(), "验证错误");
                    return false;
                }
            }
        }
        
        // 验证请求头
        for (int i = 0; i < headersTableModel.getRowCount(); i++) {
            String name = (String) headersTableModel.getValueAt(i, 0);
            if (name != null && !name.trim().isEmpty()) {
                // 名称存在但值为空的情况（除了某些特殊请求头）仅警告
                String value = (String) headersTableModel.getValueAt(i, 1);
                if (value == null || value.trim().isEmpty()) {
                    // 有些请求头可以没有值，如Content-Length等
                    if (!isNoValueHeaderAllowed(name)) {
                        int result = Messages.showYesNoDialog(
                            "请求头 '" + name + "' 的值为空，是否继续?",
                            "警告",
                            Messages.getWarningIcon()
                        );
                        if (result != Messages.YES) {
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    // 判断请求头是否允许没有值
    private boolean isNoValueHeaderAllowed(String headerName) {
        String name = headerName.toLowerCase();
        // 一些允许没有值的请求头
        return name.equals("connection") || 
               name.equals("content-length") || 
               name.equals("keep-alive") || 
               name.equals("upgrade-insecure-requests");
    }
    
    @Override
    protected void doOKAction() {
        if (validateSettings()) {
            saveSettings();
            super.doOKAction();
        }
    }
}
