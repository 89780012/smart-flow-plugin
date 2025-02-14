package com.smart.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.smart.bean.ComponentInfo;
import com.smart.bean.ComponentItem;
import com.smart.bean.ComponentProp;
import com.smart.bean.ParamProp;
import com.smart.cache.PluginCache;
import com.smart.enums.DataType;
import com.smart.enums.ParamType;
import com.smart.enums.ThreadType;
import com.smart.enums.ValueCategory;

import com.smart.tasks.AsyncTaskManager;
import com.smart.utils.StringUtils;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.*;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ComponentUtils {

    // 搜索bean引用
    public static void searchBean(ActionEvent e, CommonDialog parentDialog, Project project) {
        // 创建一个继承 DialogWrapper 的内部类
        class SearchBeanDialog extends DialogWrapper {
            private JTextField searchField;
            private JList<String> beanList;
            private DefaultListModel<String> listModel;
            private JLabel loadingLabel;
            private JButton refreshButton;

            protected SearchBeanDialog() {
                super(project, false); // false 表示非模态
                setTitle("搜索Bean");
                init(); // 这会调用 createCenterPanel
            }

            @Override
            protected JComponent createCenterPanel() {
                JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));

                // 顶部搜索面板
                JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
                searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                searchField = new JTextField();
                searchPanel.add(searchField, BorderLayout.CENTER);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                refreshButton = new JButton("刷新Bean");
                buttonPanel.add(refreshButton);
                searchPanel.add(buttonPanel, BorderLayout.EAST);

                // 创建列表
                listModel = new DefaultListModel<>();
                beanList = new JList<>(listModel);
                beanList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                // 加载提示标签
                loadingLabel = new JLabel("正在刷新...", SwingConstants.CENTER);
                loadingLabel.setVisible(false);

                JPanel centerPanel = new JPanel(new BorderLayout());
                centerPanel.add(new JScrollPane(beanList), BorderLayout.CENTER);
                centerPanel.add(loadingLabel, BorderLayout.NORTH);

                // 添加到主面板
                dialogPanel.add(searchPanel, BorderLayout.NORTH);
                dialogPanel.add(centerPanel, BorderLayout.CENTER);

                // 初始化数据
                updateBeanList(listModel, "", project);

                // 添加实时搜索
                Timer searchTimer = new Timer(300, null);
                searchTimer.setRepeats(false);

                searchField.getDocument().addDocumentListener(new DocumentListener() {
                    private void searchWithDelay() {
                        searchTimer.stop();
                        searchTimer.setActionCommand(searchField.getText());
                        searchTimer.addActionListener(e -> {
                            updateBeanList(listModel, searchField.getText(), project);
                            searchTimer.removeActionListener(searchTimer.getActionListeners()[0]);
                        });
                        searchTimer.start();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) { searchWithDelay(); }
                    @Override
                    public void removeUpdate(DocumentEvent e) { searchWithDelay(); }
                    @Override
                    public void changedUpdate(DocumentEvent e) { searchWithDelay(); }
                });

                // 刷新按钮事件
                refreshButton.addActionListener(ae -> {
                    // 显示加载状态
                    beanList.setEnabled(false);
                    loadingLabel.setVisible(true);
                    refreshButton.setEnabled(false);
                    searchField.setEnabled(false);

                    listModel.clear();
                    listModel.addElement("加载中...");

                    AsyncTaskManager.getInstance().refreshSpringBeans(project);

                    Timer timer = new Timer(500, e1 -> {
                        try {
                            updateBeanList(listModel, searchField.getText(), project);
                        } finally {
                            beanList.setEnabled(true);
                            loadingLabel.setVisible(false);
                            refreshButton.setEnabled(true);
                            searchField.setEnabled(true);
                            ((Timer) e1.getSource()).stop();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                });

                // 双击选择
                beanList.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            String selected = beanList.getSelectedValue();
                            if (selected != null) {
                                handleBeanSelection(selected, parentDialog, project);
                                close(OK_EXIT_CODE);
                            }
                        }
                    }
                });

                return dialogPanel;
            }

            @Override
            protected void doOKAction() {
                String selected = beanList.getSelectedValue();
                if (selected != null) {
                    handleBeanSelection(selected, parentDialog, project);
                }
                super.doOKAction();
            }

            @Override
            public JComponent getPreferredFocusedComponent() {
                return searchField;
            }
        }

        // 创建并显示对话框
        SearchBeanDialog dialog = new SearchBeanDialog();
        dialog.setModal(false);
        dialog.setSize(400, 500);
        dialog.show();
    }

    // 更新Bean列表
    private static void updateBeanList(DefaultListModel<String> model, String filter, Project project) {
        model.clear();
        Map<String, PsiClass> beans = PluginCache.springBeanClasses;
        if (beans != null && !beans.isEmpty()) {
            beans.keySet().stream()
                .filter(name -> filter.isEmpty() || name.toLowerCase().contains(filter.toLowerCase()))
                .sorted()
                .forEach(model::addElement);
        } else {
            model.addElement("未找到Bean");
        }
    }

    // 处理Bean选择
    private static void handleBeanSelection(String beanName, CommonDialog dialog, Project project) {
        PsiClass selectedClass = PluginCache.springBeanClasses.get(beanName);
        if (selectedClass != null) {
            dialog.getBeanReferenceField().setText(beanName);
            dialog.setLastSelectedBeanFile(selectedClass.getContainingFile());
        }
    }

    // 搜索方法
    public static void searchMethod(ActionEvent e, CommonDialog dialog, Project project) {
        String beanReference = dialog.getBeanReferenceField().getText();
        if (StringUtils.isEmpty(beanReference)) {
            Messages.showMessageDialog("请先选择Bean", "提示", Messages.getInformationIcon());
            return;
        }

        // 如果是手动输入的情况，需要根据bean名称查找对应的类文件
        if (dialog.getLastSelectedBeanFile() == null) {
            // 从所有Spring Bean中查找匹配的类
            Map<String,PsiClass> springBeanClasses = PluginCache.springBeanClasses;
            for (String beanName : springBeanClasses.keySet()) {
                PsiClass psiClass = springBeanClasses.get(beanName);
                if (beanName.equals(beanReference)) {
                    dialog.setLastSelectedBeanFile(psiClass.getContainingFile());
                    break;
                }
            }
            
            // 如果还是没找到对应的文件
            if (dialog.getLastSelectedBeanFile() == null) {
                Messages.showMessageDialog("未找到对应的Bean类文件，请通过搜索按钮选择Bean", "提示", Messages.getInformationIcon());
                return;
            }
        }

        // 打开方法选择对话框
        ChooseMethodDialog methodDialog = new ChooseMethodDialog(project, dialog.getLastSelectedBeanFile());
        if (methodDialog.showAndGet()) {
            PsiMethod selectedMethod = methodDialog.getSelectedMethod();
            if (selectedMethod != null) {
                // 设置方法名
                String methodName = selectedMethod.getContainingClass().getQualifiedName() + "#" + selectedMethod.getName();
                dialog.getMethodNameField().setText(methodName);
                
                // 设置方法文件和元素
                dialog.setLastSelectedMethodFile(selectedMethod.getContainingFile());
                dialog.setLastSelectedMethodElement(selectedMethod);
            }
        }
    }

    public static String extractPackageName(PsiFile file) {
        if (file instanceof PsiJavaFile) {
            PsiJavaFile javaFile = (PsiJavaFile) file;
            return javaFile.getPackageName();
        }
        return "";
    }

    // 保存设置到源代码
//    public static void saveSettings(String componentId, CommonDialog dialog) {
//
//        String paramName = String.valueOf(dialog.paramsTableModel.getValueAt(0, 0));
//        String keyName = String.valueOf(dialog.paramsTableModel.getValueAt(0, 1));
//        String dataType = String.valueOf(dialog.paramsTableModel.getValueAt(0, 2));
//        String val = String.valueOf(dialog.paramsTableModel.getValueAt(0, 3));
//        String valType = String.valueOf(dialog.paramsTableModel.getValueAt(0, 4));
//        String valDesc = String.valueOf(dialog.paramsTableModel.getValueAt(0, 5));
//
//        ComponentInfo componentInfo = PluginCache.componentInfoMap.get(componentId);
//        ComponentProp componentProp = componentInfo.getComponentProp();
//        if (componentProp == null) {
//            componentProp = new ComponentProp();
//        }
//        componentProp.setBeanRef(dialog.beanReferenceField.getText());
//        componentProp.setMethod(dialog.methodNameField.getText());
//
//        String threadTypeName = String.valueOf(dialog.threadTypeComboBox.getSelectedItem());
//        componentProp.setThreadType(ThreadType.valueOf(threadTypeName));
//
//        List<ParamProp> props = new ArrayList<>();
//        ParamProp paramProp = new ParamProp();
//
//        paramProp.setParamType(ParamType.valueOf(paramName));
//        paramProp.setKeyName(keyName);
//        paramProp.setDataType(DataType.valueOf(dataType));
//        paramProp.setVal(val);
//        paramProp.setValueCategory(ValueCategory.valueOf(valType));
//        paramProp.setValDesc(valDesc);
//        props.add(paramProp);
//        componentProp.setParamProps(props);
//        componentInfo.setComponentProp(componentProp);
//        PluginCache.componentInfoMap.put(componentId, componentInfo);
//        EventBus.getInstance().post(EventType.UPDATE_COMPONENT_SETTINGS + "_" + file.getPath(), componentId);
//    }

    public static boolean setConfigValue(CommonDialog dialog,ComponentInfo componentInfo){
        ComponentItem item = PluginCache.componentItemMap.get(componentInfo.getType());
        boolean flag = false;  //是否存在配置
        if(item != null){
            if(!StringUtils.isEmpty(item.getBeanRef())){
                dialog.beanReferenceField.setText(item.getBeanRef());
                flag = true;
            }
            if(!StringUtils.isEmpty(item.getMethod())){
                dialog.methodNameField.setText(item.getMethod());
                flag = true;
            }
        }
        return flag;
    }

    public static void setBeanReferenceField(String beanRef, CommonDialog dialog) {
        for (String beanName : PluginCache.springBeanClasses.keySet()) {
            PsiClass psiClass = PluginCache.springBeanClasses.get(beanName);
            if (beanName.equals(beanRef)) {
                dialog.setLastSelectedBeanFile(psiClass.getContainingFile());
                break;
            }
        }
    }

    public static void setMethodReferenceField(String methodName, CommonDialog dialog) {
        String[] parts = methodName.split("#");
        if (parts.length == 2) {
            String methodSimpleName = parts[1];
            if (dialog.getLastSelectedBeanFile() instanceof PsiJavaFile) {
                PsiJavaFile javaFile = (PsiJavaFile) dialog.getLastSelectedBeanFile();
                for (PsiClass aClass : javaFile.getClasses()) {
                    if (aClass != null) {
                        for (PsiMethod method : aClass.getMethods()) {
                            if (method != null && method.getName().equals(methodSimpleName)) {
                                dialog.setLastSelectedMethodFile(dialog.getLastSelectedBeanFile());
                                dialog.setLastSelectedMethodElement(method);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public static DefaultTableCellRenderer createTableHeaderRenderer(){
        // 建一个自定义的表头渲染器
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                        row, column);
                comp.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.LIGHT_GRAY));
                comp.setBackground(new Color(237, 237, 237)); // IDEA 风格的浅灰色背景
                comp.setForeground(Color.BLACK);
                comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                return comp;
            }
        };
        return headerRenderer;
    }

    // 创建参数类型下拉框
    public static DefaultCellEditor createParamTypeComboBox(){
        JComboBox<ParamType> paramTypeComboBox = new JComboBox<>(ParamType.values());
        paramTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ParamType) {
                    setText(((ParamType) value).getDisplayName());
                }
                return this;
            }
        });
        return new DefaultCellEditor(paramTypeComboBox);
    }

    // 设置数据类型列的下拉框
    public static DefaultCellEditor createDataTypeComboBox(){
        JComboBox<DataType> dataTypeComboBox = new JComboBox<>(DataType.values());
        dataTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DataType) {
                    setText(((DataType) value).getDisplayName());
                }
                return this;
            }
        });
        return new DefaultCellEditor(dataTypeComboBox);
    }

    // 创建值类型列的下拉框
    public static DefaultCellEditor createValueTypeComboBox(){
        JComboBox<ValueCategory> valueTypeComboBox = new JComboBox<>(ValueCategory.values());
        valueTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ValueCategory) {
                    setText(((ValueCategory) value).getDisplayName());
                }
                return this;
            }
        });
        return new DefaultCellEditor(valueTypeComboBox);
    }

    // 创建参数类型的单元格渲染器
    public static DefaultTableCellRenderer createParamTypeRenderer(){
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof ParamType) {
                    setText(((ParamType) value).getDisplayName());
                }
                // 重置为默认背景色
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);

                // 只有当前选中的单元格才改变颜色
                if (hasFocus && table.getSelectedRow() == row && table.getSelectedColumn() == column) {
                    setBackground(new Color(255, 255, 200)); // 浅黄色背景
                    setForeground(Color.BLACK); // 确保文字为黑色
                }

                // 设置边框
                setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                return this;
            }
        };
    }

    // 创建数据类型的单元格渲染器
    public static DefaultTableCellRenderer createDataTypeRenderer(){
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof DataType) {
                    setText(((DataType) value).getDisplayName());
                }
                
                // 重置为默认背景色
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);

                // 只有当前选中的单元格才改变颜色
                if (hasFocus && table.getSelectedRow() == row && table.getSelectedColumn() == column) {
                    setBackground(new Color(255, 255, 200)); // 浅黄色背景
                    setForeground(Color.BLACK); // 确保文字为黑色
                }

                // 设置边框
                setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                        return this;
            }
        };
    }

    // 创建数据类型的单元格渲染器
    public static DefaultTableCellRenderer createValueTypeRenderer(){
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof ValueCategory) {
                    setText(((ValueCategory) value).getDisplayName());
                }
                // 重置为默认背景色
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);

                // 只有当前选中的单元格才改变颜色
                if (hasFocus && table.getSelectedRow() == row && table.getSelectedColumn() == column) {
                    setBackground(new Color(255, 255, 200)); // 浅黄色背景
                    setForeground(Color.BLACK); // 确保文字为黑色
                }

                // 设置边框
                setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                return this;
            }
        };
    }

    // 创建数据类型的单元格渲染器
    public static DefaultTableCellRenderer createDescRenderer(){
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // 设置背景色为浅灰色
                if (!isSelected) {
                    c.setBackground(new Color(240, 240, 240));
                }
                return this;
            }
        };
    }



}
