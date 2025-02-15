package com.smart.dialog;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.table.JBTable;
import com.smart.bean.ComponentItem;
import com.smart.cache.PluginCache;
import com.smart.enums.ThreadType;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CommonDialog extends DialogWrapper {
    public VirtualFile file;
    public DefaultTableModel paramsTableModel;
    public JTextField beanReferenceField;
    public JTextField methodNameField;
    public JComboBox<Enum> threadTypeComboBox;
    public JBTable paramsTable;
    public PsiFile lastSelectedBeanFile;
    public JPanel canvasPanel;
    public PsiFile lastSelectedMethodFile;
    public PsiElement lastSelectedMethodElement;
    public Project project;
    public String type ; //打印组件
    public JTextField bindKeyTextField;
    public String cacheComponentId;


    public CommonDialog(boolean b) {
        super(b);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return null;
    }


    public void loadExistingSettings() {};

    //创建 bean 面板
    public JPanel createBeanPanel(boolean withThread) {
        ComponentItem item = PluginCache.componentItemMap.get(type);
        // 创建设置面板
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        //=================== Bean引用 ===================
        settingsPanel.add(new JLabel("Bean:"), gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        beanReferenceField = new JTextField(20);
        //不可编辑控制
        if(!item.isBeanEdit()){
            beanReferenceField.setEditable(false);  // 设置为不可编辑
            beanReferenceField.setBackground(UIManager.getColor("TextField.background")); // 保持背景色
        }
        settingsPanel.add(beanReferenceField, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        JButton searchBeanButton = new JButton("搜索");
        //不可编辑控制
        if(!PluginCache.componentItemMap.get(type).isBeanEdit()){
            searchBeanButton.setEnabled(false);
            searchBeanButton.setBackground(new Color(240, 240, 240));
        }
        searchBeanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComponentUtils.searchBean(e, CommonDialog.this, project);
            }
        });
        settingsPanel.add(searchBeanButton, gbc);

        // 修改打开Bean文件的按钮事件
        gbc.gridx++;
        JButton openBeanFileButton = new JButton("→");
        openBeanFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (lastSelectedBeanFile != null) {
                    // 在新窗口中打开文件
                    new OpenFileDescriptor(
                            project,
                            lastSelectedBeanFile.getVirtualFile(),
                            -1
                    ).navigate(true);  // true表示请求焦点
                } else {
                    JOptionPane.showMessageDialog(
                            getRootPane(),  // 使用getRootPane()作为父组件
                            "请先通过搜索按钮选择一个Bean文件",
                            "提示",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });
        settingsPanel.add(openBeanFileButton, gbc);
        //=================== Bean引用 ===================
        //=================== 方法 ======================
        // 方法名
        gbc.gridx = 0;
        gbc.gridy++;
        settingsPanel.add(new JLabel("方法名:"), gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        methodNameField = new JTextField(20);
        //不可编辑控制
        if(!item.isBeanEdit()){
            methodNameField.setEditable(false);  // 设置为不可编辑
            methodNameField.setBackground(UIManager.getColor("TextField.background")); // 保持背景色
        }
        settingsPanel.add(methodNameField, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        JButton searchMethodButton = new JButton("搜索");
        //不可编辑控制
        if(!PluginCache.componentItemMap.get(type).isBeanEdit()){
            searchMethodButton.setEnabled(false);
            searchMethodButton.setBackground(new Color(240, 240, 240));
        }
        searchMethodButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComponentUtils.searchMethod(e, CommonDialog.this, project);
            }
        });
        settingsPanel.add(searchMethodButton, gbc);

        // 修改打开方法所在文件的按钮事件
        gbc.gridx++;
        JButton openMethodFileButton = new JButton("→");
        openMethodFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (lastSelectedMethodFile != null) {
                    // 在新窗口中打开文件并定位到方法
                    int offset = lastSelectedMethodElement.getTextOffset();
                    new OpenFileDescriptor(
                            project,
                            lastSelectedMethodFile.getVirtualFile(),
                            offset
                    ).navigate(true);  // true表示请求焦点
                } else {
                    JOptionPane.showMessageDialog(
                            getRootPane(),  // 使用getRootPane()作为父组件
                            "请先通过搜索按钮选择一个方法",
                            "提示",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });
        settingsPanel.add(openMethodFileButton, gbc);
        //=================== 方法 ======================

        //=================== 线程调用类型 ======================
        if(withThread){
            // 线程调用类型
            gbc.gridx = 0;
            gbc.gridy++;
            settingsPanel.add(new JLabel("线程调用类型:"), gbc);

            gbc.gridx++;
            threadTypeComboBox = new JComboBox<>(ThreadType.values());
            threadTypeComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof ThreadType) {
                        setText(((ThreadType) value).getDisplayName());
                    }
                    return this;
                }
            });
            settingsPanel.add(threadTypeComboBox, gbc);

            // 添加帮助图标到下拉框后面
            gbc.gridx++;
            gbc.insets = new Insets(5, 2, 5, 5); // 调整左边距使图标更靠近下拉框
            JLabel helpIcon = new JLabel(UIManager.getIcon("OptionPane.questionIcon"));
            helpIcon.setToolTipText("<html><b>线程调用类型说明：</b><br>" +
                    "- 同步：等待运行，阻塞当前线程<br>" +
                    "- 异步：用于异步发送，不阻塞当前线程, 返回值不生效<br>" +
                    "选择合适的线程类型可以优化程序性能和响应性</html>");
            settingsPanel.add(helpIcon, gbc);
        }
        return settingsPanel;
    }

    public JPanel createCustomBeanPanel() {
        ComponentItem item = PluginCache.componentItemMap.get(type);
        item.setBeanEdit(true); //自定义全放开
        // 创建设置面板
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        //=================== Bean引用 ===================
        settingsPanel.add(new JLabel("Bean:"), gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        beanReferenceField = new JTextField(20);
        //不可编辑控制
        beanReferenceField.setEditable(false);  // 设置为不可编辑
        beanReferenceField.setBackground(UIManager.getColor("TextField.background")); // 保持背景色
        settingsPanel.add(beanReferenceField, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        JButton searchBeanButton = new JButton("搜索");
        //不可编辑控制
        searchBeanButton.setEnabled(false);
        searchBeanButton.setBackground(new Color(240, 240, 240));
        searchBeanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComponentUtils.searchBean(e, CommonDialog.this, project);
            }
        });
        settingsPanel.add(searchBeanButton, gbc);

        // 修改打开Bean文件的按钮事件
        gbc.gridx++;
        JButton openBeanFileButton = new JButton("→");
        openBeanFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (lastSelectedBeanFile != null) {
                    // 在新窗口中打开文件
                    new OpenFileDescriptor(
                            project,
                            lastSelectedBeanFile.getVirtualFile(),
                            -1
                    ).navigate(true);  // true表示请求焦点
                } else {
                    JOptionPane.showMessageDialog(
                            getRootPane(),  // 使用getRootPane()作为父组件
                            "请先通过搜索按钮选择一个Bean文件",
                            "提示",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });
        settingsPanel.add(openBeanFileButton, gbc);
        //=================== Bean引用 ===================
        //=================== 方法 ======================
        // 方法名
        gbc.gridx = 0;
        gbc.gridy++;
        settingsPanel.add(new JLabel("方法名:"), gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        methodNameField = new JTextField(20);
        //不可编辑控制
        methodNameField.setEditable(false);  // 设置为不可编辑
        methodNameField.setBackground(UIManager.getColor("TextField.background")); // 保持背景色
        settingsPanel.add(methodNameField, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        JButton searchMethodButton = new JButton("搜索");
        //不可编辑控制
        searchMethodButton.setEnabled(false);
        searchMethodButton.setBackground(new Color(240, 240, 240));
        searchMethodButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComponentUtils.searchMethod(e, CommonDialog.this, project);
            }
        });
        settingsPanel.add(searchMethodButton, gbc);

        // 修改打开方法所在文件的按钮事件
        gbc.gridx++;
        JButton openMethodFileButton = new JButton("→");
        openMethodFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (lastSelectedMethodFile != null) {
                    // 在新窗口中打开文件并定位到方法
                    int offset = lastSelectedMethodElement.getTextOffset();
                    new OpenFileDescriptor(
                            project,
                            lastSelectedMethodFile.getVirtualFile(),
                            offset
                    ).navigate(true);  // true表示请求焦点
                } else {
                    JOptionPane.showMessageDialog(
                            getRootPane(),  // 使用getRootPane()作为父组件
                            "请先通过搜索按钮选择一个方法",
                            "提示",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });
        settingsPanel.add(openMethodFileButton, gbc);
        //=================== 方法 ======================

        //=================== 线程调用类型 ======================
        // 线程调用类型
        gbc.gridx = 0;
        gbc.gridy++;
        settingsPanel.add(new JLabel("线程调用类型:"), gbc);

        gbc.gridx++;
        threadTypeComboBox = new JComboBox<>(ThreadType.values());
        threadTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ThreadType) {
                    setText(((ThreadType) value).getDisplayName());
                }
                return this;
            }
        });
        threadTypeComboBox.setEnabled(false);
        settingsPanel.add(threadTypeComboBox, gbc);

        // 添加帮助图标到下拉框后面
        gbc.gridx++;
        gbc.insets = new Insets(5, 2, 5, 5); // 调整左边距使图标更靠近下拉框
        JLabel helpIcon = new JLabel(UIManager.getIcon("OptionPane.questionIcon"));
        helpIcon.setToolTipText("<html><b>线程调用类型说明：</b><br>" +
                "- 同步：等待运行，阻塞当前线程<br>" +
                "- 异步：用于异步发送，不阻塞当前线程, 返回值不生效<br>" +
                "选择合适的线程类型可以优化程序性能和响应性</html>");
        settingsPanel.add(helpIcon, gbc);
        return settingsPanel;
    }


    public DefaultTableModel getParamsTableModel() {
        return paramsTableModel;
    }

    public void setParamsTableModel(DefaultTableModel paramsTableModel) {
        this.paramsTableModel = paramsTableModel;
    }

    public JTextField getBeanReferenceField() {
        return beanReferenceField;
    }

    public void setBeanReferenceField(JTextField beanReferenceField) {
        this.beanReferenceField = beanReferenceField;
    }

    public JTextField getMethodNameField() {
        return methodNameField;
    }

    public void setMethodNameField(JTextField methodNameField) {
        this.methodNameField = methodNameField;
    }

    public JComboBox<Enum> getThreadTypeComboBox() {
        return threadTypeComboBox;
    }

    public void setThreadTypeComboBox(JComboBox<Enum> threadTypeComboBox) {
        this.threadTypeComboBox = threadTypeComboBox;
    }

    public JBTable getParamsTable() {
        return paramsTable;
    }

    public void setParamsTable(JBTable paramsTable) {
        this.paramsTable = paramsTable;
    }

    public PsiFile getLastSelectedBeanFile() {
        return lastSelectedBeanFile;
    }

    public void setLastSelectedBeanFile(PsiFile lastSelectedBeanFile) {
        this.lastSelectedBeanFile = lastSelectedBeanFile;
    }

    public JPanel getCanvasPanel() {
        return canvasPanel;
    }

    public void setCanvasPanel(JPanel canvasPanel) {
        this.canvasPanel = canvasPanel;
    }

    public PsiFile getLastSelectedMethodFile() {
        return lastSelectedMethodFile;
    }

    public void setLastSelectedMethodFile(PsiFile lastSelectedMethodFile) {
        this.lastSelectedMethodFile = lastSelectedMethodFile;
    }

    public PsiElement getLastSelectedMethodElement() {
        return lastSelectedMethodElement;
    }

    public void setLastSelectedMethodElement(PsiElement lastSelectedMethodElement) {
        this.lastSelectedMethodElement = lastSelectedMethodElement;
    }

    public String getCode(){
        return "";
    }

}
