package com.smart.window;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import com.smart.service.SettingService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ApiDebugPanel extends JPanel {
    private final Project project;
    private JTextField baseUrlField;
    private JComboBox<String> methodCombo;
    private JTextField urlField;
    private JTable queryParamsTable;
    private JTextArea bodyTextArea;
    private JTable headersTable;
    private DefaultTableModel queryParamsModel;
    private DefaultTableModel bodyParamsModel;
    private DefaultTableModel headersModel;
    private JButton sendButton;
    private JTextArea responseArea;
    private JTextArea headersArea;
    private JLabel statusLabel;
    private JTabbedPane contentTabs;
    private JTabbedPane parametersTabs;
    private JComboBox<String> contentTypeCombo;

    public ApiDebugPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(2));
        
        // 设置首选大小
        setPreferredSize(new Dimension(600, 600));

        // 创建顶部请求面板
        JPanel requestPanel = createRequestPanel();
        add(requestPanel, BorderLayout.NORTH);

        // 创建内容选项卡面板
        contentTabs = new JTabbedPane();
        contentTabs.setBorder(JBUI.Borders.empty(2, 0, 0, 0));
        
        // 添加Headers选项卡
        contentTabs.addTab("请求头", AllIcons.Nodes.Parameter, createHeadersPanel());
        
        // 添加Parameters选项卡
        contentTabs.addTab("请求参数", AllIcons.Nodes.Parameter, createParametersPanel());
        
        // 添加Response选项卡
        contentTabs.addTab("响应", AllIcons.Debugger.Console, createResponsePanel());
        
        add(contentTabs, BorderLayout.CENTER);
        
        // 加载设置
        loadSettings();
    }

    private JPanel createRequestPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 2, 2));
        panel.setBorder(JBUI.Borders.empty(2));

        // 第一行：Base URL设置面板
        JPanel baseUrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        baseUrlField = new JTextField(20);
        JLabel domainLabel = new JLabel("域名:");
        domainLabel.setPreferredSize(new Dimension(35, 25));
        baseUrlPanel.add(domainLabel);
        baseUrlPanel.add(baseUrlField);

        
        JButton saveBaseUrlButton = new JButton("", AllIcons.Actions.MenuSaveall);
        saveBaseUrlButton.setToolTipText("保存域名配置");
        saveBaseUrlButton.addActionListener(e -> saveSettings());
        baseUrlPanel.add(saveBaseUrlButton);
        
        panel.add(baseUrlPanel);

        // 第二行：请求设置面板
        JPanel requestSettingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        // 请求方法下拉框
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE"});
        methodCombo.setPreferredSize(new Dimension(65, 25));
        requestSettingsPanel.add(methodCombo);
                
        
        // 请求路径输入框
        urlField = new JTextField(20);
        JLabel pathLabel = new JLabel("路径:");
        pathLabel.setPreferredSize(new Dimension(35, 25));
        requestSettingsPanel.add(pathLabel);
        requestSettingsPanel.add(urlField);
        
        // 发送按钮
        sendButton = new JButton("", AllIcons.Actions.Execute);
        sendButton.setToolTipText("发送请求");
        sendButton.addActionListener(e -> sendRequest());
        requestSettingsPanel.add(sendButton);
        
        // 状态标签
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(JBUI.Borders.empty(0, 5));
        statusLabel.setPreferredSize(new Dimension(100, 25));
        requestSettingsPanel.add(statusLabel);
        
        panel.add(requestSettingsPanel);
        
        return panel;
    }

    private JPanel createHeadersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty());

        // 创建工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("添加", AllIcons.General.Add);
        JButton removeButton = new JButton("删除", AllIcons.General.Remove);
        toolbar.add(addButton);
        toolbar.add(removeButton);
        panel.add(toolbar, BorderLayout.NORTH);

        // 创建headers表格
        String[] columnNames = {"Header名", "Header值"};
        headersModel = new DefaultTableModel(columnNames, 0);
        headersTable = new JBTable(headersModel);

        // 设置表格列宽
        headersTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        headersTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        
        // 添加按钮事件
        addButton.addActionListener(e -> headersModel.addRow(new Object[]{"", ""}));
        removeButton.addActionListener(e -> {
            int selectedRow = headersTable.getSelectedRow();
            if (selectedRow != -1) {
                headersModel.removeRow(selectedRow);
            }
        });

        panel.add(new JBScrollPane(headersTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createParametersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty());
        // 创建参数选项卡
        parametersTabs = new JTabbedPane();
        
        // Query Parameters选项卡
        parametersTabs.addTab("Query参数", createQueryParamsPanel());
        
        // Body Parameters选项卡
        parametersTabs.addTab("Body体", createBodyPanel());

        panel.add(parametersTabs, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createResponsePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty());
        // 创建响应选项卡
        JTabbedPane responseTabs = new JTabbedPane();

        // 响应内容选项卡
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseTabs.addTab("响应日志", AllIcons.Actions.ShowCode, new JBScrollPane(responseArea));

        // 响应头选项卡
        headersArea = new JTextArea();
        headersArea.setEditable(false);
        responseTabs.addTab("响应头", AllIcons.Nodes.Parameter, new JBScrollPane(headersArea));
        panel.add(responseTabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createQueryParamsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 创建查询参数表格
        String[] columnNames = {"参数名", "参数值", "参数类型", "是否必填", "示例值"};
        queryParamsModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if(column == 1){
                    return true;
                }
                return false;
            }

        };

        queryParamsTable = new JBTable(queryParamsModel);
        
        // 设置表格列宽
        queryParamsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        queryParamsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        queryParamsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        queryParamsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        queryParamsTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        panel.add(new JBScrollPane(queryParamsTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBodyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 添加工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton formatButton = new JButton("格式化", AllIcons.Actions.PrettyPrint);
        formatButton.addActionListener(e -> formatJsonBody());
        toolbar.add(formatButton);

        // 添加协议选择
        contentTypeCombo = new JComboBox<>(new String[]{"application/json", "application/x-www-form-urlencoded", "multipart/form-data"});
        contentTypeCombo.setPreferredSize(new Dimension(130, 25));
        toolbar.add(contentTypeCombo);
        
        panel.add(toolbar, BorderLayout.NORTH);

        // 添加协议选择监听器
        contentTypeCombo.addActionListener(e -> {
            panel.remove(1); // 移除当前的请求体显示组件
            if ("application/json".equals(contentTypeCombo.getSelectedItem())) {
                // 创建请求体文本区域
                bodyTextArea = new JTextArea();
                bodyTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                panel.add(new JBScrollPane(bodyTextArea), BorderLayout.CENTER);
            } else {
                // 创建表格
                String[] columnNames = {"参数名", "参数值", "参数类型", "是否必填", "示例值"};
                bodyParamsModel = new DefaultTableModel(columnNames, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        if(column == 1){
                            return true;
                        }
                        return false;
                    }
                };
                JTable bodyParamsTable = new JBTable(bodyParamsModel);
                panel.add(new JBScrollPane(bodyParamsTable), BorderLayout.CENTER);
            }
            panel.revalidate();
            panel.repaint();
        });

        // 初始化显示内容
        if ("application/json".equals(contentTypeCombo.getSelectedItem())) {
            // 创建请求体文本区域
            bodyTextArea = new JTextArea();
            bodyTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            panel.add(new JBScrollPane(bodyTextArea), BorderLayout.CENTER);
        } else {
            // 创建表格
            String[] columnNames = {"参数名", "参数值", "参数类型", "是否必填", "示例值"};
            DefaultTableModel bodyParamsModel = new DefaultTableModel(columnNames, 0);
            JTable bodyParamsTable = new JBTable(bodyParamsModel);
            panel.add(new JBScrollPane(bodyParamsTable), BorderLayout.CENTER);
        }
        return panel;
    }

    private void formatJsonBody() {
        try {
            String text = bodyTextArea.getText();
            if (text != null && !text.isEmpty()) {
                // 简单的 JSON 格式化实现
                StringBuilder formatted = new StringBuilder();
                int indentLevel = 0;
                boolean inQuotes = false;
                
                for (char c : text.toCharArray()) {
                    switch (c) {
                        case '{':
                        case '[':
                            formatted.append(c).append("\n").append("    ".repeat(++indentLevel));
                            break;
                        case '}':
                        case ']':
                            formatted.append("\n").append("    ".repeat(--indentLevel)).append(c);
                            break;
                        case '"':
                            inQuotes = !inQuotes;
                            formatted.append(c);
                            break;
                        case ',':
                            formatted.append(c);
                            if (!inQuotes) {
                                formatted.append("\n").append("    ".repeat(indentLevel));
                            }
                            break;
                        case ':':
                            formatted.append(c).append(" ");
                            break;
                        default:
                            formatted.append(c);
                    }
                }
                bodyTextArea.setText(formatted.toString());
            }
        } catch (Exception e) {
            // 如果格式化失败，保持原文本不变
        }
    }

    private void loadSettings() {
        SettingService settings = SettingService.getInstance(project);
        baseUrlField.setText(settings.getBaseUrl());
    }
    
    private void saveSettings() {
        SettingService settings = SettingService.getInstance(project);
        settings.setBaseUrl(baseUrlField.getText());
        
        // 显示保存成功通知
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Smart Flow Notifications")
            .createNotification("Base URL设置已保存", NotificationType.INFORMATION)
            .notify(project);
    }

    public void fillFromBizFile(BizFileInfo bizFileInfo) {
        //表示为新创建的biz文件还未初始化
        if(bizFileInfo.getMethod() == null || bizFileInfo.getMethod().isEmpty() || bizFileInfo.getUrl() == null || bizFileInfo.getUrl().isEmpty()){
            return;
        }
        // 设置请求方式
        methodCombo.setSelectedItem(bizFileInfo.getMethod().toUpperCase());
        
        // 设置URL
        urlField.setText(bizFileInfo.getUrl());
        
        // 设置协议
        contentTypeCombo.setSelectedItem(bizFileInfo.getProtocol());
        
        // 清空参数
        if(queryParamsModel != null){
            queryParamsModel.setRowCount(0);
        }
        if(bodyParamsModel != null){
            bodyParamsModel.setRowCount(0);
        }
        if(bodyTextArea != null){
            bodyTextArea.setText("");
        }

        // 正常Query参数
        for (BizFileInfo.ParamInfo param : bizFileInfo.getParams().getQueryParams()) {
            queryParamsModel.addRow(new Object[]{
                    param.getName(),
                    param.getDefaultValue(),
                    param.getType().getDisplayName(),
                    param.getRequired().getDisplayName(),
                    param.getDefaultValue()
            });
        }

        // 检查是否是JSON协议
        boolean isJsonProtocol = "application/json".equalsIgnoreCase(bizFileInfo.getProtocol());
        if (isJsonProtocol) {
            String content = "";

            if (bizFileInfo.getParams() != null && bizFileInfo.getParams().getJsonParams() != null) {
                content = bizFileInfo.getParams().getJsonParams().getContent();
                try {
                    bodyTextArea.setText(content);
                } catch (Exception e) {
                    bodyTextArea.setText("无效的JSON格式");
                }
            }
        } else {
            // 正常Body key value 参数
            for (BizFileInfo.ParamInfo param : bizFileInfo.getParams().getBodyParams()) {
                bodyParamsModel.addRow(new Object[]{
                        param.getName(),
                        param.getDefaultValue(),
                        param.getType().getDisplayName(),
                        param.getRequired().getDisplayName(),
                        param.getDefaultValue()
                });
            }

        }
        contentTabs.setSelectedIndex(1);
        parametersTabs.setSelectedIndex(0);
    }
    
    private void sendRequest() {
        try {
            // 构建完整URL
            String baseUrl = baseUrlField.getText();
            String apiPath = urlField.getText();
            
            // 确保baseUrl不以/结尾，apiPath不以/开头
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (apiPath.startsWith("/")) {
                apiPath = apiPath.substring(1);
            }
            
            String fullUrl = String.format("%s/%s", baseUrl, apiPath);

            // 创建HTTP客户端
            HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            
            // 收集查询参数
            Map<String, Object> queryParams = new HashMap<>();
            int queryRowCount = queryParamsModel.getRowCount();
            for (int i = 0; i < queryRowCount; i++) {
                String paramName = (String) queryParamsModel.getValueAt(i, 0);
                String example = (String) queryParamsModel.getValueAt(i, 1);
                if (example != null && !example.isEmpty()) {
                    queryParams.put(paramName, example);
                }
            }
            
            // 添加查询参数到URL
            if (!queryParams.isEmpty()) {
                StringBuilder urlBuilder = new StringBuilder(fullUrl);
                urlBuilder.append("?");
                queryParams.forEach((name, value) -> 
                    urlBuilder.append(name).append("=").append(value).append("&"));
                fullUrl = urlBuilder.substring(0, urlBuilder.length() - 1);
            }

            // 收集请求头
            Map<String, String> headers = new HashMap<>();
            int headerCount = headersModel.getRowCount();
            for (int i = 0; i < headerCount; i++) {
                String headerName = (String) headersModel.getValueAt(i, 0);
                String headerValue = (String) headersModel.getValueAt(i, 1);
                if (headerName != null && !headerName.isEmpty() && headerValue != null) {
                    headers.put(headerName, headerValue);
                }
            }

            // 获取请求体
            String requestBody = bodyTextArea.getText().trim();
            String method = (String) methodCombo.getSelectedItem();

            // 构建请求
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofMinutes(1));

            // 添加请求头
            headers.forEach(requestBuilder::header);
            
            // 设置Content-Type
            if (!requestBody.isEmpty() && !headers.containsKey("Content-Type")) {
                String contentType = (String) contentTypeCombo.getSelectedItem();
                requestBuilder.header("Content-Type", contentType);
                headers.put("Content-Type", contentType);
            }

            // 根据HTTP方法设置请求
            switch (method) {
                case "GET":
                    requestBuilder.GET();
                    break;
                case "POST":
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
                    break;
                case "PUT":
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(requestBody));
                    break;
                case "DELETE":
                    requestBuilder.DELETE();
                    break;
                default:
                    throw new IllegalArgumentException("不支持的HTTP方法: " + method);
            }

            // 打印请求信息
            StringBuilder requestInfo = new StringBuilder();
            requestInfo.append("=== 请求信息 ===\n");
            requestInfo.append(String.format("请求URL: %s\n", fullUrl));
            requestInfo.append(String.format("请求方法: %s\n", method));
            
            if (!headers.isEmpty()) {
                requestInfo.append("\n=== 请求头 ===\n");
                headers.forEach((name, value) -> 
                    requestInfo.append(String.format("%s: %s\n", name, value)));
            }
            
            if (!queryParams.isEmpty()) {
                requestInfo.append("\n=== 查询参数 ===\n");
                queryParams.forEach((name, value) ->
                    requestInfo.append(String.format("%s: %s\n", name, value)));
            }
            
            if (!requestBody.isEmpty()) {
                requestInfo.append("\n=== 请求体 ===\n");
                try {
                    // 格式化 JSON 请求体
                    StringBuilder formatted = new StringBuilder();
                    int indentLevel = 0;
                    boolean inQuotes = false;
                    
                    for (char c : requestBody.toCharArray()) {
                        switch (c) {
                            case '{':
                            case '[':
                                formatted.append(c).append("\n").append("    ".repeat(++indentLevel));
                                break;
                            case '}':
                            case ']':
                                formatted.append("\n").append("    ".repeat(--indentLevel)).append(c);
                                break;
                            case '"':
                                inQuotes = !inQuotes;
                                formatted.append(c);
                                break;
                            case ',':
                                formatted.append(c);
                                if (!inQuotes) {
                                    formatted.append("\n").append("    ".repeat(indentLevel));
                                }
                                break;
                            case ':':
                                formatted.append(c).append(" ");
                                break;
                            default:
                                formatted.append(c);
                        }
                    }
                    requestInfo.append(formatted.toString()).append("\n");
                } catch (Exception e) {
                    requestInfo.append(requestBody).append("\n");
                }
            }
            
            requestInfo.append("\n=== 响应信息 ===\n");
            
            // 发送请求
            statusLabel.setText("发送请求中...");
            HttpResponse<String> response = client.send(requestBuilder.build(), 
                HttpResponse.BodyHandlers.ofString());

            // 处理响应
            String responseBody = response.body();
            
            // 更新状态
            int statusCode = response.statusCode();
            statusLabel.setText("状态: " + statusCode);

            // 显示响应头
            StringBuilder responseHeaders = new StringBuilder();
            response.headers().map().forEach((name, values) -> {
                values.forEach(value -> 
                    responseHeaders.append(name).append(": ").append(value).append("\n"));
            });
            headersArea.setText(responseHeaders.toString());

            // 格式化并显示响应
            try {
                // 格式化响应 JSON
                StringBuilder formatted = new StringBuilder();
                int indentLevel = 0;
                boolean inQuotes = false;
                
                for (char c : responseBody.toCharArray()) {
                    switch (c) {
                        case '{':
                        case '[':
                            formatted.append(c).append("\n").append("    ".repeat(++indentLevel));
                            break;
                        case '}':
                        case ']':
                            formatted.append("\n").append("    ".repeat(--indentLevel)).append(c);
                            break;
                        case '"':
                            inQuotes = !inQuotes;
                            formatted.append(c);
                            break;
                        case ',':
                            formatted.append(c);
                            if (!inQuotes) {
                                formatted.append("\n").append("    ".repeat(indentLevel));
                            }
                            break;
                        case ':':
                            formatted.append(c).append(" ");
                            break;
                        default:
                            formatted.append(c);
                    }
                }
                responseArea.setText(requestInfo.toString() + formatted.toString());
            } catch (Exception e) {
                // 如果不是 JSON，直接显示原始响应
                responseArea.setText(requestInfo.toString() + responseBody);
            }

            // 切换到Response选项卡
            contentTabs.setSelectedIndex(2);

        } catch (Exception e) {
            statusLabel.setText("错误: " + e.getMessage());
            responseArea.setText("发送请求时发生错误:\n" + e.getMessage());
            contentTabs.setSelectedIndex(2);
        }
    }
} 