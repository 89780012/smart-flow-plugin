package com.smart.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.tree.TreeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import com.smart.utils.BizMarkdownGenerator;

public class BizFilesPanel extends JPanel {
    private final Project project;
    private final Tree fileTree;
    private final DefaultTreeModel treeModel;
    private SearchTextField searchField;
    private ApiDebugPanel debugPanel;
    
    public BizFilesPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());

        // 创建上下分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(JBUI.Borders.empty());
        
        // 创建上层面板
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // 创建工具栏
        JPanel toolbarPanel = createToolbarPanel();
        topPanel.add(toolbarPanel, BorderLayout.NORTH);

        // 创建文件树
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(project.getName());
        treeModel = new DefaultTreeModel(root);
        fileTree = new Tree(treeModel);
        
        // 设置树的渲染器和基本属性
        setupFileTree();
        
        // 添加文件树到滚动面板
        JScrollPane scrollPane = new JBScrollPane(fileTree);
        scrollPane.setBorder(JBUI.Borders.empty());
        topPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 创建下层调试面板
        debugPanel = new ApiDebugPanel(project);
        
        // 添加到分割面板
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(debugPanel);
        
        // 设置分割比例
        splitPane.setResizeWeight(0.6);
        
        add(splitPane, BorderLayout.CENTER);

        // 初始加载项目文件
        refreshProjectFiles();
    }
    
    private JPanel createToolbarPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(JBUI.Borders.empty(0, 0, 1, 0)); // 只添加底部边框
        
        // 创建工具栏
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        // 刷新按钮
        actionGroup.add(new AnAction("刷新", "刷新文件列表", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                refreshProjectFiles();
            }
        });
        
        // 展开全部按钮
        actionGroup.add(new AnAction("展开全部", "展开全部节点", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                TreeUtil.expandAll(fileTree);
            }
        });
        
        // 折叠全部按钮
        actionGroup.add(new AnAction("折叠全部", "折叠全部按钮", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                TreeUtil.collapseAll(fileTree, 0);
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "BizFilesToolbar",
                actionGroup,
                true
        );
        toolbar.setTargetComponent(this);
        
        // 搜索框
        searchField = new SearchTextField();
        searchField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                filterFiles(searchField.getText());
            }
        });
        
        topPanel.add(toolbar.getComponent(), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        
        return topPanel;
    }

    private void setupFileTree() {
        // 设置树的基本属性
        fileTree.setCellRenderer(new ProjectTreeCellRenderer());
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        
        // 启用多选
        fileTree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
        );
        
        // 添加双击和右键监听器
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
        });
    }

    private void handleMouseClick(MouseEvent e) {
        if (e.getClickCount() == 2) {
            TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                
                if (userObject instanceof BizFileInfo) {
                    BizFileInfo bizFileInfo = (BizFileInfo) userObject;
                    // 填充接口调试面板
                    debugPanel.fillFromBizFile(bizFileInfo);
                }
            }
        }
    }

    private void showPopupMenu(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        
        // 获取点击位置的路径
        TreePath clickPath = fileTree.getPathForLocation(x, y);
        
        // 获取所有选中的路径
        TreePath[] selectedPaths = fileTree.getSelectionPaths();
        
        // 如果点击位置不在选中项中，则更新选中项
        if (clickPath != null && (selectedPaths == null || !containsPath(selectedPaths, clickPath))) {
            fileTree.setSelectionPath(clickPath);
            selectedPaths = new TreePath[]{clickPath};
        }
        
        if (selectedPaths == null || selectedPaths.length == 0) return;
        
        JPopupMenu popupMenu = new JPopupMenu();
        
        // 如果只选中了一个项目
        if (selectedPaths.length == 1) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
            Object userObject = node.getUserObject();
            
            if (userObject instanceof VirtualFile && ((VirtualFile) userObject).isDirectory() 
                || userObject instanceof CompressedDirectory) {
                
                VirtualFile directory;
                if (userObject instanceof CompressedDirectory) {
                    CompressedDirectory compDir = (CompressedDirectory) userObject;
                    // 获取压缩目录的最后一层目录
                    directory = getDeepestDirectory(compDir.getDirectory(), compDir.getDisplayPath());
                } else {
                    directory = (VirtualFile) userObject;
                }
                    
                JMenuItem createBizFile = new JMenuItem("创建biz文件", AllIcons.FileTypes.Any_type);
                createBizFile.addActionListener(e1 -> createNewBizFile(directory));
                popupMenu.add(createBizFile);
                
                popupMenu.addSeparator();

                JMenuItem deleteItem = new JMenuItem("删除", AllIcons.Actions.DeleteTag);
                if (userObject instanceof CompressedDirectory) {
                    CompressedDirectory compDir = (CompressedDirectory) userObject;
                    deleteItem.addActionListener(e1 -> deleteCompressedDirectory(compDir));
                } else {
                    deleteItem.addActionListener(e1 -> deleteDirectory((VirtualFile) userObject));
                }
                popupMenu.add(deleteItem);
                
                popupMenu.addSeparator();
                
                // 添加生成Markdown文档菜单
                JMenu generateDocsMenu = new JMenu("生成API文档");
                generateDocsMenu.setIcon(AllIcons.FileTypes.UiForm);
                
                JMenuItem singleDoc = new JMenuItem("生成文档", AllIcons.Actions.MenuSaveall);
                singleDoc.addActionListener(e1 -> generateSingleMarkdown(directory));
                generateDocsMenu.add(singleDoc);
                
                popupMenu.add(generateDocsMenu);
            } else if (userObject instanceof BizFileInfo) {
                BizFileInfo bizFileInfo = (BizFileInfo) userObject;

                JMenuItem openItem = new JMenuItem("打开当前文件", AllIcons.Actions.Menu_open);
                openItem.addActionListener(e1 -> openBizFile(bizFileInfo));
                popupMenu.add(openItem);
                
                popupMenu.addSeparator();
                
                JMenuItem renameItem = new JMenuItem("重命名", AllIcons.Actions.Edit);
                renameItem.addActionListener(e1 -> renameBizFile(bizFileInfo));
                popupMenu.add(renameItem);
                
                popupMenu.addSeparator();
                
                JMenuItem deleteItem = new JMenuItem("删除", AllIcons.Actions.DeleteTag);
                deleteItem.addActionListener(e1 -> deleteBizFile(bizFileInfo));
                popupMenu.add(deleteItem);
                
                popupMenu.addSeparator();
                
                // 添加生成Markdown子菜单
                JMenu markdownMenu = new JMenu("生成Markdown");
                markdownMenu.setIcon(AllIcons.FileTypes.UiForm);
                
                JMenuItem copyMarkdown = new JMenuItem("复制到剪贴板", AllIcons.Actions.Copy);
                copyMarkdown.addActionListener(e1 -> copyMarkdownToClipboard(bizFileInfo));
                markdownMenu.add(copyMarkdown);
                
                JMenuItem saveMarkdown = new JMenuItem("保存为文件", AllIcons.Actions.Menu_saveall);
                saveMarkdown.addActionListener(e1 -> saveMarkdownToFile(bizFileInfo));
                markdownMenu.add(saveMarkdown);
                
                popupMenu.add(markdownMenu);
            }
        } else {
            // 多选状态下只显示删除选项
            JMenuItem deleteItem = new JMenuItem("删除选中项", AllIcons.Actions.DeleteTag);
            TreePath[] finalSelectedPaths = selectedPaths;
            deleteItem.addActionListener(e1 -> deleteSelectedItems(finalSelectedPaths));
            popupMenu.add(deleteItem);
        }
        
        if (popupMenu.getComponentCount() > 0) {
            popupMenu.show(fileTree, x, y);
        }
    }

    // 添加新的辅助方法来获取最深层目录
    private VirtualFile getDeepestDirectory(VirtualFile startDir, String displayPath) {
        String[] pathParts = displayPath.split("\\.");
        VirtualFile currentDir = startDir;
        
        // 从起始目录开始，逐层查找子目录
        for (int i = 1; i < pathParts.length; i++) {
            VirtualFile child = currentDir.findChild(pathParts[i]);
            if (child != null && child.isDirectory()) {
                currentDir = child;
            } else {
                break;
            }
        }
        
        return currentDir;
    }

    private boolean containsPath(TreePath[] paths, TreePath path) {
        for (TreePath treePath : paths) {
            if (treePath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private void createNewBizFile(VirtualFile directory) {
        String fileName = Messages.showInputDialog(
            project,
            "请输入biz文件名称:",
            "创建biz文件",
            Messages.getQuestionIcon()
        );

        if (fileName != null) {
            // 确保文件名有.biz后缀
            if (!fileName.toLowerCase().endsWith(".biz")) {
                fileName += ".biz";
            }

            try {
                String finalFileName = fileName;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        VirtualFile newFile = directory.createChildData(this, finalFileName);
                        // 可以添加默认的文件内容
                        newFile.setBinaryContent("".getBytes());
                        
                        // 刷新项目视图
                        refreshProjectFiles();
                        
                        // 打开新创建的文件
                        FileEditorManager.getInstance(project).openFile(newFile, true);
                    } catch (IOException ex) {
                        Messages.showErrorDialog(
                            project,
                            "Error creating file: " + ex.getMessage(),
                            "Error"
                        );
                    }
                });
            } catch (Exception ex) {
                Messages.showErrorDialog(
                    project,
                    "Error creating file: " + ex.getMessage(),
                    "Error"
                );
            }
        }
    }

    private void refreshProjectFiles() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();
        
        String basePath = project.getBasePath();
        VirtualFile projectDir = basePath != null ? LocalFileSystem.getInstance().findFileByPath(basePath) : null;

        buildProjectTree(projectDir, root);
        
        treeModel.reload();
        
        // 刷新后默认展开所有节点
        TreeUtil.expandAll(fileTree);
    }

    private void buildProjectTree(VirtualFile directory, DefaultMutableTreeNode parent) {
        // 收集当前目录下的所有内容
        List<VirtualFile> dirs = new ArrayList<>();
        List<VirtualFile> bizFiles = new ArrayList<>();
        collectContent(directory, dirs, bizFiles);
        
        // 如果当前目录及其子目录都没有biz文件，则不显示
        if (!hasBizFiles(directory)) {
            return;
        }
        
        // 处理目录
        for (VirtualFile dir : dirs) {
            // 只处理包含biz文件的目录
            if (hasBizFiles(dir)) {
                // 尝试获取可压缩的路径
                Pair<String, VirtualFile> compressedInfo = getCompressiblePath(dir);
                String compressedPath = compressedInfo.getFirst();
                VirtualFile lastDir = compressedInfo.getSecond();
                
                if (!compressedPath.equals(dir.getName())) {
                    // 如果路径可以压缩，创建压缩节点
                    DefaultMutableTreeNode compressedNode = new DefaultMutableTreeNode(
                        new CompressedDirectory(dir, compressedPath)
                    );
                    parent.add(compressedNode);
                    // 继续处理最后一个目录的内容
                    buildProjectTree(lastDir, compressedNode);
                } else {
                    // 如果路径不能压缩，正常添加节点
                    DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(dir);
                    parent.add(dirNode);
                    buildProjectTree(dir, dirNode);
                }
            }
        }
        
        // 处理biz文件
        for (VirtualFile file : bizFiles) {
            try {
                BizFileInfo bizFileInfo = BizFileUtils.parseBizFile(file);
                DefaultMutableTreeNode bizNode = new DefaultMutableTreeNode(bizFileInfo);
                parent.add(bizNode);
            } catch (Exception e) {
                DefaultMutableTreeNode bizNode = new DefaultMutableTreeNode(new BizFileInfo(file));
                parent.add(bizNode);
            }
        }
    }

    // 添加新的辅助方法，检查目录及其子目录是否包含biz文件
    private boolean hasBizFiles(VirtualFile directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }
        
        // 检查当前目录
        VirtualFile[] children = directory.getChildren();
        if (children == null) {
            return false;
        }
        
        for (VirtualFile child : children) {
            if (!child.isDirectory() && "biz".equals(child.getExtension())) {
                return true;
            }
            
            if (child.isDirectory() && hasBizFiles(child)) {
                return true;
            }
        }
        
        return false;
    }

    private void filterFiles(String searchText) {
        if (searchText.isEmpty()) {
            refreshProjectFiles();  // 这里会自动展开所有节点
            return;
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();
        
        searchText = searchText.toLowerCase();
        String basePath = project.getBasePath();
        VirtualFile projectDir = basePath != null ? LocalFileSystem.getInstance().findFileByPath(basePath) : null;

        searchAndAddMatchingFiles(projectDir, root, searchText);
        
        treeModel.reload();
        TreeUtil.expandAll(fileTree);  // 搜索后也展开所有节点
    }

    private boolean searchAndAddMatchingFiles(VirtualFile directory, DefaultMutableTreeNode parent, String searchText) {
        if (directory == null || !directory.isValid()) {
            return false;
        }

        VirtualFile[] children = directory.getChildren();
        boolean hasMatches = false;
        
        for (VirtualFile child : children) {
            if (child.getName().equals("target") || child.getName().startsWith(".")) {
                continue;
            }
            
            if (child.isDirectory()) {
                DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(child);
                // 递归搜索子目录
                if (searchAndAddMatchingFiles(child, dirNode, searchText)) {
                    parent.add(dirNode);
                    hasMatches = true;
                }
            } else if ("biz".equals(child.getExtension())) {
                BizFileInfo bizFileInfo = BizFileUtils.parseBizFile(child);
                if (matchesBizFile(bizFileInfo, searchText)) {
                    DefaultMutableTreeNode bizNode = new DefaultMutableTreeNode(bizFileInfo);
                    parent.add(bizNode);
                    hasMatches = true;
                }
            }
        }
        
        return hasMatches;
    }

    private boolean matchesBizFile(BizFileInfo bizFileInfo, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        
        searchText = searchText.toLowerCase();
        return bizFileInfo.getFile().getName().toLowerCase().contains(searchText) ||
               bizFileInfo.getUrl().toLowerCase().contains(searchText) ||
               bizFileInfo.getMethod().toLowerCase().contains(searchText);
    }

    private void renameBizFile(BizFileInfo bizFileInfo) {
        String newName = Messages.showInputDialog(
            project,
            "创建新文件:",
            "重命名",
            Messages.getQuestionIcon(),
            bizFileInfo.getFile().getNameWithoutExtension(),
            null
        );
        
        if (newName != null && !newName.isEmpty()) {
            if (!newName.toLowerCase().endsWith(".biz")) {
                newName += ".biz";
            }
            
            try {
                String finalNewName = newName;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        bizFileInfo.getFile().rename(this, finalNewName);
                        refreshProjectFiles();
                    } catch (IOException ex) {
                        Messages.showErrorDialog(
                            project,
                            "Error renaming file: " + ex.getMessage(),
                            "Rename Error"
                        );
                    }
                });
            } catch (Exception ex) {
                Messages.showErrorDialog(
                    project,
                    "Error renaming file: " + ex.getMessage(),
                    "Rename Error"
                );
            }
        }
    }

    private void deleteBizFile(BizFileInfo bizFileInfo) {
        int result = Messages.showYesNoDialog(
            project,
            "你确定删除 '" + bizFileInfo.getFile().getName() + "'?",
            "删除文件",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            try {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        bizFileInfo.getFile().delete(this);
                        refreshProjectFiles();
                    } catch (IOException ex) {
                        Messages.showErrorDialog(
                            project,
                            "Error deleting file: " + ex.getMessage(),
                            "Delete Error"
                        );
                    }
                });
            } catch (Exception ex) {
                Messages.showErrorDialog(
                    project,
                    "Error deleting file: " + ex.getMessage(),
                    "Delete Error"
                );
            }
        }
    }

    private static class CreateDirectoryDialog extends DialogWrapper {
        private final JTextField pathField;
        private final String baseText;

        public CreateDirectoryDialog(Project project, String title, String baseText, String initialPath, Icon icon) {
            super(project, true);
            this.baseText = baseText;
            setTitle(title);
            
            pathField = new JTextField(initialPath);
            // 选中文本框中的所有文本
            pathField.addAncestorListener(new AncestorListener() {
                @Override
                public void ancestorAdded(AncestorEvent event) {
                    pathField.selectAll();
                    pathField.requestFocusInWindow();
                }
                @Override
                public void ancestorRemoved(AncestorEvent event) {}
                @Override
                public void ancestorMoved(AncestorEvent event) {}
            });
            
            init();
        }

        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 5));
            panel.setPreferredSize(new Dimension(400, 60));
            
            JLabel label = new JLabel(baseText);
            label.setBorder(JBUI.Borders.empty(0, 0, 5, 0));
            
            panel.add(label, BorderLayout.NORTH);
            panel.add(pathField, BorderLayout.CENTER);
            
            // 添加提示信息
            JLabel tipLabel = new JLabel("Use '.' or '/' as separator for nested directories");
            tipLabel.setForeground(UIManager.getColor("Label.infoForeground"));
            tipLabel.setFont(JBUI.Fonts.smallFont());
            panel.add(tipLabel, BorderLayout.SOUTH);
            
            return panel;
        }

        public String getEnteredPath() {
            return pathField.getText().trim();
        }
    }

    // 添加新的辅助方法
    private void collectContent(VirtualFile dir, List<VirtualFile> dirs, List<VirtualFile> bizFiles) {
        VirtualFile[] children = dir.getChildren();
        for (VirtualFile child : children) {
            if (child.getName().equals("target") || child.getName().startsWith(".")) {
                continue;
            }
            
            if (child.isDirectory()) {
                dirs.add(child);
            } else if ("biz".equals(child.getExtension())) {
                bizFiles.add(child);
            }
        }
    }

    // 添加新的辅助方法来获取可压缩的路径
    private Pair<String, VirtualFile> getCompressiblePath(VirtualFile startDir) {
        StringBuilder path = new StringBuilder(startDir.getName());
        VirtualFile currentDir = startDir;
        
        while (true) {
            // 获取当前目录的子内容
            VirtualFile[] children = currentDir.getChildren();
            if (children == null) break;
            
            // 统计子目录和biz文件
            List<VirtualFile> subDirs = new ArrayList<>();
            boolean hasBizFiles = false;
            
            for (VirtualFile child : children) {
                if (child.getName().equals("target") || child.getName().startsWith(".")) {
                    continue;
                }
                if (child.isDirectory()) {
                    subDirs.add(child);
                } else if ("biz".equals(child.getExtension())) {
                    hasBizFiles = true;
                    break;
                }
            }
            
            // 如果当前目录有biz文件或者有多个子目录，停止压缩
            if (hasBizFiles || subDirs.size() != 1) {
                break;
            }
            
            // 如果只有一个子目录，继续压缩
            if (subDirs.size() == 1) {
                currentDir = subDirs.get(0);
                path.append(".").append(currentDir.getName());
            } else {
                break;
            }
        }
        
        return new Pair<>(path.toString(), currentDir);
    }

    // 添加Pair类
    private static class Pair<A, B> {
        private final A first;
        private final B second;
        
        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
        
        public A getFirst() { return first; }
        public B getSecond() { return second; }
    }

    // 修改getPackagePath方法
    private String getPackagePath(VirtualFile directory) {
        if (directory == null) return "";
        
        // 获取当前选中的节点
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
        if (node == null) return "";
        
        // 获取从根到当前节点的完整路径
        List<String> pathParts = new ArrayList<>();
        TreePath treePath = new TreePath(node.getPath());
        
        // 从根节点开始收集所有路径部分
        for (int i = 0; i < treePath.getPathCount(); i++) {
            DefaultMutableTreeNode pathNode = (DefaultMutableTreeNode) treePath.getPathComponent(i);
            Object userObject = pathNode.getUserObject();
            
            if (userObject instanceof CompressedDirectory) {
                // 对于压缩目录，获取其完整路径
                CompressedDirectory compDir = (CompressedDirectory) userObject;
                // 分解压缩路径并添加各个部分
                String[] parts = compDir.getDisplayPath().split("\\.");
                Collections.addAll(pathParts, parts);
            } else if (userObject instanceof VirtualFile) {
                VirtualFile file = (VirtualFile) userObject;
                // 跳过src目录和项目根目录

                String basePath = project.getBasePath();
                VirtualFile baseDir = basePath != null ? LocalFileSystem.getInstance().findFileByPath(basePath) : null;
                if (!"src".equals(file.getName()) && !file.equals(baseDir)) {
                    pathParts.add(file.getName());
                }
            }
        }
        
        // 获取当前节点的父节点路径中的所有压缩目录
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        while (parent != null && !parent.isRoot()) {
            Object userObject = parent.getUserObject();
            if (userObject instanceof CompressedDirectory) {
                CompressedDirectory compDir = (CompressedDirectory) userObject;
                // 在开头添加压缩目录的路径
                String[] parts = compDir.getDisplayPath().split("\\.");
                for (int i = parts.length - 1; i >= 0; i--) {
                    if (!pathParts.contains(parts[i])) {
                        pathParts.add(0, parts[i]);
                    }
                }
            }
            parent = (DefaultMutableTreeNode) parent.getParent();
        }
        
        // 移除重复的路径部分并合并
        return String.join(".", new LinkedHashSet<>(pathParts));
    }

    // 添加新的删除压缩目录的方法
    private void deleteCompressedDirectory(CompressedDirectory compDir) {
        String confirmMessage = "Are you sure you want to delete directory '" +
                compDir.getDisplayPath() + "' and all its contents?";

        int result = Messages.showYesNoDialog(
                project,
                confirmMessage,
                "Delete Directory",
                Messages.getQuestionIcon()
        );

        if (result == Messages.YES) {
            try {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        // 直接删除压缩目录的起始目录
                        VirtualFile directoryToDelete = compDir.getDirectory();
                        if (directoryToDelete != null && directoryToDelete.exists()) {
                            directoryToDelete.delete(this);
                        }

                        refreshProjectFiles();
                    } catch (IOException ex) {
                        Messages.showErrorDialog(
                                project,
                                "Error deleting directory: " + ex.getMessage(),
                                "Delete Error"
                        );
                    }
                });
            } catch (Exception ex) {
                Messages.showErrorDialog(
                        project,
                        "Error deleting directory: " + ex.getMessage(),
                        "Delete Error"
                );
            }
        }
    }

    // 添加删除目录的方法
    private void deleteDirectory(VirtualFile directory) {
        // 获取要删除的目录名称显示
        String displayName = directory.getName();
        String confirmMessage = "Are you sure you want to delete directory '";

        // 检查是否是压缩目录的删除操作
        DefaultMutableTreeNode node = findNodeForFile(directory);
        if (node != null && node.getUserObject() instanceof CompressedDirectory) {
            CompressedDirectory compDir = (CompressedDirectory) node.getUserObject();
            displayName = compDir.getDisplayPath();
            directory = compDir.getDirectory(); // 获取压缩目录的起始目录
        }

        confirmMessage += displayName + "' and all its contents?";

        int result = Messages.showYesNoDialog(
                project,
                confirmMessage,
                "Delete Directory",
                Messages.getQuestionIcon()
        );

        if (result == Messages.YES) {
            try {
                VirtualFile finalDirectory = directory;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        if (node != null && node.getUserObject() instanceof CompressedDirectory) {
                            // 对于压缩目录，只删除压缩路径对应的目录
                            CompressedDirectory compDir = (CompressedDirectory) node.getUserObject();
                            String[] pathParts = compDir.getDisplayPath().split("\\.");
                            VirtualFile current = finalDirectory;

                            // 遍历到压缩路径的最后一个目录
                            for (int i = 0; i < pathParts.length - 1; i++) {
                                VirtualFile child = current.findChild(pathParts[i]);
                                if (child != null && child.isDirectory()) {
                                    current = child;
                                }
                            }

                            // 删除最后一个目录
                            VirtualFile targetDir = current.findChild(pathParts[pathParts.length - 1]);
                            if (targetDir != null) {
                                targetDir.delete(this);
                            }
                        } else {
                            // 普通目录的删除
                            finalDirectory.delete(this);
                        }
                        refreshProjectFiles();
                    } catch (IOException ex) {
                        Messages.showErrorDialog(
                                project,
                                "Error deleting directory: " + ex.getMessage(),
                                "Delete Error"
                        );
                    }
                });
            } catch (Exception ex) {
                Messages.showErrorDialog(
                        project,
                        "Error deleting directory: " + ex.getMessage(),
                        "Delete Error"
                );
            }
        }
    }

    // 修改deleteSelectedItems方法
    private void deleteSelectedItems(TreePath[] paths) {
        // 收集要删除的项目
        List<String> itemNames = new ArrayList<>();
        List<VirtualFile> filesToDelete = new ArrayList<>();
        
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();
            
            if (userObject instanceof VirtualFile) {
                VirtualFile file = (VirtualFile) userObject;
                itemNames.add(getFullPath(file));
                filesToDelete.add(file);
            } else if (userObject instanceof BizFileInfo) {
                BizFileInfo bizFileInfo = (BizFileInfo) userObject;
                itemNames.add(getFullPath(bizFileInfo.getFile()));
                filesToDelete.add(bizFileInfo.getFile());
            } else if (userObject instanceof CompressedDirectory) {
                CompressedDirectory compDir = (CompressedDirectory) userObject;
                itemNames.add(compDir.getDisplayPath());  // 使用压缩目录的显示路径
                filesToDelete.add(compDir.getDirectory());
            }
        }
        
        // 显示确认对话框
        String message = String.format("你确定要删除这 %d 个文件吗?\n%s",
            itemNames.size(), 
            String.join("\n", itemNames)
        );
        
        int result = Messages.showYesNoDialog(
            project,
            message,
            "多选删除",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            try {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        for (VirtualFile file : filesToDelete) {
                            file.delete(this);
                        }
                        refreshProjectFiles();
                    } catch (IOException ex) {
                        Messages.showErrorDialog(
                            project,
                            "Error deleting items: " + ex.getMessage(),
                            "Delete Error"
                        );
                    }
                });
            } catch (Exception ex) {
                Messages.showErrorDialog(
                    project,
                    "Error deleting items: " + ex.getMessage(),
                    "Delete Error"
                );
            }
        }
    }

    // 修改getFullPath方法
    private String getFullPath(VirtualFile file) {
        // 首先尝试获取当前节点的完整路径
        DefaultMutableTreeNode node = findNodeForFile(file);
        if (node != null) {
            // 获取从根节点到当前节点的完整路径
            TreePath treePath = new TreePath(node.getPath());
            StringBuilder fullPath = new StringBuilder();
            
            // 跳过根节点，从第二个节点开始处理
            for (int i = 1; i < treePath.getPathCount(); i++) {
                DefaultMutableTreeNode pathNode = (DefaultMutableTreeNode) treePath.getPathComponent(i);
                Object userObject = pathNode.getUserObject();
                
                if (userObject instanceof CompressedDirectory) {
                    // 对于压缩目录，使用其显示路径
                    if (fullPath.length() > 0) {
                        fullPath.append(".");
                    }
                    fullPath.append(((CompressedDirectory) userObject).getDisplayPath());
                } else if (userObject instanceof VirtualFile) {
                    // 对于普通目录，添加目录名
                    VirtualFile dirFile = (VirtualFile) userObject;
                    if (fullPath.length() > 0) {
                        fullPath.append(".");
                    }
                    fullPath.append(dirFile.getName());
                }
            }
            
            // 如果当前文件不是路径中的最后一个节点，添加文件名
            if (!file.equals(node.getUserObject())) {
                if (fullPath.length() > 0) {
                    fullPath.append(".");
                }
                fullPath.append(file.getName());
            }
            
            if (fullPath.length() > 0) {
                return fullPath.toString();
            }
        }
        
        // 如果无法通过树节点获取路径，使用基本的路径构建方法
        List<String> pathParts = new ArrayList<>();
        VirtualFile current = file;
        
        String basePath = project.getBasePath();
        VirtualFile projectRoot = basePath != null ? LocalFileSystem.getInstance().findFileByPath(basePath) : null;
        
        while (current != null && !current.equals(projectRoot)) {
            // 跳过src目录
            if (!"src".equals(current.getName())) {
                pathParts.add(0, current.getName());
            }
            current = current.getParent();
        }
        
        return String.join(".", pathParts);
    }

    // 添加查找文件对应节点的辅助方法
    private DefaultMutableTreeNode findNodeForFile(VirtualFile file) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        Enumeration<?> e = root.depthFirstEnumeration();
        
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            Object userObject = node.getUserObject();
            
            if (userObject instanceof VirtualFile && file.equals(userObject)) {
                return node;
            } else if (userObject instanceof CompressedDirectory 
                    && file.equals(((CompressedDirectory) userObject).getDirectory())) {
                return node;
            } else if (userObject instanceof BizFileInfo 
                    && file.equals(((BizFileInfo) userObject).getFile())) {
                return node;
            }
        }
        
        // 如果没有找到直接匹配的节点，尝试查找父目录
        VirtualFile parent = file.getParent();
        if (parent != null) {
            return findNodeForFile(parent);
        }
        
        return null;
    }

    private void openBizFile(BizFileInfo bizFileInfo) {
        try {
            VirtualFile file = bizFileInfo.getFile();
            if (file != null) {
                FileEditorManager.getInstance(project).openFile(file, true);
            }
        } catch (Exception e) {
            Messages.showErrorDialog(project, "Failed to open file: " + e.getMessage(), "Error");
        }
    }

    // 添加新的方法
    private void copyMarkdownToClipboard(BizFileInfo bizFileInfo) {

        if(!checkBizFile(bizFileInfo)){
            return;
        }

        String markdown = BizMarkdownGenerator.generateMarkdown(bizFileInfo);

        StringBuilder combinedMd = new StringBuilder();
        combinedMd.append("# API文档\n\n");
        combinedMd.append("## 目录\n\n");

        // 生成目录
        combinedMd.append(String.format("%d. [%s](#%d-%s)\n",
                1,
                bizFileInfo.getName(),
                1,
                bizFileInfo.getName().replaceAll("\\s+", "-")));
        combinedMd.append("\n---\n\n");
        combinedMd.append(String.format("## %d. %s\n\n",   1, bizFileInfo.getName()));
        combinedMd.append(markdown);
        combinedMd.append("---\n\n");

        StringSelection selection = new StringSelection(combinedMd.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        
        Messages.showInfoMessage(project, "Markdown内容已复制到剪贴板", "复制成功");
    }

    public boolean checkBizFile(BizFileInfo bizFileInfo) {
        if(bizFileInfo.getName()== null || bizFileInfo.getName().isEmpty()){
            Messages.showErrorDialog(project, "biz文件名称参数未设置", "错误");
            return false;
        }

        if(bizFileInfo.getUrl()== null || bizFileInfo.getUrl().isEmpty()){
            Messages.showErrorDialog(project, "biz文件Url未设置", "错误");
            return false;
        }
        return true;
    }

    private void saveMarkdownToFile(BizFileInfo bizFileInfo) {

        if(!checkBizFile(bizFileInfo)){
            return;
        }

        String markdown = BizMarkdownGenerator.generateMarkdown(bizFileInfo);
        StringBuilder combinedMd = new StringBuilder();
        combinedMd.append("# API文档\n\n");
        combinedMd.append("## 目录\n\n");

        // 生成目录
        combinedMd.append(String.format("%d. [%s](#%d-%s)\n",
                1,
                bizFileInfo.getName(),
                1,
                bizFileInfo.getName().replaceAll("\\s+", "-")));
        combinedMd.append("\n---\n\n");
        combinedMd.append(String.format("## %d. %s\n\n",   1, bizFileInfo.getName()));
        combinedMd.append(markdown);
        combinedMd.append("---\n\n");

        String combinedMdStr = combinedMd.toString();
        
        // 创建文件选择器
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存Markdown文件");
        fileChooser.setSelectedFile(new File(bizFileInfo.getFile().getNameWithoutExtension() + ".md"));
        
        // 设置文件过滤器
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Markdown files (*.md)", "md");
        fileChooser.setFileFilter(filter);
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // 确保文件有.md后缀
            if (!file.getName().toLowerCase().endsWith(".md")) {
                file = new File(file.getPath() + ".md");
            }
            
            try {
                Files.write(file.toPath(), combinedMdStr.getBytes(StandardCharsets.UTF_8));
                Messages.showInfoMessage(project, "Markdown文件已保存到: " + file.getPath(), "保存成功");
            } catch (IOException e) {
                Messages.showErrorDialog(project, "保存文件失败: " + e.getMessage(), "错误");
            }
        }
    }

    // 添加新的方法
    private void generateSingleMarkdown(VirtualFile directory) {
        List<BizFileInfo> bizFiles = new ArrayList<>();
        BizFileUtils.collectBizFiles(directory, bizFiles);
        
        if (bizFiles.isEmpty()) {
            Messages.showInfoMessage(project, "未找到biz文件", "提示");
            return;
        }
        
        // 创建文件选择器
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存API文档");
        fileChooser.setSelectedFile(new File("api-doc.md"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Markdown files (*.md)", "md"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".md")) {
                file = new File(file.getPath() + ".md");
            }
            
            try {
                StringBuilder combinedMd = new StringBuilder();
                combinedMd.append("# API文档\n\n");
                combinedMd.append("## 目录\n\n");
                
                // 生成目录
                for (int i = 0; i < bizFiles.size(); i++) {
                    BizFileInfo bizFile = bizFiles.get(i);
                    combinedMd.append(String.format("%d. [%s](#%d-%s)\n", 
                        i + 1, 
                        bizFile.getName(),
                        i + 1,
                        bizFile.getName().replaceAll("\\s+", "-")));
                }
                combinedMd.append("\n---\n\n");
                
                // 生成详细文档
                for (int i = 0; i < bizFiles.size(); i++) {
                    BizFileInfo bizFile = bizFiles.get(i);
                    combinedMd.append(String.format("## %d. %s\n\n", i + 1, bizFile.getName()));
                    combinedMd.append(BizMarkdownGenerator.generateMarkdown(bizFile));
                    combinedMd.append("---\n\n");
                }
                
                Files.write(file.toPath(), combinedMd.toString().getBytes(StandardCharsets.UTF_8));
                Messages.showInfoMessage(project, "API文档已保存到: " + file.getPath(), "生成成功");
                
            } catch (IOException e) {
                Messages.showErrorDialog(project, "保存文件失败: " + e.getMessage(), "错误");
            }
        }
    }

} 