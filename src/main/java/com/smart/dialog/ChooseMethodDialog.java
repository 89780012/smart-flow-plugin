package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChooseMethodDialog extends DialogWrapper {
    private final Project project;
    private final PsiFile psiFile;
    private JBList<PsiMethod> methodList;
    private PsiMethod selectedMethod;

    public ChooseMethodDialog(Project project, PsiFile psiFile) {
        super(project);
        this.project = project;
        this.psiFile = psiFile;
        init();
        setTitle("选择方法");
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.setPreferredSize(new Dimension(400, 300));

        // 获取文件中的所有方法
        List<PsiMethod> methods = new ArrayList<>();
        if (psiFile instanceof PsiJavaFile) {
            PsiJavaFile javaFile = (PsiJavaFile) psiFile;
            PsiClass[] classes = javaFile.getClasses();
            for (PsiClass aClass : classes) {
                PsiMethod[] classMethods = aClass.getMethods();
                for (PsiMethod method : classMethods) {
                    // 只添加公共方法
                    if (method.getModifierList().hasModifierProperty("public")) {
                        methods.add(method);
                    }
                }
            }
        }

        // 创建方法列表
        methodList = new JBList<>(methods.toArray(new PsiMethod[0]));
        methodList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PsiMethod) {
                    PsiMethod method = (PsiMethod) value;
                    setText(method.getName() + "()");
                }
                return this;
            }
        });

        // 添加选择监听器
        methodList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedMethod = methodList.getSelectedValue();
            }
        });

        // 添加滚动面板
        JBScrollPane scrollPane = new JBScrollPane(methodList);
        dialogPanel.add(scrollPane, BorderLayout.CENTER);

        return dialogPanel;
    }

    public PsiMethod getSelectedMethod() {
        return selectedMethod;
    }

    @Override
    protected void doOKAction() {
        if (methodList.getSelectedValue() != null) {
            selectedMethod = methodList.getSelectedValue();
            super.doOKAction();
        }
    }
} 