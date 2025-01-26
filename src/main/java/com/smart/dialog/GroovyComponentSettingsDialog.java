package com.smart.dialog;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.smart.bean.ComponentInfo;
import com.smart.bean.ComponentProp;
import com.smart.cache.PluginCache;
import com.smart.event.EventBus;
import com.smart.event.EventType;
import com.smart.ui.GroovyAIPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class GroovyComponentSettingsDialog extends CommonDialog {
    private Project project;
    private String componentId;
    private EditorEx sqlEditor;
    private JButton aiButton;
    private JDialog aiDialog;
    private GroovyAIPanel aiPanel;

    public GroovyComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("Groovy脚本配置");
    }

    @Override
    protected JComponent createCenterPanel() {
        return createConfigurationPanel();
    }

    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        
        // 创建多行文本区域
        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setText("Groovy脚本使用说明:\n" +
                              "1. 在此编辑器中编写Groovy脚本代码\n" + 
                              "2. 脚本将在流程执行时被调用\n" +
                              "3. 可以使用内置变量 $vars.get(key) 访问上下文信息\n" +
                              "4. 可以使用内置变量 $vars.put(key, value) 设置上下文信息");
        
        // 设置文本区域属性
        descriptionArea.setEditable(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setOpaque(false);
        descriptionArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        descriptionArea.setForeground(new Color(80, 80, 80));
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 将文本区域添加到滚动面板
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        scrollPane.setPreferredSize(new Dimension(600, 80));
        scrollPane.setBorder(null);
        
        // 添加AI按钮到工具栏
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        aiButton = new JButton("AI助手");
        aiButton.addActionListener(e -> toggleAIDialog());
        toolbarPanel.add(aiButton);
        
        // 组合描述区域和工具栏
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(scrollPane, BorderLayout.CENTER);
        topPanel.add(toolbarPanel, BorderLayout.SOUTH);
        dialogPanel.add(topPanel, BorderLayout.NORTH);

        // 创建中央面板
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        
        // Groovy编辑器面板
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createTitledBorder("Groovy脚本"));
        
        // 创建Groovy编辑器
        sqlEditor = (EditorEx)EditorFactory.getInstance().createEditor(
            EditorFactory.getInstance().createDocument(""),
            project,
            PlainTextFileType.INSTANCE,
            false
        );
        
        // 配置编辑器设置
        configureEditor(sqlEditor);
        
        // 设置编辑器大小
        JComponent editorComponent = sqlEditor.getComponent();
        editorComponent.setPreferredSize(new Dimension(600, 400));
        editorComponent.setMinimumSize(new Dimension(500, 300));
        
        editorPanel.add(sqlEditor.getComponent(), BorderLayout.CENTER);
        centerPanel.add(editorPanel, BorderLayout.CENTER);
        dialogPanel.add(centerPanel, BorderLayout.CENTER);
        
        // 加载现有设置
        loadExistingSettings();
        
        // 设置整个对话框的首选大小
        dialogPanel.setPreferredSize(new Dimension(800, 600));
        
        return dialogPanel;
    }

    private void configureEditor(EditorEx editor) {
        EditorSettings settings = editor.getSettings();
        settings.setFoldingOutlineShown(true);
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setIndentGuidesShown(true);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(false);
        settings.setAdditionalColumnsCount(3);
        settings.setAdditionalLinesCount(3);
        settings.setRightMarginShown(true);
        settings.setRightMargin(120);
        settings.setUseSoftWraps(true);
        settings.setUseTabCharacter(false);
    }

    private void toggleAIDialog() {
        if (aiDialog == null || !aiDialog.isDisplayable()) {
            // 创建对话框
            Window window = SwingUtilities.getWindowAncestor(this.getContentPane());
            if (window instanceof Frame) {
                aiDialog = new JDialog((Frame) window, "Groovy AI助手", false);
            } else if (window instanceof Dialog) {
                aiDialog = new JDialog((Dialog) window, "Groovy AI助手", false);
            } else {
                aiDialog = new JDialog((Frame) null, "Groovy AI助手", false);
            }
            
            aiPanel = new GroovyAIPanel(project, sqlEditor.getDocument());
            aiPanel.welcome();
            
            // 设置对话框内容和属性
            aiDialog.setContentPane(aiPanel);
            aiDialog.setSize(500, 600);
            
            // 计算对话框位置
            Point editorLocation = sqlEditor.getComponent().getLocationOnScreen();
            Dimension editorSize = sqlEditor.getComponent().getSize();
            
            // 将对话框放在编辑器的右侧
            aiDialog.setLocation(
                editorLocation.x + editorSize.width + 10,
                editorLocation.y
            );
            
            // 添加窗口移动监听
            window.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentMoved(ComponentEvent e) {
                    updateAIDialogPosition();
                }
                
                @Override
                public void componentResized(ComponentEvent e) {
                    updateAIDialogPosition();
                }
            });
            
            // 显示对话框
            aiDialog.setVisible(true);
        } else {
            // 切换对话框的可见性
            aiDialog.setVisible(!aiDialog.isVisible());
        }
    }

    // 更新AI对话框位置
    private void updateAIDialogPosition() {
        if (aiDialog != null && aiDialog.isVisible()) {
            Point editorLocation = sqlEditor.getComponent().getLocationOnScreen();
            Dimension editorSize = sqlEditor.getComponent().getSize();
            
            aiDialog.setLocation(
                editorLocation.x + editorSize.width + 10,
                editorLocation.y
            );
        }
    }

    public String getCode(){
        return sqlEditor.getDocument().getText();
    }


    public void loadExistingSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        ComponentProp componentProp = componentInfo.getComponentProp();
        if (componentProp == null) {
            componentProp = new ComponentProp();
            componentInfo.setComponentProp(componentProp);
            return;
        }
        //================属性=====================
        if(componentProp.getScript() != null) {
            setSqlText(componentProp.getScript());
        }
    }

    @Override
    protected void doOKAction() {
        if (validateSettings()) {
            saveSettings();
            super.doOKAction();
        }
    }

    // 保存配置
    public void saveSettings() {
        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(cacheComponentId);
        ComponentProp componentProp = componentInfo.getComponentProp();
        if (componentProp == null) {
            componentProp = new ComponentProp();
        }
        componentProp.setScript(getSqlText());
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(componentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }

    private boolean validateSettings() {

        String sql = getSqlText().trim();
        if (sql.isEmpty()) {
            Messages.showWarningDialog("脚本不能为空", "验证失败");
            return false;
        }

        return true;
    }

    @Override
    public void dispose() {
        if (aiDialog != null) {
            aiDialog.dispose();
        }
        if (sqlEditor != null) {
            EditorFactory.getInstance().releaseEditor(sqlEditor);
        }
        super.dispose();
    }

    // Getter和Setter方法
    public String getSqlText() {
        if (sqlEditor == null) {
            return "";
        }
        String sql = sqlEditor.getDocument().getText();
        // 不需要重复添加CDATA
        if (!sql.trim().startsWith("<![CDATA[")) {
            sql = "<![CDATA[" + sql + "]]>";
        }
        return sql;
    }

    public void setSqlText(String sql) {
        if (sqlEditor != null) {
            // 如果SQL包含CDATA，则去除CDATA标记
            if (sql != null) {
                sql = removeCDATA(sql);
            }
            final String finalSql = sql;
            com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction(() -> {
                sqlEditor.getDocument().setText(finalSql);
            });
        }
    }

    // 添加一个辅助方法来移除CDATA标记
    private String removeCDATA(String sql) {
        sql = sql.trim();
        if (sql.startsWith("<![CDATA[") && sql.endsWith("]]>")) {
            sql = sql.substring(9, sql.length() - 3);
        }
        return sql;
    }

}
