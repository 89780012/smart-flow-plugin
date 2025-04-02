package com.smart;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import java.util.*;

import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import com.smart.bean.ComponentItem;
import com.smart.cache.PluginCache;
import com.smart.tasks.AsyncTaskManager;
import com.smart.ui.*;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import com.smart.utils.SourseCodeUtils;
import com.smart.utils.ToggleButtonUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.event.DocumentEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.util.List;

import com.intellij.icons.AllIcons;
import com.smart.archive.ArchiveManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.application.ApplicationManager;
import com.smart.archive.ArchiveDialog;

public class BizFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile file;
    private final JPanel mainPanel;
    private SidebarPanel sidebarPanel;
    private VisualLayoutPanel visualLayoutPanel;
    //局部属性
    private Map<String, Object> propertyMap = null;
    private JPanel leftPanel;
    private JScrollPane functionPanel;
    private JSplitPane leftSplitPane;
    private JToggleButton componentTab;
    private JToggleButton functionTab;
    private ResizablePanel leftComponentTree; // 左侧组件树
    private JPanel componentTreePanel; //组件树

    private JPanel leftContainer; //左侧容器, 包含左侧tab标签 以及扩展面板
    private JPanel centerContainer;  //中间容器，包含画布
    private JPanel rightContainer; //右侧区域，包含垂直tab 以及扩展面板

    private Tree componentTree; // 组件树实例

    // 添加成员变量
    private JToggleButton demoTab;
    private DemoTreePanel demoPanel;
    private ArchiveManager archiveManager;
    private JToggleButton archiveTab;

    private JToggleButton aiTab;
    private AIPanel aiPanel;

    // 在BizFileEditor类中添加一个成员变量来存储原始宽度
    private int originalComponentTreeWidth; // 移除默认值

    /**
     * 构造函数
     *
     * @param project 当前项目
     * @param file    当前文件
     */
    public BizFileEditor(Project project, VirtualFile file) {
        this.project = project;
        this.file = file;
        this.mainPanel = new JPanel(new BorderLayout());
        PluginCache.project = project;
        propertyMap = new HashMap<>();  //独属于当个file的变量
        initComponentConfig(); // 加载组件配置


        mainPanel.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                mainPanel.requestFocusInWindow();
            }
        });

        //===============左侧容器面板===============
        leftContainer = new JPanel(new BorderLayout());
        //左侧tab页
        JPanel leftTabPanel = createLeftTabPanel();
        leftContainer.add(leftTabPanel, BorderLayout.WEST);
        // 左侧组件树
        leftComponentTree = createLeftComponentTree();
        leftContainer.add(leftComponentTree, BorderLayout.CENTER);
        mainPanel.add(leftContainer, BorderLayout.WEST);
        //===============左侧容器面板===============

        //===============画布区域==================
        //中间区域
        centerContainer = new JPanel(new BorderLayout());
        // 中间画布
        VisualLayoutPanel vPanel = new VisualLayoutPanel(file, project);
        initBizConfig(vPanel); //画布初始化后 加载biz配置
        JPanel viewPortPanel = vPanel.getLayeredPane();
        centerContainer.add(viewPortPanel, BorderLayout.CENTER);
        mainPanel.add(centerContainer, BorderLayout.CENTER);
        //===============画布区域==================

        //===============右侧容器面板==================
        // 右侧垂直侧边栏
        rightContainer = new JPanel(new BorderLayout());
        sidebarPanel = new SidebarPanel(propertyMap,file);
        rightContainer.add(sidebarPanel.getMainContainer());
        mainPanel.add(rightContainer,BorderLayout.EAST);
        //===============右侧容器面板==================

        // 为mainPanel添加下边框，仿照IDEA风格
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new JBColor(Gray._200, Gray._80)),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        // 保存 VisualLayoutPanel 实例
        this.visualLayoutPanel = vPanel;
        AsyncTaskManager.getInstance().startSpringBeanLoadTask(project);

        // 初始化存档管理器
        archiveManager = new ArchiveManager(project, file ,this);
        PluginCache.archiveManager = archiveManager;

    }

    //加载biz配置到
    private SourseCodeUtils initBizConfig(VisualLayoutPanel visualLayoutPanel){
        // 从源码中加载组件
        SourseCodeUtils sourseCodeUtils = new SourseCodeUtils(file, propertyMap);
        sourseCodeUtils.initVPanel(visualLayoutPanel);
        sourseCodeUtils.loadComponentsFromSource();
        PluginCache.sourceCodeUtilMap.put(file.getPath(),sourseCodeUtils);
        return sourseCodeUtils;
    }

    private JPanel createLeftTabPanel() {
        // 创建垂直标签栏面板
        JPanel tabPanel = new JPanel();
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
        tabPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, JBColor.border()));
        tabPanel.setPreferredSize(new Dimension(40, -1));

        // 创建标签按钮
        componentTab = createTabButton("组件", AllIcons.Nodes.Module, true);
        functionTab = createTabButton("功能", AllIcons.Actions.Execute, false);
        demoTab = createTabButton("Demo", AllIcons.Actions.Preview, false);
        archiveTab = createTabButton("存档", AllIcons.Vcs.History, false);
        aiTab = createTabButton("AI", AllIcons.Nodes.Artifact, false);

        tabPanel.add(componentTab);
        tabPanel.add(Box.createVerticalGlue()); // 弹性布局
        tabPanel.add(aiTab);
        tabPanel.add(archiveTab); // 档标签放在Demo标签上方
        tabPanel.add(demoTab); // Demo标签放在功能签上方
        //tabPanel.add(functionTab); // 功能标签在最底部
        PluginCache.archiveTab = archiveTab;
        return tabPanel;
    }

    private JToggleButton createTabButton(String tooltip, Icon icon, boolean selected) {

        JToggleButton button = ToggleButtonUtils.createToggleButton(tooltip,icon,selected);
        // 添加击事件
        button.addActionListener(e -> {
            boolean isSelected = button.isSelected();
            if (tooltip.equals("AI")) {
                if (isSelected) {
                    componentTab.setSelected(false);
                    if (aiPanel == null) {
                        aiPanel = new AIPanel(project, visualLayoutPanel,this.file);
                        aiPanel.welcome();
                    }
                    // 在改变宽度前记录当前宽度
                    originalComponentTreeWidth = leftComponentTree.getPreferredSize().width;
                    //覆盖整个左侧展开面板
                    leftSplitPane.setTopComponent(aiPanel);
                    leftSplitPane.setDividerLocation(0.5);
                    // 设置左侧面板宽度为500
                    leftComponentTree.setPreferredSize(new Dimension(500, -1));
                    leftComponentTree.revalidate();
                    leftComponentTree.repaint();
                } else {
                    if (leftSplitPane.getTopComponent() == aiPanel) {
                        leftSplitPane.setTopComponent(null);
                    }
                    // 恢复到记录的原始宽度
                    if(originalComponentTreeWidth > 0){
                        leftComponentTree.setPreferredSize(new Dimension(originalComponentTreeWidth, -1));
                    }else{
                        leftComponentTree.setPreferredSize(new Dimension(500, -1));
                    }
                    leftComponentTree.revalidate();
                    leftComponentTree.repaint();
                }
            } else if (tooltip.equals("存档")) {
                showArchiveDialog();
            } else if (tooltip.equals("组件")) {
                if (isSelected) {
                    aiTab.setSelected(false);
                    leftSplitPane.setTopComponent(componentTreePanel);
                    leftSplitPane.setDividerLocation(0.5);
                    // 恢复到记录的原始宽度
                    if(originalComponentTreeWidth > 0){
                        leftComponentTree.setPreferredSize(new Dimension(originalComponentTreeWidth, -1));
                    }else{
                        leftComponentTree.setPreferredSize(new Dimension(220, -1));
                    }
                    leftComponentTree.revalidate();
                    leftComponentTree.repaint();
                } else {
                    if (leftSplitPane.getTopComponent() == componentTreePanel) {
                        leftSplitPane.setTopComponent(null);
                    }
                }
            } else if (tooltip.equals("功能")) {
                if (isSelected) {
                    demoTab.setSelected(false);
                    if (leftSplitPane.getBottomComponent() == demoPanel) {
                        leftSplitPane.setBottomComponent(null);
                    }
                    leftSplitPane.setBottomComponent(functionPanel);
                } else {
                    if (leftSplitPane.getBottomComponent() == functionPanel) {
                        leftSplitPane.setBottomComponent(null);
                    }
                }
            } else if (tooltip.equals("Demo")) {
                if (isSelected) {
                    functionTab.setSelected(false);
                    if (leftSplitPane.getBottomComponent() == functionPanel) {
                        leftSplitPane.setBottomComponent(null);
                    }
                    if (demoPanel == null) {
                        demoPanel = createDemoPanel();
                    }
                    leftSplitPane.setBottomComponent(demoPanel);
                } else {
                    if (leftSplitPane.getBottomComponent() == demoPanel) {
                        leftSplitPane.setBottomComponent(null);
                    }
                }
            }

            // 更新按钮外观
            updateButtonAppearance(button, isSelected);

            // 更新分割面板
            updateSplitPane();
        });

        return button;
    }

    // 添加辅助方法来更新按钮外观
    private void updateButtonAppearance(JToggleButton button, boolean isSelected) {
        if (isSelected) {
            button.setBackground(new JBColor(new Color(0, 0, 0, 20), new Color(255, 255, 255, 20)));
            button.setOpaque(true);
        } else {
            button.setBackground(null);
            button.setOpaque(false);
        }
    }

    // 添加辅助方法来更新分割面
    private void updateSplitPane() {
        // 根据组件面板是否显示调整分割比例
        if (componentTab.isSelected() || aiTab.isSelected()) {
            leftSplitPane.setDividerLocation(0.5);
        } else {
            leftSplitPane.setDividerLocation(0.0);
        }

        // 更新左侧面板可见性
        leftComponentTree.setVisible(componentTab.isSelected() ||
                functionTab.isSelected() ||
                demoTab.isSelected() ||
                aiTab.isSelected()
                );

        leftSplitPane.revalidate();
        leftSplitPane.repaint();
    }

    /**
     * 左侧组件面板
     *
     * @return
     */
    public ResizablePanel createLeftComponentTree() {
        // 创建左侧面板
        leftPanel = new JPanel(new BorderLayout());

        // 创建组件树
        componentTreePanel = createComponentTreePanel();

        // 初始化分割面板
        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplitPane.setTopComponent(componentTreePanel);

        // 默认不显示功能面板
        leftSplitPane.setDividerSize(1);
        leftSplitPane.setBorder(null);

        // 将分割面板添加到左侧主面板
        leftPanel.add(leftSplitPane, BorderLayout.CENTER);

        // 创建一个可调整大小的面板
        ResizablePanel resizablePanel = new ResizablePanel(leftPanel, ResizablePanel.RIGHT);
        resizablePanel.setPreferredSize(new Dimension(220, -1));

        // 添加右侧边框
        resizablePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new JBColor(Gray._200, Gray._80)),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        leftPanel.setBorder(null);
        return resizablePanel;
    }

    /**
     * 加载置文件
     */
    private void initComponentConfig() {
        // 加载内置件配置
        loadBuiltInComponents();
        // 加载项目自定义组件置
        loadCustomComponents();
    }

    /**
     * 加载内置组件配置
     */
    private void loadBuiltInComponents() {
        try (InputStream inputStream = BizFileEditor.class.getResourceAsStream("/components.xml")) {
            loadComponentsFromStream(inputStream, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载项目自定义组件配置
     */
    private void loadCustomComponents() {
        // 获取项目目录
        String basePath = project.getBasePath();
        VirtualFile projectDir = basePath != null ? LocalFileSystem.getInstance().findFileByPath(basePath) : null;
        
        // 查找有模块中的 resources 目录
        List<VirtualFile> resourcesDirs = findAllResourcesDirectories(projectDir);

        for (VirtualFile resourcesDir : resourcesDirs) {
            // 在每个 resources 目录中查找 components.xml
            VirtualFile customConfigFile = resourcesDir.findChild("components.xml");
            if (customConfigFile != null && customConfigFile.exists()) {
                try (InputStream inputStream = customConfigFile.getInputStream()) {
                    loadComponentsFromStream(inputStream, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 递归查找所有 resources 目录
     *
     * @param dir 起始目录
     * @return resources 目录列表
     */
    private List<VirtualFile> findAllResourcesDirectories(VirtualFile dir) {
        List<VirtualFile> resourcesDirs = new ArrayList<>();
        findResourcesDirectoriesRecursively(dir, resourcesDirs);
        return resourcesDirs;
    }

    /**
     * 递归查找 resources 目录的辅助方法
     *
     * @param dir           当前目录
     * @param resourcesDirs 结果列表
     */
    private void findResourcesDirectoriesRecursively(VirtualFile dir, List<VirtualFile> resourcesDirs) {
        if (dir == null || !dir.isDirectory())
            return;

        // 检查前目录是否为 resources 目录
        if (isResourcesDirectory(dir)) {
            resourcesDirs.add(dir);
        }

        // 递检查子目录
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory() && !shouldSkipDirectory(child)) {
                findResourcesDirectoriesRecursively(child, resourcesDirs);
            }
        }
    }

    /**
     * 判断给定目录是否为 resources 目录
     *
     * @param dir 要检查的目录
     * @return 是否为 resources 目录
     */
    private boolean isResourcesDirectory(VirtualFile dir) {
        String path = dir.getPath().toLowerCase();
        // 添加更多能的 resources 目录路径式
        return path.endsWith("/resources") ||
                path.endsWith("/main/resources") ||
                path.endsWith("/src/main/resources") ||
                path.endsWith("/test/resources") ||
                path.endsWith("/src/test/resources") ||
                // 处理 Windows 路径分隔符
                path.endsWith("\\resources") ||
                path.endsWith("\\main\\resources") ||
                path.endsWith("\\src\\main\\resources") ||
                path.endsWith("\\test\\resources") ||
                path.endsWith("\\src\\test\\resources");
    }

    /**
     * 判断是否应该跳过某些目录的搜索
     *
     * @param dir 要检的目录
     * @return 是否该跳过
     */
    private boolean shouldSkipDirectory(VirtualFile dir) {
        String name = dir.getName().toLowerCase();
        // 扩展需要跳过的目录列表
        return name.equals(".git") ||
                name.equals(".idea") ||
                name.equals("target") ||
                name.equals("build") ||
                name.equals("node_modules") ||
                name.equals("out") ||
                name.equals("dist") ||
                name.equals(".gradle");
    }

    /**
     * 从输入加载组件配置
     *
     * @param inputStream 配置件输入流
     * @param isCustom    是否为自定义组件
     */
    private void loadComponentsFromStream(InputStream inputStream, boolean isCustom) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();

        NodeList typeList = doc.getElementsByTagName("component-type");
        for (int i = 0; i < typeList.getLength(); i++) {
            Element typeElement = (Element) typeList.item(i);
            NodeList itemList = typeElement.getElementsByTagName("item");
            for (int j = 0; j < itemList.getLength(); j++) {
                Element itemElement = (Element) itemList.item(j);
                String name = itemElement.getElementsByTagName("name").item(0).getTextContent();
                String icon = "/icons/biz.svg"; // 默认图标
                String type = itemElement.getElementsByTagName("type").item(0).getTextContent();

                // 获取图标节点
                NodeList iconNodes = itemElement.getElementsByTagName("icon");
                if (iconNodes.getLength() > 0) {
                    String customIcon = iconNodes.item(0).getTextContent();
                    if (customIcon != null && !customIcon.trim().isEmpty()) {
                        // 1. 首先尝试从插件内部resources加载
                        if (BizFileEditor.class.getResource(customIcon) != null) {
                            icon = customIcon;
                        } 
                        // 2. 如果不是插件内部图标且是自定义组件,尝试从项目resources加载
                        else if (isCustom) {
                            String basePath = project.getBasePath();
                            if (basePath != null) {
                                VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
                                if (baseDir != null) {
                                    // 查找所有resources目录
                                    List<VirtualFile> resourcesDirs = findAllResourcesDirectories(baseDir);
                                    for (VirtualFile resourcesDir : resourcesDirs) {
                                        // 移除路径开头的/,避免路径拼接问题
                                        String iconPath = customIcon.startsWith("/") ? customIcon.substring(1) : customIcon;
                                        VirtualFile iconFile = resourcesDir.findFileByRelativePath(iconPath);
                                        if (iconFile != null && iconFile.exists()) {
                                            icon = iconFile.getPath();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        // 3. 如果是插件内置组件且图标以/icons/开头,使用该图标
                        else if (!isCustom && customIcon.startsWith("/icons/")) {
                            icon = customIcon;
                        }
                    }
                }

                boolean isBeanEdit = true;
                // 如果是自定义组件，修改type
                if (isCustom) {
                    type = "custom-" + type;
                    isBeanEdit = false; //显示出来，但是不允许编辑
                }else{
                    NodeList nodeList = itemElement.getElementsByTagName("isBeanEdit");
                    if (nodeList.getLength() > 0) {
                        String isBeanEditStr = nodeList.item(0).getTextContent().toLowerCase();
                        if (isBeanEditStr.equals("false")) {
                            isBeanEdit = false;
                        }
                    }
                }

                String beanRef = getElementContent(itemElement, "beanRef");
                String method = getElementContent(itemElement, "method");
                String threadType = getElementContent(itemElement, "threadType");
                String description = getElementContent(itemElement, "description");

                // 解析config配置
                ComponentItem.ComponentConfig config = null;
                NodeList configNodes = itemElement.getElementsByTagName("config");
                if (configNodes.getLength() > 0) {
                    Element configElement = (Element) configNodes.item(0);
                    config = parseComponentConfig(configElement);
                }

                ComponentItem componentItem = new ComponentItem(name, icon, type, isBeanEdit, 
                                                             beanRef, method, description, config,threadType);
                PluginCache.componentItemMap.put(type, componentItem);
                PluginCache.componentIconMap.put(name, icon);
                PluginCache.type2icon.put(type, icon);
            }
        }
    }

    // 辅助方法:获取元素内容
    private String getElementContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? nodeList.item(0).getTextContent() : "";
    }

    // 解析组件配置
    private ComponentItem.ComponentConfig parseComponentConfig(Element configElement) {
        ComponentItem.ComponentConfig config = new ComponentItem.ComponentConfig();
        
        // 解析header
        Element headerElement = (Element) configElement.getElementsByTagName("header").item(0);
        if (headerElement != null) {
            NodeList columns = headerElement.getElementsByTagName("column");
            for (int i = 0; i < columns.getLength(); i++) {
                Element column = (Element) columns.item(i);
                String name = column.getTextContent();
                boolean isEdit = Boolean.parseBoolean(column.getAttribute("isEdit"));
                int width = 0;
                try {
                    width = Integer.parseInt(column.getAttribute("width"));
                } catch (NumberFormatException e) {
                    // 如果width属性不存在或无效,使用默认值0
                }
                config.getHeader().add(new ComponentItem.ColumnConfig(name, isEdit, width));
            }
        }
        
        // 解析data
        Element dataElement = (Element) configElement.getElementsByTagName("data").item(0);
        if (dataElement != null) {
            NodeList rows = dataElement.getElementsByTagName("row");
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                ComponentItem.RowConfig rowConfig = new ComponentItem.RowConfig();
                NodeList columns = row.getElementsByTagName("column");
                for (int j = 0; j < columns.getLength(); j++) {
                    Element column = (Element) columns.item(j);
                    rowConfig.getColumns().add(column.getTextContent());
                }
                config.getData().add(rowConfig);
            }
        }
        
        return config;
    }

    /**
     * 加载树节点组件
     *
     * @param parentNode 父节点
     * @param isCustom   是否为自定义组件
     */
    private void loadTreeComponents(DefaultMutableTreeNode parentNode, boolean isCustom) throws Exception {
        if (isCustom) {
            String basePath = project.getBasePath();
            VirtualFile baseDir = basePath != null ? LocalFileSystem.getInstance().findFileByPath(basePath) : null;
            // 获取所有模块的 components.xml
            List<VirtualFile> resourcesDirs = findAllResourcesDirectories(baseDir);
            for (VirtualFile resourcesDir : resourcesDirs) {
                VirtualFile customConfigFile = resourcesDir.findChild("components.xml");
                if (customConfigFile != null && customConfigFile.exists()) {
                    try (InputStream inputStream = customConfigFile.getInputStream()) {
                        loadTreeComponentsFromStream(inputStream, parentNode, true);
                    }
                }
            }
        } else {
            // 加内置组件
            try (InputStream inputStream = BizFileEditor.class.getResourceAsStream("/components.xml")) {
                loadTreeComponentsFromStream(inputStream, parentNode, false);
            }
        }
    }

    /**
     * 从输入流加载树节点组件
     */
    private void loadTreeComponentsFromStream(InputStream inputStream, DefaultMutableTreeNode parentNode,
                                              boolean isCustom) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();

        NodeList typeList = doc.getElementsByTagName("component-type");
        for (int i = 0; i < typeList.getLength(); i++) {
            Element typeElement = (Element) typeList.item(i);
            String typeName = typeElement.getAttribute("display-name");
            DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(typeName);
            parentNode.add(typeNode);

            NodeList itemList = typeElement.getElementsByTagName("item");
            for (int j = 0; j < itemList.getLength(); j++) {
                Element itemElement = (Element) itemList.item(j);
                String name = itemElement.getElementsByTagName("name").item(0).getTextContent();
                String iconPath = "/icons/biz.svg"; // 默认图标
                String type = itemElement.getElementsByTagName("type").item(0).getTextContent();

                // 获取图标节点
                NodeList iconNodes = itemElement.getElementsByTagName("icon");
                if (iconNodes.getLength() > 0) {
                    String customIcon = iconNodes.item(0).getTextContent();
                    if (customIcon != null && !customIcon.trim().isEmpty()) {
                        // 1. 首先尝试从插件内部resources加载
                        if (BizFileEditor.class.getResource(customIcon) != null) {
                            iconPath = customIcon;
                        } 
                        // 2. 如果不是插件内部图标且是自定义组件,尝试从项目resources加载
                        else if (isCustom) {
                            String basePath = project.getBasePath();
                            if (basePath != null) {
                                VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
                                if (baseDir != null) {
                                    // 查找所有resources目录
                                    List<VirtualFile> resourcesDirs = findAllResourcesDirectories(baseDir);
                                    for (VirtualFile resourcesDir : resourcesDirs) {
                                        // 移除路径开头的/,避免路径拼接问题
                                        iconPath = customIcon.startsWith("/") ? customIcon.substring(1) : customIcon;
                                        VirtualFile iconFile = resourcesDir.findFileByRelativePath(iconPath);
                                        if (iconFile != null && iconFile.exists()) {
                                            iconPath = iconFile.getPath();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        // 3. 如果是插件内置组件且图标以/icons/开头,使用该图标
                        else if (!isCustom && customIcon.startsWith("/icons/")) {
                            iconPath = customIcon;
                        }
                    }
                }

                if (isCustom) {
                    type = "custom-" + type;
                }

                // 只是左侧组件展示用, 无实际用处
                ComponentItem componentItem = new ComponentItem(name, iconPath, type, false, "", "");
                typeNode.add(new DefaultMutableTreeNode(componentItem));
            }
        }
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mainPanel;
    }

    @Override
    public String getName() {
        return "可视化";
    }

    @Override
    public void setState( FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener( PropertyChangeListener listener) {
    }

    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {
        // 解绑UI事件
        if (visualLayoutPanel != null && file != null) {
            PluginCache.sourceCodeUtilMap.get(file.getPath()).unregisterUIEvent(file.getPath());
        }
        if (visualLayoutPanel != null) {
            visualLayoutPanel.dispose();
            visualLayoutPanel = null;
        }

        // 清理其他资源
        if (sidebarPanel != null) {
            sidebarPanel = null;
        }

        if (mainPanel != null) {
            mainPanel.removeAll();
        }

        if (aiPanel != null) {
            aiPanel = null;
        }
    }

    @Override
    public VirtualFile getFile() {
        return file;
    }

    private JPanel createComponentTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 建顶部工具栏面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(JBUI.Borders.empty(0, 0, 1, 0)); // 只添加底部边框

        // 创建工具栏按钮组
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        // 添加刷新按钮
        actionGroup.add(new AnAction("Refresh", "Refresh component list", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                refreshComponentTree();
            }
        });

        // 添加展开全部按钮
        actionGroup.add(new AnAction("Expand All", "Expand all nodes", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                TreeUtil.expandAll(componentTree);
            }
        });

        // 添加折叠全部按钮
        actionGroup.add(new AnAction("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                TreeUtil.collapseAll(componentTree, 0);
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "ComponentTreeToolbar",
                actionGroup,
                true
        );
        toolbar.setTargetComponent(panel);

        // 创建搜索框
        SearchTextField searchField = new SearchTextField();
        searchField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                filterComponents(searchField.getText());
            }
        });

        topPanel.add(toolbar.getComponent(), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);

        // 创建组件
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("组件");
        try {
            loadTreeComponents(root, false);
            DefaultMutableTreeNode customNode = new DefaultMutableTreeNode("扩展组件");
            root.add(customNode);
            loadTreeComponents(customNode, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        componentTree = new Tree(treeModel);
        componentTree.setRootVisible(false);
        componentTree.setCellRenderer(new ComponentTreeCellRenderer());
        componentTree.setDragEnabled(true);

        // 设置 TransferHandler
        componentTree.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) componentTree.getLastSelectedPathComponent();
                if (node != null && node.getUserObject() instanceof ComponentItem) {
                    final ComponentItem item = (ComponentItem) node.getUserObject();
                    return new Transferable() {
                        @Override
                        public DataFlavor[] getTransferDataFlavors() {
                            return new DataFlavor[] { new DataFlavor(ComponentItem.class, "ComponentItem") };
                        }

                        @Override
                        public boolean isDataFlavorSupported(DataFlavor flavor) {
                            return flavor.getRepresentationClass() == ComponentItem.class;
                        }

                        @Override
                        public Object getTransferData(DataFlavor flavor)
                                throws UnsupportedFlavorException, IOException {
                            if (isDataFlavorSupported(flavor)) {
                                return item;
                            }
                            throw new UnsupportedFlavorException(flavor);
                        }
                    };
                }
                return null;
            }
        });
        componentTree.setBorder(JBUI.Borders.empty());
        JBScrollPane scrollPane = new JBScrollPane(componentTree);
        scrollPane.setBorder(JBUI.Borders.empty());
        // 将顶部工具栏和组件树添加到面板
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // 加刷新组件树的方法
    private void refreshComponentTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) componentTree.getModel().getRoot();
        root.removeAllChildren();
        try {
            loadTreeComponents(root, false);
            DefaultMutableTreeNode customNode = new DefaultMutableTreeNode("扩展组件");
            root.add(customNode);
            loadTreeComponents(customNode, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ((DefaultTreeModel) componentTree.getModel()).reload();
        TreeUtil.expandAll(componentTree);
    }

    // 添加组件过滤方法
    private void filterComponents(String searchText) {
        if (searchText.isEmpty()) {
            refreshComponentTree();
            return;
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) componentTree.getModel().getRoot();
        root.removeAllChildren();

        searchText = searchText.toLowerCase();
        try {
            // 过滤并加载内置组件
            DefaultMutableTreeNode filteredNode = new DefaultMutableTreeNode("组件");
            loadFilteredComponents(filteredNode, false, searchText);
            if (filteredNode.getChildCount() > 0) {
                root.add(filteredNode);
            }

            // 过滤并加载自定义组件
            DefaultMutableTreeNode filteredCustomNode = new DefaultMutableTreeNode("扩展组");
            loadFilteredComponents(filteredCustomNode, true, searchText);
            if (filteredCustomNode.getChildCount() > 0) {
                root.add(filteredCustomNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ((DefaultTreeModel) componentTree.getModel()).reload();
        TreeUtil.expandAll(componentTree);
    }

    // 添加过滤加载组件的方法
    private void loadFilteredComponents(DefaultMutableTreeNode parentNode, boolean isCustom, String searchText) throws Exception {
        if (isCustom) {
            String basePath = project.getBasePath();
            VirtualFile baseDir = basePath != null ? LocalFileSystem.getInstance().findFileByPath(basePath) : null;
            // 加载自定义组件配置
            List<VirtualFile> resourcesDirs = findAllResourcesDirectories(baseDir);
            for (VirtualFile resourcesDir : resourcesDirs) {
                VirtualFile customConfigFile = resourcesDir.findChild("components.xml");
                if (customConfigFile != null && customConfigFile.exists()) {
                    try (InputStream inputStream = customConfigFile.getInputStream()) {
                        loadFilteredComponentsFromStream(inputStream, parentNode, true, searchText);
                    }
                }
            }
        } else {
            // 加载内置组件配置
            try (InputStream inputStream = BizFileEditor.class.getResourceAsStream("/components.xml")) {
                loadFilteredComponentsFromStream(inputStream, parentNode, false, searchText);
            }
        }
    }

    // 添加从流中过滤加载组件的方法
    private void loadFilteredComponentsFromStream(InputStream inputStream, DefaultMutableTreeNode parentNode,
                                                  boolean isCustom, String searchText) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();

        NodeList typeList = doc.getElementsByTagName("component-type");
        for (int i = 0; i < typeList.getLength(); i++) {
            Element typeElement = (Element) typeList.item(i);
            String typeName = typeElement.getAttribute("display-name");
            DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(typeName);
            boolean hasMatchingChild = false;

            NodeList itemList = typeElement.getElementsByTagName("item");
            for (int j = 0; j < itemList.getLength(); j++) {
                Element itemElement = (Element) itemList.item(j);
                String name = itemElement.getElementsByTagName("name").item(0).getTextContent();
                String type = itemElement.getElementsByTagName("type").item(0).getTextContent();

                // 检查组件是否匹配搜索文本
                if (name.toLowerCase().contains(searchText) || type.toLowerCase().contains(searchText)) {
                    String iconPath = "/icons/biz.svg";
                    NodeList iconNodes = itemElement.getElementsByTagName("icon");
                    if (iconNodes.getLength() > 0) {
                        String customIcon = iconNodes.item(0).getTextContent();
                        if (customIcon != null && !customIcon.trim().isEmpty()) {
                            // 1. 首先尝试从插件内部resources加载
                            if (BizFileEditor.class.getResource(customIcon) != null) {
                                iconPath = customIcon;
                            } 
                            // 2. 如果不是插件内部图标且是自定义组件,尝试从项目resources加载
                            else if (isCustom) {
                                String basePath = project.getBasePath();
                                if (basePath != null) {
                                    VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
                                    if (baseDir != null) {
                                        // 查找所有resources目录
                                        List<VirtualFile> resourcesDirs = findAllResourcesDirectories(baseDir);
                                        for (VirtualFile resourcesDir : resourcesDirs) {
                                            // 移除路径开头的/,避免路径拼接问题
                                            iconPath = customIcon.startsWith("/") ? customIcon.substring(1) : customIcon;
                                            VirtualFile iconFile = resourcesDir.findFileByRelativePath(iconPath);
                                            if (iconFile != null && iconFile.exists()) {
                                                iconPath = iconFile.getPath();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            // 3. 如果是插件内置组件且图标以/icons/开头,使用该图标
                            else if (!isCustom && customIcon.startsWith("/icons/")) {
                                iconPath = customIcon;
                            }
                        }
                    }

                    if (isCustom) {
                        type = "custom-" + type;
                    }

                    ComponentItem componentItem = new ComponentItem(name, iconPath, type, false, "", "");
                    typeNode.add(new DefaultMutableTreeNode(componentItem));
                    hasMatchingChild = true;
                }
            }

            if (hasMatchingChild) {
                parentNode.add(typeNode);
            }
        }
    }

    // 添加创建demo面板的方法
    private DemoTreePanel createDemoPanel() {
        // 直接返DemoTreePanel实例,不需要额外的JBScrollPane包装
        DemoTreePanel demoTreePanel = new DemoTreePanel(project,visualLayoutPanel);
        return demoTreePanel;
    }

    private  ArchiveDialog dialog;
    private void showArchiveDialog() {
        if (dialog == null || !dialog.isValid()) {
            // 如果对话框不存在或已被销毁，创建新的
            dialog = new ArchiveDialog(project, archiveManager);
            dialog.show();
        } else {
            // 对话框存在，切换显示/隐藏状态
            if (dialog.isVisible()) {
                dialog.hide();
                PluginCache.archiveTab.setSelected(false);
            } else {
                dialog.show();  // 这里会自动刷新数据
            }
        }
    }

    /**
     * 刷新整个编辑器页面
     */
    public void refresh() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 保存当前编辑器状态
                boolean isComponentTabSelected = componentTab.isSelected();
                boolean isFunctionTabSelected = functionTab.isSelected();
                boolean isDemoTabSelected = demoTab.isSelected();
                boolean isAiTabSelected = aiTab.isSelected();
                
                // 获取当前分割面板位置
                int dividerLocation = leftSplitPane.getDividerLocation();
                
                // 使用 runWriteAction 确保在写安全的上下文中执行文件操作
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                        
                        //刷新文件
                        PluginCache.sourceCodeUtilMap.get(file.getPath()).loadComponentsFromSource();

                        // 在下一个 EDT 周期中恢复状态
                        ApplicationManager.getApplication().invokeLater(() -> {
                            FileEditor[] editors = fileEditorManager.getEditors(file);
                            for (FileEditor editor : editors) {
                                if (editor instanceof BizFileEditor) {
                                    BizFileEditor newEditor = (BizFileEditor) editor;
                                    
                                    // 恢复标签状
                                    newEditor.componentTab.setSelected(isComponentTabSelected);
                                    newEditor.functionTab.setSelected(isFunctionTabSelected);
                                    newEditor.demoTab.setSelected(isDemoTabSelected);
                                    newEditor.aiTab.setSelected(isAiTabSelected);
                                    
                                    // 恢复分割面板位置
                                    newEditor.leftSplitPane.setDividerLocation(dividerLocation);
                                    
                                    // 恢复功能面板
                                    if(isFunctionTabSelected) {
                                        newEditor.leftSplitPane.setBottomComponent(newEditor.functionPanel);
                                    }
                                    
                                    // 恢复Demo面板
                                    if(isDemoTabSelected) {
                                        if(newEditor.demoPanel == null) {
                                            newEditor.demoPanel = newEditor.createDemoPanel();
                                        }
                                        newEditor.leftSplitPane.setBottomComponent(newEditor.demoPanel);
                                    }
                                    

                                    // 更新UI
                                    newEditor.updateSplitPane();
                                    
                                    // 恢复AI面板状态
                                    if (isAiTabSelected) {
                                        aiTab.setSelected(true);
                                        if (aiPanel == null) {
                                            aiPanel = new AIPanel(project, visualLayoutPanel,this.file);
                                        }
                                        newEditor.leftSplitPane.setTopComponent(aiPanel);
                                    }
                                    
                                    break;
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                
            } catch (Exception e) {
                // 处理异常
                Messages.showErrorDialog(project, 
                    "刷新文件失败: " + e.getMessage(), 
                    "错误");
                e.printStackTrace();
            }
        });
    }

}