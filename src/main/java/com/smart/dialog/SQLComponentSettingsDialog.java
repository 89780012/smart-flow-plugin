package com.smart.dialog;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBLabel;
import com.smart.bean.ComponentInfo;
import com.smart.bean.ComponentProp;
import com.smart.bean.ParamProp;
import com.smart.cache.PluginCache;
import com.smart.enums.*;
import com.smart.event.EventBus;
import com.smart.event.EventType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import com.smart.ui.SQLAIPanel;
import com.smart.utils.FlowDataSourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class SQLComponentSettingsDialog extends CommonDialog {
    private Project project;
    private String componentId;
    private EditorEx sqlEditor;
    private JComboBox<ReturnType> returnTypeComboBox;
    private JComboBox<String> dataSourceComboBox;
    private JLabel descriptionLabel;
    private JComboBox<PaginationType> paginationTypeComboBox;
    private JButton aiButton;
    private JDialog aiDialog;
    private SQLAIPanel aiPanel;
    private JComboBox<SQLOperationType> operationTypeComboBox;

    public SQLComponentSettingsDialog(String componentId, String cacheComponentId, Project project, JPanel canvasPanel, String type, VirtualFile file) {
        super(false);
        this.componentId = componentId;
        this.cacheComponentId = cacheComponentId;
        this.project = project;
        this.canvasPanel = canvasPanel;
        this.type = type;
        this.file = file;
        init();
        setTitle("SQL配置");
    }

    @Override
    protected JComponent createCenterPanel() {
        // 不再调用super.createCenterPanel()，直接返回我们自己创建的面板
        return createConfigurationPanel();
    }

    public JPanel createConfigurationPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建描述区域和工具栏面板
        JPanel headerPanel = new JPanel(new BorderLayout());
        
        // 左侧描述标签
        descriptionLabel = new JLabel("SQL组件,用于执行一些较为简单的SQL操作");
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 右侧工具栏
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        aiButton = new JButton("AI助手");
        aiButton.addActionListener(e -> toggleAIDialog());
        toolbarPanel.add(aiButton);
        
        // 组合描述和工具栏
        headerPanel.add(descriptionLabel, BorderLayout.CENTER);
        headerPanel.add(toolbarPanel, BorderLayout.EAST);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央面板
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        
        // 数据源选择面板
        JPanel dataSourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataSourcePanel.add(new JLabel("数据源:"));
        dataSourceComboBox = FlowDataSourceUtils.getDataSourceComboBox( project, file);
        dataSourcePanel.add(dataSourceComboBox);

        JLabel operationTypeLabel = new JLabel("操作类型:");
        operationTypeLabel.setForeground(Color.RED);
        dataSourcePanel.add(operationTypeLabel);
        operationTypeComboBox = createOperationTypeComboBox();
        dataSourcePanel.add(operationTypeComboBox);
        
        dataSourcePanel.add(new JLabel("是否分页(查询生效):"));
        paginationTypeComboBox = createPaginationTypeComboBox();
        dataSourcePanel.add(paginationTypeComboBox);
        
        centerPanel.add(dataSourcePanel, BorderLayout.NORTH);

        // SQL编辑器面板
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createTitledBorder("SQL语句"));
        
        // 创建SQL编辑器
        sqlEditor = (EditorEx)EditorFactory.getInstance().createEditor(
            EditorFactory.getInstance().createDocument(""),
            project,
            PlainTextFileType.INSTANCE,
            false
        );
        
        // 配置编辑器设置
        configureEditor(sqlEditor);
        
        // 设置编辑器面板的首选大小和最小大小
        JComponent editorComponent = sqlEditor.getComponent();
        editorComponent.setPreferredSize(new Dimension(600, 400));  // 设置首选大小
        editorComponent.setMinimumSize(new Dimension(500, 300));    // 设置最小大小
        
        // SQL提示面板
        JPanel sqlHintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JBLabel hintLabel = new JBLabel("提示: 支持标准SQL语法，可以使用#{paramName}作为参数占位符");
        sqlHintPanel.add(hintLabel);
        
        editorPanel.add(sqlHintPanel, BorderLayout.NORTH);
        editorPanel.add(sqlEditor.getComponent(), BorderLayout.CENTER);

        // 返回值类型和绑定参数设置面板
        JPanel returnTypeAndBindKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        returnTypeAndBindKeyPanel.add(new JLabel("返回值类型(查询有效、增删改返回数量,整型):"));
        returnTypeComboBox = createReturnTypeComboBox();
        returnTypeAndBindKeyPanel.add(returnTypeComboBox);

        returnTypeAndBindKeyPanel.add(new JLabel("绑定参数:"));
        bindKeyTextField = new JTextField(20);
        returnTypeAndBindKeyPanel.add(bindKeyTextField);

        // 将编辑器和返回值类型、绑定参数面板添加到中央面板
        JPanel sqlPanel = new JPanel(new BorderLayout(0, 5));
        sqlPanel.add(editorPanel, BorderLayout.CENTER);
        sqlPanel.add(returnTypeAndBindKeyPanel, BorderLayout.SOUTH);
        centerPanel.add(sqlPanel, BorderLayout.CENTER);
        
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

    private JComboBox<ReturnType> createReturnTypeComboBox() {
        JComboBox<ReturnType> comboBox = new JComboBox<>(ReturnType.values());
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof ReturnType) {
                    value = ((ReturnType) value).getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        return comboBox;
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
        // 设置数据源
        if (componentProp.getDataSourceKey() != null) {
            dataSourceComboBox.setSelectedItem(componentProp.getDataSourceKey());
        }
        if(componentProp.getSql() != null) {
            setSqlText(componentProp.getSql());
        }
        if(componentProp.getReturnType() != null) {
            setSelectedReturnType(componentProp.getReturnType());
        }
        if(componentProp.getBindKey() != null) {
            bindKeyTextField.setText(componentProp.getBindKey());
        }
        if(componentProp.getPaginationType() != null) {
            paginationTypeComboBox.setSelectedItem(componentProp.getPaginationType());
        }
        if(componentProp.getOperationType() != null) {
            operationTypeComboBox.setSelectedItem(componentProp.getOperationType());
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
        
        // 保存数据源设置
        String selectedDataSource = (String) dataSourceComboBox.getSelectedItem();
        componentProp.setDataSourceKey(selectedDataSource);
        
        // 保存SQL文本（包含CDATA）
        componentProp.setSql(getSqlText());
        componentProp.setReturnType(getSelectedReturnType());
        componentProp.setBindKey(bindKeyTextField.getText());
        componentProp.setPaginationType((PaginationType) paginationTypeComboBox.getSelectedItem());
        componentProp.setOperationType((SQLOperationType) operationTypeComboBox.getSelectedItem());
        componentInfo.setComponentProp(componentProp);
        PluginCache.componentInfoMap.put(cacheComponentId, componentInfo);
        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
    }

    private boolean validateSettings() {
        if (dataSourceComboBox.getSelectedItem() == null) {
            Messages.showWarningDialog("请选择数据源", "验证失败");
            return false;
        }
        
        String sql = getSqlText().trim();
        if (sql.isEmpty()) {
            Messages.showWarningDialog("SQL语句不能为空", "验证失败");
            return false;
        }

        String bindKey = bindKeyTextField.getText();
        if (bindKey.isEmpty()) {
            Messages.showWarningDialog( "绑定参数不能为空", "验证失败");
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

    public ReturnType getSelectedReturnType() {
        return returnTypeComboBox.getSelectedItem() != null ? 
            (ReturnType) returnTypeComboBox.getSelectedItem() : ReturnType.LIST_MAP;
    }

    public void setSelectedReturnType(ReturnType returnType) {
        returnTypeComboBox.setSelectedItem(returnType);
    }

    private JComboBox<PaginationType> createPaginationTypeComboBox() {
        JComboBox<PaginationType> comboBox = new JComboBox<>(PaginationType.values());
        // 设置默认选项为不分页
        comboBox.setSelectedItem(PaginationType.NO);
        
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof PaginationType) {
                    value = ((PaginationType) value).getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        return comboBox;
    }

    private void toggleAIDialog() {
        if (aiDialog == null || !aiDialog.isDisplayable()) {
            // 创建对话框
            Window window = SwingUtilities.getWindowAncestor(this.getContentPane());
            if (window instanceof Frame) {
                aiDialog = new JDialog((Frame) window, "SQL AI助手", false);
            } else if (window instanceof Dialog) {
                aiDialog = new JDialog((Dialog) window, "SQL AI助手", false);
            } else {
                aiDialog = new JDialog((Frame) null, "SQL AI助手", false);
            }
            
            aiPanel = new SQLAIPanel(project, sqlEditor.getDocument());
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

    private JComboBox<SQLOperationType> createOperationTypeComboBox() {
        JComboBox<SQLOperationType> comboBox = new JComboBox<>(SQLOperationType.values());
        // 设置默认选项为查询
        comboBox.setSelectedItem(SQLOperationType.QUERY);
        
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof SQLOperationType) {
                    value = ((SQLOperationType) value).getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        return comboBox;
    }
}
