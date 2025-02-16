package com.smart.settings;

import com.intellij.ui.components.JBLabel;
import com.smart.cache.PluginCache;
import com.smart.service.LicenseService;
import com.smart.service.ModelService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class SmartPluginSettingsComponent {
    private final JPanel mainPanel;
    private final JTextField licenseKeyField;
    private final JLabel statusLabel;
    private final JButton activateButton;
//    private final JButton saveButton;
    private final JCheckBox enableRemoteStorageCheckBox;


    private JPanel openAIPanel;
    private JCheckBox enableOpenAICheckBox;
    private JTextField openAIBaseUrlField;
    private JPasswordField openAIAuthKeyField;
    private JTextField openAIModelField;

    private static final int SAVE_DELAY_MS = 500; // 保存延迟时间
    private Timer saveTimer;

    public SmartPluginSettingsComponent() {
        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 创建功能设置面板
        JPanel featurePanel = new JPanel(new GridBagLayout());
        featurePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "功能设置",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        // 设置最小高度
        featurePanel.setMinimumSize(new Dimension(550, 80));
        featurePanel.setPreferredSize(new Dimension(550, 80));

        // 远程存储选项
        GridBagConstraints featureGbc = new GridBagConstraints();
        featureGbc.insets = new Insets(15, 10, 15, 10);  // 增加内部间距
        featureGbc.fill = GridBagConstraints.HORIZONTAL;
        featureGbc.anchor = GridBagConstraints.WEST;

        featureGbc.gridx = 0;
        featureGbc.gridy = 0;
        featureGbc.weightx = 0.0;
        enableRemoteStorageCheckBox = new JCheckBox("开启流程文件远程存储");
        featurePanel.add(enableRemoteStorageCheckBox, featureGbc);

        featureGbc.gridx = 1;
        featureGbc.weightx = 1.0;
        JLabel vipLabel = new JLabel("(VIP可用)");
        vipLabel.setForeground(new Color(128, 128, 128));
        featurePanel.add(vipLabel, featureGbc);

        // 添加功能设置面板到主面板
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        mainPanel.add(featurePanel, gbc);

        // 创建OpenAI配置面板
        openAIPanel = new JPanel(new GridBagLayout());
        openAIPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "OpenAI配置",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        GridBagConstraints openAIGbc = new GridBagConstraints();
        openAIGbc.insets = new Insets(5, 5, 5, 5);
        openAIGbc.fill = GridBagConstraints.HORIZONTAL;
        openAIGbc.anchor = GridBagConstraints.WEST;

        // 添加启用OpenAI选项
        openAIGbc.gridx = 0;
        openAIGbc.gridy = 0;
        openAIGbc.gridwidth = 1;
        openAIGbc.weightx = 0.0;
        enableOpenAICheckBox = new JCheckBox("启用OpenAI服务");
        openAIPanel.add(enableOpenAICheckBox, openAIGbc);

        // 添加VIP标识
        openAIGbc.gridx = 1;
        openAIGbc.weightx = 1.0;
        JLabel openAiVipLabel = new JLabel("(VIP可用)");
        openAiVipLabel.setForeground(new Color(128, 128, 128));
        openAIPanel.add(openAiVipLabel, openAIGbc);

        // 添加启用说明
        openAIGbc.gridx = 0;
        openAIGbc.gridy = 1;
        openAIGbc.gridwidth = 3;
        openAIGbc.weightx = 1.0;
        JLabel enableDescLabel = new JLabel("<html>启用后将使用自定义OpenAI服务进行代码分析和智能对话。" +
                "如不启用则使用内置的AI服务。</html>");
        enableDescLabel.setForeground(new Color(128, 128, 128));
        enableDescLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 0));
        openAIPanel.add(enableDescLabel, openAIGbc);

        // 添加Base URL配置
        openAIGbc.gridx = 0;
        openAIGbc.gridy = 2;
        openAIGbc.gridwidth = 1;
        openAIGbc.weightx = 0.0;
        JLabel baseUrlLabel = new JLabel("服务地址:");
        openAIPanel.add(baseUrlLabel, openAIGbc);

        openAIGbc.gridx = 1;
        openAIGbc.gridwidth = 2;
        openAIGbc.weightx = 1.0;
        openAIBaseUrlField = new JTextField();
        openAIBaseUrlField.setToolTipText("例如: https://api.openai.com");
        openAIPanel.add(openAIBaseUrlField, openAIGbc);

        // 添加Base URL说明
        openAIGbc.gridx = 0;
        openAIGbc.gridy = 3;
        openAIGbc.gridwidth = 3;
        JLabel baseUrlDescLabel = new JLabel("<html>OpenAI服务地址，可以是官方地址或自定义代理地址。" +
                "留空则使用默认地址。</html>");
        baseUrlDescLabel.setForeground(new Color(128, 128, 128));
        baseUrlDescLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 0));
        openAIPanel.add(baseUrlDescLabel, openAIGbc);

        // 添加Auth Key配置
        openAIGbc.gridx = 0;
        openAIGbc.gridy = 4;
        openAIGbc.gridwidth = 1;
        openAIGbc.weightx = 0.0;
        JLabel authKeyLabel = new JLabel("授权密钥:");
        openAIPanel.add(authKeyLabel, openAIGbc);

        openAIGbc.gridx = 1;
        openAIGbc.gridwidth = 2;
        openAIGbc.weightx = 1.0;
        openAIAuthKeyField = new JPasswordField();
        openAIAuthKeyField.setToolTipText("请输入您的OpenAI API Key");
        openAIPanel.add(openAIAuthKeyField, openAIGbc);

        // 添加Auth Key说明
        openAIGbc.gridx = 0;
        openAIGbc.gridy = 5;
        openAIGbc.gridwidth = 3;
        JLabel authKeyDescLabel = new JLabel("<html>OpenAI API密钥，用于访问OpenAI服务。" +
                "请妥善保管您的密钥。</html>");
        authKeyDescLabel.setForeground(new Color(128, 128, 128));
        authKeyDescLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 0));
        openAIPanel.add(authKeyDescLabel, openAIGbc);

        // 添加模型选择配置
        openAIGbc.gridx = 0;
        openAIGbc.gridy = 6;
        openAIGbc.gridwidth = 1;
        openAIGbc.weightx = 0.0;
        JLabel modelLabel = new JLabel("模型名称:");
        openAIPanel.add(modelLabel, openAIGbc);

        openAIGbc.gridx = 1;
        openAIGbc.weightx = 1.0;
        openAIModelField = new JTextField();
        openAIModelField.setToolTipText("输入OpenAI模型名称，例如：gpt-3.5-turbo, gpt-4");
        openAIPanel.add(openAIModelField, openAIGbc);

        openAIGbc.gridx = 2;
        openAIGbc.weightx = 0.0;
        JButton fetchModelsButton = new JButton("获取模型");
        fetchModelsButton.setPreferredSize(new Dimension(100, fetchModelsButton.getPreferredSize().height));
        openAIPanel.add(fetchModelsButton, openAIGbc);

        // 添加获取模型按钮事件
        fetchModelsButton.addActionListener(e -> {
            fetchModelsButton.setEnabled(false);
            fetchModelsButton.setText("获取中...");
            
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    ModelService.getInstance().fetchAvailableModels();
                    return null;
                }

                @Override
                protected void done() {
                    List<String> models = ModelService.getInstance().getAvailableModels();
                    if (!models.isEmpty()) {
                        String[] modelArray = models.toArray(new String[0]);
                        String selectedModel = (String) JOptionPane.showInputDialog(
                            mainPanel,
                            enableOpenAICheckBox.isSelected() ? 
                                "请选择配置账号可用的模型:" :
                                "请选择内置可用的模型:",
                            "选择模型",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            modelArray,
                            modelArray[0]
                        );
                        if (selectedModel != null) {
                            openAIModelField.setText(selectedModel);
                        }
                    } else {
                        JOptionPane.showMessageDialog(mainPanel, 
                            "获取模型列表失败",
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    fetchModelsButton.setEnabled(true);
                    fetchModelsButton.setText("获取模型");
                }
            };
            worker.execute();
        });

        // 添加模型选择说明
        openAIGbc.gridx = 0;
        openAIGbc.gridy = 7;
        openAIGbc.gridwidth = 3;
        JLabel modelDescLabel = new JLabel("<html>输入要使用的OpenAI模型名称，常用模型：gpt-3.5-turbo、gpt-4、gpt-4-turbo-preview</html>");
        modelDescLabel.setForeground(new Color(128, 128, 128));
        modelDescLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 0));
        openAIPanel.add(modelDescLabel, openAIGbc);

        // 将OpenAI面板添加到主面板
        mainPanel.add(openAIPanel, new GridBagConstraints() {{
            gridx = 0;
            gridy = 3;  // 调整位置到数据库配置面板之后
            gridwidth = 3;
            weightx = 1.0;
            weighty = 0.0;
            fill = GridBagConstraints.HORIZONTAL;
            insets = new Insets(20, 0, 20, 0);
        }});

        // 创建授权配置面板
        JPanel licensePanel = new JPanel(new GridBagLayout());
        licensePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "授权信息",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        GridBagConstraints licenseGbc = new GridBagConstraints();
        licenseGbc.insets = new Insets(5, 5, 5, 5);
        licenseGbc.fill = GridBagConstraints.HORIZONTAL;
        licenseGbc.anchor = GridBagConstraints.WEST;

        // 授权码行
        licenseGbc.gridx = 0;
        licenseGbc.gridy = 0;
        licenseGbc.weightx = 0.0;
        JLabel licenseLabel = new JBLabel("授权码: ");
        licenseLabel.setPreferredSize(new Dimension(100, licenseLabel.getPreferredSize().height));
        licensePanel.add(licenseLabel, licenseGbc);

        licenseGbc.gridx = 1;
        licenseGbc.weightx = 1.0;
        licenseKeyField = new JTextField();
        licensePanel.add(licenseKeyField, licenseGbc);

        licenseGbc.gridx = 2;
        licenseGbc.weightx = 0.0;
        activateButton = new JButton("激活");
        activateButton.setPreferredSize(new Dimension(80, activateButton.getPreferredSize().height));
        licensePanel.add(activateButton, licenseGbc);

        // 状态标签行
        licenseGbc.gridx = 0;
        licenseGbc.gridy = 1;
        licenseGbc.gridwidth = 3;
        statusLabel = new JLabel();
        licensePanel.add(statusLabel, licenseGbc);

        // 添加授权配置面板到主面板
        gbc.gridx = 0;
        gbc.gridy = 4;  // 调整位置到OpenAI面板之后
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        mainPanel.add(licensePanel, gbc);

        // 在所有面板添加完成后，添加一个垂直方向的弹性占位组件
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;  // 让这个组件占用所有剩余的垂直空间
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(new JPanel(), gbc);

        // 创建按钮面板
//        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
//        saveButton = new JButton("保存");
//        saveButton.setPreferredSize(new Dimension(80, saveButton.getPreferredSize().height));
//        buttonPanel.add(saveButton);
//
//        // 将按钮面板添加到主面板底部
//        gbc.gridx = 0;
//        gbc.gridy = 6;  // 调整按钮面板位置
//        gbc.weighty = 0.0;
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        mainPanel.add(buttonPanel, gbc);

        // 设置初始状态
        updateActivationStatus();

        // 添加激活按钮事件
        activateButton.addActionListener(e -> {
            // 更新状态为激活中
            statusLabel.setText("激活状态: 激活中...");
            statusLabel.setForeground(new Color(128, 128, 128));
            activateButton.setEnabled(false);

            // 验证授权
            boolean success = LicenseService.getInstance().validateLicense(licenseKeyField.getText());

            // 恢复按钮状态
            activateButton.setEnabled(true);

            // 更新激活状态
            updateActivationStatus();

            if (success) {
                JOptionPane.showMessageDialog(mainPanel, "激活成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainPanel, "激活失败，请检查授权码！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });


        // 添加复选框状态变化监听
        enableRemoteStorageCheckBox.addItemListener(e -> {
            boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
            PluginCache.enableRemoteStorage = isSelected;
        });

        // 设置合适的大小
        // mainPanel.setPreferredSize(new Dimension(550, 450));

        // 初始化保存定时器
        saveTimer = new Timer(SAVE_DELAY_MS, e -> saveSettings());
        saveTimer.setRepeats(false);
        
        // 添加文本框监听
        addTextFieldListener(licenseKeyField);

        addTextFieldListener(openAIBaseUrlField);
        addTextFieldListener(openAIAuthKeyField);
        addTextFieldListener(openAIModelField);
        
        // 添加复选框监听
        addCheckBoxListener(enableRemoteStorageCheckBox);
        addCheckBoxListener(enableOpenAICheckBox);

    }

    public JComponent getPanel() {
        return mainPanel;
    }

    // 确保 updateActivationStatus 方法正确处理到期时间
    public void updateActivationStatus() {
        boolean isValid = PluginCache.isValidLicense;
        if (isValid) {
            statusLabel.setText("激活状态: 已激活");
            statusLabel.setForeground(new Color(0, 150, 0));
            // 获取并显示到期时间
            String expiresAt = LicenseService.getInstance().getExpiresAt();
            if (expiresAt != null && !expiresAt.isEmpty()) {
                String formattedDate = expiresAt.replace("T", " ").substring(0, 19);
                statusLabel.setText("激活状态: 已激活" + "   到期时间: " + formattedDate);
                statusLabel.setForeground(new Color(0, 150, 0));
            } else {
                statusLabel.setText("激活状态: 已激活");
                statusLabel.setForeground(new Color(0, 150, 0));
            }
        } else {
            statusLabel.setText("激活状态: 未激活");
            statusLabel.setForeground(new Color(200, 0, 0));
        }
    }

    public JComponent getPreferredFocusedComponent() {
        return licenseKeyField;
    }

    public String getLicenseKey() {
        return licenseKeyField.getText();
    }

    public void setLicenseKey(String key) {
        licenseKeyField.setText(key);
    }

    public boolean isEnableRemoteStorage() {
        return enableRemoteStorageCheckBox.isSelected();
    }

    public void setEnableRemoteStorage(boolean enable) {
        enableRemoteStorageCheckBox.setSelected(enable);
    }


    public boolean isOpenAIEnabled() {
        return enableOpenAICheckBox.isSelected();
    }

    public void setOpenAIEnabled(boolean enabled) {
        enableOpenAICheckBox.setSelected(enabled);
    }

    public String getOpenAIBaseUrl() {
        return openAIBaseUrlField.getText();
    }

    public void setOpenAIBaseUrl(String url) {
        openAIBaseUrlField.setText(url);
    }

    public String getOpenAIAuthKey() {
        return new String(openAIAuthKeyField.getPassword());
    }

    public void setOpenAIAuthKey(String key) {
        openAIAuthKeyField.setText(key);
    }

    public String getOpenAIModel() {
        return openAIModelField.getText();
    }

    public void setOpenAIModel(String model) {
        openAIModelField.setText(model);
    }

    public List<String> getAvailableModels() {
        return ModelService.getInstance().getAvailableModels();
    }

    public void setAvailableModels(List<String> models) {
        // 不需要在UI中保存模型列表，因为每次都是实时获取的
    }

    // 添加文本框监听器
    private void addTextFieldListener(JTextField textField) {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { scheduleSave(); }
            public void removeUpdate(DocumentEvent e) { scheduleSave(); }
            public void changedUpdate(DocumentEvent e) { scheduleSave(); }
        });
    }
    
    // 添加复选框监听器
    private void addCheckBoxListener(JCheckBox checkBox) {
        checkBox.addItemListener(e -> scheduleSave());
    }
    
    // 调度保存任务
    private void scheduleSave() {
        saveTimer.restart();
    }
    
    // 保存设置
    private void saveSettings() {
        SmartPluginSettings settings = SmartPluginSettings.getInstance();
        settings.setLicenseKey(getLicenseKey());
        settings.setEnableRemoteStorage(isEnableRemoteStorage());
        settings.setEnableOpenAI(isOpenAIEnabled());
        settings.setOpenAIBaseUrl(getOpenAIBaseUrl());
        settings.setOpenAIAuthKey(getOpenAIAuthKey());
        settings.setOpenAIModel(getOpenAIModel());
        settings.loadState(settings);
    }
} 