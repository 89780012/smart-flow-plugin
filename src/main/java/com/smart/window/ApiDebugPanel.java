package com.smart.window;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.smart.enums.DataType;
import com.smart.enums.RequireType;
import com.smart.service.SettingService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiDebugPanel extends JPanel {
    private final Project project;
    private JComboBox<String> protocolCombo;
    private JTextField baseUrlField;
    private JComboBox<String> methodCombo;
    private JTextField urlField;
    private JTable paramsTable;
    private JTable headersTable;
    private DefaultTableModel paramsModel;
    private DefaultTableModel headersModel;
    private JButton sendButton;
    private JTextArea responseArea;
    private JTextArea headersArea;
    private JLabel statusLabel;
    private JTabbedPane contentTabs;

    public ApiDebugPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        // 创建顶部请求面板
        JPanel requestPanel = createRequestPanel();
        add(requestPanel, BorderLayout.NORTH);

        // 创建内容选项卡面板
        contentTabs = new JTabbedPane();
        contentTabs.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        
        // 添加Headers选项卡
        contentTabs.addTab("Headers", AllIcons.Nodes.Parameter, createHeadersPanel());
        
        // 添加Parameters选项卡
        contentTabs.addTab("Parameters", AllIcons.Nodes.Parameter, createParametersPanel());
        
        // 添加Response选项卡
        contentTabs.addTab("Response", AllIcons.Debugger.Console, createResponsePanel());
        
        add(contentTabs, BorderLayout.CENTER);
        
        // 加载设置
        loadSettings();
    }

    private JPanel createRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(JBUI.Borders.empty(0, 0, 5, 0));

        // Base URL设置面板
        JPanel baseUrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        protocolCombo = new JComboBox<>(new String[]{"HTTP", "HTTPS"});
        baseUrlPanel.add(protocolCombo);
        
        baseUrlField = new JTextField(30);
        baseUrlPanel.add(new JLabel("Base URL:"));
        baseUrlPanel.add(baseUrlField);
        
        JButton saveBaseUrlButton = new JButton("保存", AllIcons.Actions.MenuSaveall);
        saveBaseUrlButton.addActionListener(e -> saveSettings());
        baseUrlPanel.add(saveBaseUrlButton);
        
        panel.add(baseUrlPanel, BorderLayout.NORTH);

        // 请求设置面板
        JPanel requestSettingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE"});
        requestSettingsPanel.add(methodCombo);
        
        urlField = new JTextField(30);
        requestSettingsPanel.add(new JLabel("API Path:"));
        requestSettingsPanel.add(urlField);
        
        sendButton = new JButton("发送请求", AllIcons.Actions.Execute);
        sendButton.addActionListener(e -> sendRequest());
        requestSettingsPanel.add(sendButton);
        
        // 状态标签
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(JBUI.Borders.empty(0, 10));
        requestSettingsPanel.add(statusLabel);
        
        panel.add(requestSettingsPanel, BorderLayout.CENTER);
        
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

        // 创建参数表格
        String[] columnNames = {"参数名", "参数值", "参数类型", "是否必填", "示例值"};
        paramsModel = new DefaultTableModel(columnNames, 0);
        paramsTable = new JBTable(paramsModel);
        
        // 设置表格列宽
        paramsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        paramsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        paramsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        paramsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        paramsTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        panel.add(new JBScrollPane(paramsTable), BorderLayout.CENTER);
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
        responseTabs.addTab("Body", AllIcons.Actions.ShowCode, new JBScrollPane(responseArea));

        // 响应头选项卡
        headersArea = new JTextArea();
        headersArea.setEditable(false);
        responseTabs.addTab("Headers", AllIcons.Nodes.Parameter, new JBScrollPane(headersArea));

        panel.add(responseTabs, BorderLayout.CENTER);
        return panel;
    }
    
    private void loadSettings() {
        SettingService settings = SettingService.getInstance(project);
        baseUrlField.setText(settings.getBaseUrl());
        protocolCombo.setSelectedItem(settings.isUseHttps() ? "HTTPS" : "HTTP");
    }
    
    private void saveSettings() {
        SettingService settings = SettingService.getInstance(project);
        settings.setBaseUrl(baseUrlField.getText());
        settings.setUseHttps("HTTPS".equals(protocolCombo.getSelectedItem()));
        
        // 显示保存成功通知
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Smart Flow Plugin")
            .createNotification("Base URL设置已保存", NotificationType.INFORMATION)
            .notify(project);
    }

    public void fillFromBizFile(BizFileInfo bizFileInfo) {
        // 设置请求方式
        methodCombo.setSelectedItem(bizFileInfo.getMethod().toUpperCase());
        
        // 设置URL
        urlField.setText(bizFileInfo.getUrl());
        
        // 清空并填充参数表格
        while (paramsModel.getRowCount() > 0) {
            paramsModel.removeRow(0);
        }
        
        // 填充参数表格
        for (BizFileInfo.ParamInfo param : bizFileInfo.getParams()) {
            paramsModel.addRow(new Object[]{
                param.getName(),
                param.getValue(),
                param.getType().getDisplayName(),
                param.getRequired().getDisplayName(),
                param.getExample()
            });
        }
        
        // 切换到Parameters选项卡
        contentTabs.setSelectedIndex(1);
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
            
            String fullUrl = String.format("%s://%s/%s",
                protocolCombo.getSelectedItem().toString().toLowerCase(),
                baseUrl,
                apiPath);

            // 创建HTTP客户端
            HttpClient client = HttpClients.createDefault();
            
            // 创建请求
            HttpRequestBase request;
            String method = (String) methodCombo.getSelectedItem();
            
            switch (method) {
                case "GET":
                    request = new HttpGet(fullUrl);
                    break;
                case "POST":
                    request = new HttpPost(fullUrl);
                    break;
                case "PUT":
                    request = new HttpPut(fullUrl);
                    break;
                case "DELETE":
                    request = new HttpDelete(fullUrl);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            // 收集请求头
            Map<String, String> headers = new HashMap<>();
            int headerCount = headersModel.getRowCount();
            for (int i = 0; i < headerCount; i++) {
                String headerName = (String) headersModel.getValueAt(i, 0);
                String headerValue = (String) headersModel.getValueAt(i, 1);
                if (headerName != null && !headerName.isEmpty() && headerValue != null) {
                    headers.put(headerName, headerValue);
                    request.addHeader(headerName, headerValue);
                }
            }

            // 收集参数
            Map<String, Object> params = new HashMap<>();
            int rowCount = paramsModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                String paramName = (String) paramsModel.getValueAt(i, 0);
                String example = (String) paramsModel.getValueAt(i, 4);
                if (example != null && !example.isEmpty()) {
                    params.put(paramName, example);
                }
            }

            // 准备请求体
            String requestBody = "";
            if (request instanceof HttpEntityEnclosingRequestBase && !params.isEmpty()) {
                requestBody = new GsonBuilder().setPrettyPrinting().create().toJson(params);
                ((HttpEntityEnclosingRequestBase) request).setEntity(
                    new StringEntity(requestBody, StandardCharsets.UTF_8));
                if (!request.containsHeader("Content-Type")) {
                    request.setHeader("Content-Type", "application/json");
                    headers.put("Content-Type", "application/json");
                }
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
            
            if (!params.isEmpty()) {
                requestInfo.append("\n=== 请求参数 ===\n");
                requestInfo.append(requestBody).append("\n");
            }
            
            requestInfo.append("\n=== 响应信息 ===\n");
            
            // 发送请求
            statusLabel.setText("Sending request...");
            HttpResponse response = client.execute(request);

            // 处理响应
            String responseBody = EntityUtils.toString(response.getEntity());
            
            // 更新状态
            int statusCode = response.getStatusLine().getStatusCode();
            statusLabel.setText("Status: " + statusCode + " " + response.getStatusLine().getReasonPhrase());

            // 显示响应头
            StringBuilder responseHeaders = new StringBuilder();
            for (org.apache.http.Header header : response.getAllHeaders()) {
                responseHeaders.append(header.getName()).append(": ").append(header.getValue()).append("\n");
            }
            headersArea.setText(responseHeaders.toString());

            // 格式化并显示响应
            try {
                JsonElement jsonElement = JsonParser.parseString(responseBody);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                responseArea.setText(requestInfo.toString() + gson.toJson(jsonElement));
            } catch (Exception e) {
                // 如果不是JSON，直接显示原始响应
                responseArea.setText(requestInfo.toString() + responseBody);
            }

            // 切换到Response选项卡
            contentTabs.setSelectedIndex(2);

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            responseArea.setText("Error occurred while sending request:\n" + e.getMessage());
            contentTabs.setSelectedIndex(2);
        }
    }
} 