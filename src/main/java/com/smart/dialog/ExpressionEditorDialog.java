package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ExpressionEditorDialog extends DialogWrapper {
    private EditorTextField expressionEditor;
    private ComboBox<String> languageComboBox;
    private String expression;
    private String language;
    private VirtualFile file;
    public ExpressionEditorDialog(Project project, String initialExpression, String initialLanguage, VirtualFile file) {
        super(project);
        this.expression = initialExpression != null ? initialExpression : "";
        this.language = initialLanguage != null ? initialLanguage : "javascript";
        this.file = file;
        setTitle("编辑连接线表达式");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建描述区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        
        // 创建多行文本区域
        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setText("表达式编辑器使用说明:\n" +
                              "1. 支持标准Groovy表达式语法\n" +
                              "2. 可使用 [变量名] 引用上下文变量\n" +
                              "3. 需要携带return关键字, 返回布尔值\n" +
                              "4. 示例: return age > 18");
        
        // 设置文本区域属性
        descriptionArea.setEditable(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setOpaque(false);
        descriptionArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        descriptionArea.setForeground(new Color(80, 80, 80));
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 将文本区域添加到滚动面板
        JScrollPane scrollPaneDes = new JScrollPane(descriptionArea);
        scrollPaneDes.setPreferredSize(new Dimension(600, 100));
        scrollPaneDes.setBorder(null);
        
        headerPanel.add(scrollPaneDes, BorderLayout.CENTER);
        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 原有的编辑器面板
        JPanel editorPanel = new JPanel(new BorderLayout());
        // 创建语言选择下拉框
        languageComboBox = new ComboBox<>(new String[]{"groovy"});
        languageComboBox.setSelectedItem(language);
        
        editorPanel.add(new JLabel("语言:"));
        editorPanel.add(languageComboBox);

        // 创建表达式编辑器
        expressionEditor = new EditorTextField(expression);
        expressionEditor.setOneLineMode(false);
        expressionEditor.setPreferredSize(new Dimension(580, 350));

        JBScrollPane scrollPane = new JBScrollPane(expressionEditor);
        editorPanel.add(scrollPane, BorderLayout.CENTER);

        dialogPanel.add(editorPanel, BorderLayout.CENTER);
        return dialogPanel;
    }

    public String getExpression() {
        return expressionEditor.getText();
    }

    public String getLanguage() {
        return (String) languageComboBox.getSelectedItem();
    }
} 