package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import com.smart.bean.*;
import com.smart.cache.PluginCache;
import com.smart.event.EventBus;
import com.smart.event.EventType;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ExceptionComponentSettingsDialog extends CommonDialog {
    private EditorTextField errorMessageEditor;
    private String componentId;
    private VirtualFile file;
    private Project project;

    public ExceptionComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("异常组件设置");

    }

    @Nullable
    @Override
    public JPanel createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(600, 400));

        // 创建描述标签
        JLabel descriptionLabel = new JLabel("请输入异常错误信息:");
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(descriptionLabel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 创建错误信息编辑器
        errorMessageEditor = new EditorTextField("");
        errorMessageEditor.setOneLineMode(false);
        errorMessageEditor.setPreferredSize(new Dimension(580, 350));

        JBScrollPane scrollPane = new JBScrollPane(errorMessageEditor);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 加载已有设置
        loadExistingSettings();

        return mainPanel;
    }

    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        if (componentInfo != null && componentInfo.getComponentProp() != null) {
            ComponentProp componentProp = componentInfo.getComponentProp();
            List<ParamProp> paramProps = componentProp.getParamProps();
            if (paramProps != null && !paramProps.isEmpty()) {
                errorMessageEditor.setText(paramProps.get(0).getVal());
            }
        }
    }

    @Override
    protected void doOKAction() {
        saveSettings();
        super.doOKAction();
    }

    public void saveSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        ComponentProp componentProp = componentInfo.getComponentProp();
        if (componentProp == null) {
            componentProp = new ComponentProp();
        }

        // 保存错误信息
        List<ParamProp> props = new ArrayList<>();
        ParamProp errorMessageProp = new ParamProp();
        errorMessageProp.setSeq("1");
        errorMessageProp.setVal(errorMessageEditor.getText());
        props.add(errorMessageProp);

        componentProp.setParamProps(props);
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }
}
