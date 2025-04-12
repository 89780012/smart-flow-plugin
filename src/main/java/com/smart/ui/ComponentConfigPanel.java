package com.smart.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.smart.bean.ComponentInfo;
import com.smart.cache.PluginCache;
import com.smart.dialog.*;

import javax.swing.*;
import java.awt.*;

public class ComponentConfigPanel extends JPanel {
    private final String componentId;
    private final Project project;
    private final JPanel canvasPanel;
    private CommonDialog currentDialog;
    private VirtualFile virtualFile;
    private String bizId;

    public ComponentConfigPanel(String componentId,String bizId, Project project, JPanel canvasPanel ,VirtualFile virtualFile) {
        this.componentId = componentId;
        this.bizId = bizId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.virtualFile = virtualFile;
        
        setLayout(new BorderLayout());
        
        initializeUI();
    }
    
    private void initializeUI() {
        // 获取组件信息
        ComponentInfo info = PluginCache.componentInfoMap.get(this.bizId + "_" + componentId);
        if (info == null) {
            showEmptyPanel("未找到组件配置信息");
            return;
        }
        
        // 根据组件类型创建对应的配置面板
        JPanel typeSpecificPanel = createTypeSpecificPanel(info.getType());
        if (typeSpecificPanel == null) {
            showEmptyPanel("暂不支持该类型组件配置");
            return;
        }
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("确定");
        // JButton cancelButton = new JButton("取消");
        
        okButton.addActionListener(e -> {
            saveCurrentSettings();
            refresh();
        });
        
        // cancelButton.addActionListener(e -> {
        //     if (currentDialog != null) {
        //         currentDialog.loadExistingSettings();
        //     }
        //     refresh();
        // });
        
        buttonPanel.add(okButton);
        //buttonPanel.add(cancelButton);
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(typeSpecificPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        removeAll();
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

    }

    // 添加显示空面板的方法
    private void showEmptyPanel(String message) {
        removeAll();
        setLayout(new BorderLayout());
        
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        JLabel messageLabel = new JLabel(message);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyPanel.add(messageLabel);
        
        add(emptyPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public String getCacheComponentId(String componentId) {
        return this.bizId + "_" + componentId;
    }

    private JPanel createTypeSpecificPanel(String componentType) {
        JPanel panel = null;
        switch (componentType) {
            case "flow-print":
                PrintComponentSettingsDialog dialog = new PrintComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-print", this.virtualFile);
                currentDialog = dialog;
                panel = dialog.createConfigurationPanel();
                break;
            case "flow-assign":
                AssignComponentSettingsDialog dialogAssign = new AssignComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-assign",this.virtualFile);
                currentDialog = dialogAssign;
                panel = dialogAssign.createConfigurationPanel();
                break;
            case "flow-sql":
                SQLComponentSettingsDialog dialogSQL = new SQLComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-sql",this.virtualFile);
                currentDialog = dialogSQL;
                panel = dialogSQL.createConfigurationPanel();
                break;
            case "flow-groovy":
                GroovyComponentSettingsDialog dialogJS = new GroovyComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-groovy",this.virtualFile);
                currentDialog = dialogJS;
                panel = dialogJS.createConfigurationPanel();
                break;
            case "flow-date":
                DateComponentSettingsDialog dialogDate = new DateComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-date",this.virtualFile);
                currentDialog = dialogDate;
                panel = dialogDate.createConfigurationPanel();
                break;
            case "flow-base64":
                Base64ComponentSettingsDialog dialogBase64 = new Base64ComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-base64",this.virtualFile);
                currentDialog = dialogBase64;
                panel = dialogBase64.createConfigurationPanel();
                break;
            case "flow-number":
                NumberComponentSettingsDialog dialogNumber = new NumberComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-number",this.virtualFile);
                currentDialog = dialogNumber;
                panel = dialogNumber.createConfigurationPanel();
                break;
            case "flow-type2type":
                Type2TypeComponentSettingsDialog dialogType2Type = new Type2TypeComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-type2type",this.virtualFile);
                currentDialog = dialogType2Type;
                panel = dialogType2Type.createConfigurationPanel();
                break;
            case "flow-random":
                RandomComponentSettingsDialog dialogRandom = new RandomComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-random",this.virtualFile);
                currentDialog = dialogRandom;
                panel = dialogRandom.createConfigurationPanel();
                break;
            case "flow-uniqueId":
                UniqueIdComponentSettingsDialog dialogUniqueId = new UniqueIdComponentSettingsDialog(
                    componentId,  getCacheComponentId(componentId),project, canvasPanel, "flow-uniqueId",this.virtualFile);
                currentDialog = dialogUniqueId;
                panel = dialogUniqueId.createConfigurationPanel();
                break;
            case "flow-sys_config":
                SysConfigComponentSettingsDialog dialogSysConfig = new SysConfigComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-sys_config",this.virtualFile);
                currentDialog = dialogSysConfig;
                panel = dialogSysConfig.createConfigurationPanel();
                break;
            case "flow-custom-refer":
                CustomReferComponentSettingsDialog dialogCustomRefer = new CustomReferComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-custom-refer",this.virtualFile);
                currentDialog = dialogCustomRefer;
                panel = dialogCustomRefer.createConfigurationPanel();
                break;
            case "flow-exception":
                ExceptionComponentSettingsDialog dialogException = new ExceptionComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-exception",this.virtualFile);
                currentDialog = dialogException;
                panel = dialogException.createCenterPanel();
                break;
            case "flow-file-upload":
                FileUploadComponentSettingsDialog dialogFileUpload = new FileUploadComponentSettingsDialog(
                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-file-upload",this.virtualFile);
                currentDialog = dialogFileUpload;
                panel = dialogFileUpload.createConfigurationPanel();
                break;
//            case "flow-http":
//                HttpComponentSettingsDialog dialogHttp = new HttpComponentSettingsDialog(
//                    componentId, getCacheComponentId(componentId), project, canvasPanel, "flow-http",this.virtualFile);
//                currentDialog = dialogHttp;
//                panel = dialogHttp.createConfigurationPanel();
//                break;
            default:
                currentDialog = null;
                return null;
        }
        
        // 如果panel创建失败，返回null
        if (panel == null) {
            currentDialog = null;
            return null;
        }
        
        // 用JBScrollPane包装panel
//        JBScrollPane scrollPane = new JBScrollPane(panel);
//        scrollPane.setBorder(null);
//        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // 创建一个容器面板来包含滚动面板
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBorder(null);
        containerPanel.add(panel, BorderLayout.CENTER);
        
        return containerPanel;
    }
    
    private void saveCurrentSettings() {
        if (currentDialog != null) {
            // 调用对话框的保存方法
            if (currentDialog instanceof PrintComponentSettingsDialog) {
                ((PrintComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof AssignComponentSettingsDialog) {
                ((AssignComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof SQLComponentSettingsDialog) {
                ((SQLComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof DateComponentSettingsDialog) {
                ((DateComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof Base64ComponentSettingsDialog) {
                ((Base64ComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof NumberComponentSettingsDialog) {
                ((NumberComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof RandomComponentSettingsDialog) {
                ((RandomComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof UniqueIdComponentSettingsDialog) {
                ((UniqueIdComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof SysConfigComponentSettingsDialog) {
                ((SysConfigComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof CustomReferComponentSettingsDialog) {
                ((CustomReferComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof ExceptionComponentSettingsDialog) {
                ((ExceptionComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof GroovyComponentSettingsDialog) {
                ((GroovyComponentSettingsDialog) currentDialog).saveSettings();
            } else if (currentDialog instanceof Type2TypeComponentSettingsDialog) {
                ((Type2TypeComponentSettingsDialog) currentDialog).saveSettings();
            }
        }
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
    }
    
    public void refresh() {
        removeAll();
        initializeUI();
        revalidate();
        repaint();
    }
} 