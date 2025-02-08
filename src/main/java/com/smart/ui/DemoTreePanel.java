package com.smart.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.smart.settings.SmartPluginSettings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.List;

import com.smart.bean.DemoItem;

public class DemoTreePanel extends JPanel {
    private final Project project;
    private Tree tree;
    private List<DemoItem> demoItems;
    private Map<String, DemoItem> demoItemMap = new HashMap<>();
    private Map<String, Boolean> fileDownloadStatus = new HashMap<>();
    private VisualLayoutPanel visualLayoutPanel;
    private JPopupMenu popupMenu;
    private SearchTextField searchField;
    
    public DemoTreePanel(Project project, VisualLayoutPanel visualLayoutPanel) {
        this.project = project;
        this.visualLayoutPanel = visualLayoutPanel;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIManager.getColor("Tree.background"));
        
        // 创建顶部面板
        createTopPanel();
        
        // 初始化树
        initTree();
        
        // 加载远程数据
        loadRemoteData();
    }

    private void createTopPanel() {
        // 创建顶部工具栏面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(JBUI.Borders.empty(0, 0, 1, 0));
        
        // 创建工具栏按钮组
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        // 添加刷新按钮
        actionGroup.add(new AnAction("Refresh", "Refresh demo list", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                refreshDemoList();
            }
        });
        
        // 添加展开全部按钮
        actionGroup.add(new AnAction("Expand All", "Expand all nodes", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                expandAll(true);
            }
        });
        
        // 添加折叠全部按钮
        actionGroup.add(new AnAction("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                expandAll(false);
            }
        });

        // 添加下载全部按钮
        actionGroup.add(new AnAction("Download All", "Download all demos", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                downloadAllDemos();
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "DemoTreeToolbar",
                actionGroup,
                true
        );
        toolbar.setTargetComponent(this);
        
        // 创建搜索框
        searchField = new SearchTextField();
        searchField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                filterDemos(searchField.getText());
            }
        });
        
        // 创建一个面板包含工具栏和搜索框
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.add(toolbar.getComponent(), BorderLayout.WEST);
        toolbarPanel.add(searchField, BorderLayout.CENTER);
        
        // 将工具栏面板添加到顶部面板
        topPanel.add(toolbarPanel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);
    }

    private void expandAll(boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(new TreePath(root), expand);
    }

    private void expandAll(TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            if (parent.getPathCount() > 1) {
                tree.collapsePath(parent);
            }
        }
    }

    private void initTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Demo");
        tree = new Tree(new DefaultTreeModel(root));
        
        // 设置自定义的单元格渲染器
        tree.setCellRenderer(new DemoTreeCellRenderer());
        tree.setRootVisible(false);
        
        // 创建右键菜单
        createPopupMenu();
        
        // 添加鼠标事件监听器
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 处理双击事件
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.isLeaf() && node.getParent() != null) {
                            handleDemoItemDoubleClick(node);
                        }
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                showPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupMenu(e);
            }
        });
        
        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadRemoteData() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(SmartPluginSettings.API_DOMAIN+ "/demo/tree");
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String jsonStr = EntityUtils.toString(entity);
                        ObjectMapper mapper = new ObjectMapper();
                        demoItems = mapper.readValue(jsonStr, 
                            mapper.getTypeFactory().constructCollectionType(List.class, DemoItem.class));
                        
                        // 更新UI需要在EDT线程中执行
                        ApplicationManager.getApplication().invokeLater(() -> {
                            updateTree();
                            checkLocalFiles();

                            expandAll(true); // 刷新后默认展开所有节点
                        });
                    }
                }
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog("加载远程数据失败: " + e.getMessage(), "错误");
                });
            }
        });
    }

    private void updateTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        root.removeAllChildren();
        demoItemMap.clear();
        
        for (DemoItem item : demoItems) {
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(item.getName());
            root.add(categoryNode);
            
            if (item.getChildren() != null) {
                for (DemoItem child : item.getChildren()) {
                    DefaultMutableTreeNode demoNode = new DefaultMutableTreeNode(child.getName());
                    categoryNode.add(demoNode);
                    demoItemMap.put(child.getName(), child);
                }
            }
        }
        
        ((DefaultTreeModel) tree.getModel()).reload();
    }

    private void checkLocalFiles() {
        fileDownloadStatus.clear();
        String demoPath = project.getBasePath() + "/src/main/resources/demo/";
        File demoDir = new File(demoPath);
        Set<String> existingFiles = new HashSet<>();
        
        if (demoDir.exists()) {
            File[] files = demoDir.listFiles((dir, name) -> name.endsWith(".biz"));
            if (files != null) {
                for (File file : files) {
                    existingFiles.add(file.getName());
                }
            }
        }
        
        for (DemoItem item : demoItems) {
            if (item.getChildren() != null) {
                for (DemoItem child : item.getChildren()) {
                    fileDownloadStatus.put(child.getName(), existingFiles.contains(child.getName()));
                }
            }
        }
        
        tree.repaint();
    }

    private void refreshDemoList() {
        loadRemoteData();
    }

    private String getFileName(String category, String demoName) {
        return category.replaceAll("\\s+", "_") + "_" + 
               demoName.replaceAll("\\s+", "_") + ".biz";
    }

    // 自定义树节点渲染器
    private class DemoTreeCellRenderer extends DefaultTreeCellRenderer {
        private final JPanel renderer = new JPanel(new BorderLayout());
        private final JLabel iconLabel = new JLabel();
        private final JLabel textLabel = new JLabel();
        
        public DemoTreeCellRenderer() {
            renderer.setOpaque(true);
            renderer.add(iconLabel, BorderLayout.WEST);
            renderer.add(textLabel, BorderLayout.CENTER);
            
            // 设置组件属性
            textLabel.setOpaque(false);
            iconLabel.setOpaque(false);
            
            // 设置图标和文本之间的间距
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            String text = node.getUserObject().toString();
            
            // 设置图标
            Icon icon;
            if (node.isRoot()) {
                icon = expanded ? AllIcons.Nodes.Folder : AllIcons.Nodes.Folder;
            } else if (node.getParent() == tree.getModel().getRoot()) {
                icon = expanded ? AllIcons.Nodes.Folder : AllIcons.Nodes.Folder;
            } else {
                icon = AllIcons.FileTypes.Any_type;
            }
            iconLabel.setIcon(icon);
            
            // 获取当前搜索文本
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isEmpty()) {
                // 处理高亮显示
                highlightText(text, searchText);
            } else {
                textLabel.setText(text);
                textLabel.setForeground(UIManager.getColor("Tree.textForeground"));
            }
            
            // 设置选中状态的背景色
            Color bg = selected ? UIManager.getColor("Tree.selectionBackground") : UIManager.getColor("Tree.textBackground");
            Color fg = selected ? UIManager.getColor("Tree.selectionForeground") : UIManager.getColor("Tree.textForeground");
            
            renderer.setBackground(bg);
            textLabel.setForeground(fg);
            
            return renderer;
        }
        
        private void highlightText(String text, String searchText) {
            if (searchText == null || searchText.isEmpty()) {
                textLabel.setText(text);
                return;
            }
            
            searchText = searchText.toLowerCase();
            String lowerText = text.toLowerCase();
            int index = lowerText.indexOf(searchText);
            
            if (index >= 0) {
                // 使用HTML实现高亮
                StringBuilder sb = new StringBuilder("<html>");
                sb.append(text.substring(0, index));
                sb.append("<span style='background-color: #FFFF00'>");
                sb.append(text.substring(index, index + searchText.length()));
                sb.append("</span>");
                sb.append(text.substring(index + searchText.length()));
                sb.append("</html>");
                textLabel.setText(sb.toString());
            } else {
                textLabel.setText(text);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(200, size.height);
    }

    // 修改handleDemoItemDoubleClick方法
    private void handleDemoItemDoubleClick(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
        String category = parentNode.toString();
        String demoName = node.toString();
        
        // 检查文件是否已存在
        String demoPath = project.getBasePath() + "/src/main/resources/demo/";
        String fileName = getFileName(category, demoName);
        File targetFile = new File(demoPath, fileName);
        
        if (targetFile.exists()) {
            // 文件已存在,直接打开
            openDemoFile(targetFile);
        } else {
            // 文件不存在,提示下载
            String[] options = {"下载并打开"};
            int choice = Messages.showDialog(
                "请选择操作方式",
                "Demo操作",
                options,
                0,
                Messages.getQuestionIcon()
            );
            
            if (choice == 0) { // 下载并打开
                downloadAndOpenDemo(category, demoName);
            }
        }
    }

    // 添加新方法用于打开文件
    private void openDemoFile(File file) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if (virtualFile != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                FileEditorManager.getInstance(project).openFile(virtualFile, true);
            });
        }
    }

    // 修改downloadAndOpenDemo方法,复用openDemoFile方法
    private void downloadAndOpenDemo(String category, String demoName) {
        try {
            // 确保demo目录存在
            String demoPath = project.getBasePath() + "/src/main/resources/demo/";
            File demoDir = new File(demoPath);
            if (!demoDir.exists()) {
                demoDir.mkdirs();
            }
            
            // 构建文件名
            String fileName = getFileName(category, demoName);
            File targetFile = new File(demoDir, fileName);
            
            boolean downloadSuccess = downloadDemoFile(category, demoName, targetFile);
            
            if (downloadSuccess) {
                openDemoFile(targetFile);
                
                // 更新文件状态缓存
                fileDownloadStatus.put(category + "/" + demoName, true);
                tree.repaint();
            }
        } catch (Exception e) {
            Messages.showErrorDialog(
                "下载文件失败: " + e.getMessage(),
                "错误"
            );
        }
    }

    // 添加下载并打开文件的方法
    private void downloadAndRenderToCanvas(String category, String demoName) {
        try {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                DemoItem demoItem = demoItemMap.get(demoName);
                if (demoItem == null) {
                    return;
                }

                String url = SmartPluginSettings.API_DOMAIN+ "/demo/file/" + demoItem.getId();
                HttpGet httpGet = new HttpGet(url);

                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        // 获取响应内容并处理字符串
                        String trimStr = EntityUtils.toString(entity, "UTF-8").trim();

                        String content = trimStr
                                .replaceAll("^\"|\"$", "") // 去除前后引号
                                .replaceAll("\\\\\"", "\"") // 处理所有的双引号转义
                                .replaceAll("\\\\n", "\n") // 处理换行符
                                .replaceAll("\\\\", ""); // 处理剩余的反斜杠

                        ApplicationManager.getApplication().invokeLater(() -> {
                            // 调用VisualLayoutPanel的方法来渲内容
                            visualLayoutPanel.renderDemoContent(content);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Messages.showErrorDialog(
                    "下载文件失败: " + e.getMessage(),
                    "错误"
            );
        }
    }

    // 添加下载文件的方法
    private boolean downloadDemoFile(String category, String demoName, File targetFile) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            DemoItem demoItem = demoItemMap.get(demoName);
            if (demoItem == null) {
                return false;
            }
            
            String url = SmartPluginSettings.API_DOMAIN+ "/demo/file/" + demoItem.getId();
            HttpGet httpGet = new HttpGet(url);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // 获取响应内容并处理字符串
                    String content = EntityUtils.toString(entity, "UTF-8")
                            .trim() // 去除前后空格
                            .replaceAll("^\"|\"$", "") // 去除前后引号
                            .replaceAll("\\\\\"", "\"") // 处理所有的双引号转义
                            .replaceAll("\\\\n", "\n") // 处理换行符
                            .replaceAll("\\\\", ""); // 处理剩余的反斜杠


                    // 使用Writer写入处理后的内容
                    try (FileOutputStream fos = new FileOutputStream(targetFile);
                         OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
                        writer.write(content);
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    // 添加创建右键菜单的方法
    private void createPopupMenu() {
        popupMenu = new JPopupMenu();
        
        // 下载并打菜单项
        JMenuItem downloadItem = new JMenuItem("下载并打开 (双击选择)", AllIcons.Actions.Download);
        downloadItem.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
                tree.getLastSelectedPathComponent();
            if (node != null && node.isLeaf()) {
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                downloadAndOpenDemo(parentNode.toString(), node.toString());
            }
        });
        
        // 渲染到画布菜单项
        // JMenuItem renderItem = new JMenuItem("渲染到画布 (双击选择)", AllIcons.Actions.Preview);
        // renderItem.addActionListener(e -> {
        //     DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
        //         tree.getLastSelectedPathComponent();
        //     if (node != null && node.isLeaf()) {
        //         DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
        //         downloadAndRenderToCanvas(parentNode.toString(), node.toString());
        //     }
        // });
        
        popupMenu.add(downloadItem);
        //popupMenu.add(renderItem);
    }

    // 添加显示右键菜单的方法
    private void showPopupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path != null) {
                tree.setSelectionPath(path); // 选中当前右键的节点
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.isLeaf() && node.getParent() != null) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }

    private void downloadAllDemos() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        List<DemoDownloadInfo> downloadList = new ArrayList<>();
        collectDownloadNodes(root, downloadList);
        
        if (downloadList.isEmpty()) {
            Messages.showInfoMessage("没有找到需要下载的Demo文件", "提示");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "下载Demo文件", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                int total = downloadList.size();
                int success = 0;
                
                for (int i = 0; i < total; i++) {
                    if (indicator.isCanceled()) break;
                    
                    DemoDownloadInfo info = downloadList.get(i);
                    indicator.setFraction((double) i / total);
                    indicator.setText(String.format("正在下载 (%d/%d): %s", i + 1, total, info.demoName));
                    
                    // 确保demo目录存在
                    String demoPath = project.getBasePath() + "/src/main/resources/demo/";
                    File demoDir = new File(demoPath);
                    if (!demoDir.exists()) {
                        demoDir.mkdirs();
                    }
                    
                    // 构建文件名并下载
                    String fileName = getFileName(info.category, info.demoName);
                    File targetFile = new File(demoDir, fileName);
                    
                    if (downloadDemoFile(info.category, info.demoName, targetFile)) {
                        success++;
                    }
                }
                
                final int finalSuccess = success;
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showInfoMessage(
                        String.format("下载完成\n成功: %d\n失败: %d", finalSuccess, total - finalSuccess),
                        "下载结果"
                    );
                    checkLocalFiles(); // 刷新文件状态
                });
            }
        });
    }

    private void collectDownloadNodes(DefaultMutableTreeNode node, List<DemoDownloadInfo> downloadList) {
        if (node.isLeaf() && node.getParent() != null && node.getParent() != tree.getModel().getRoot()) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            downloadList.add(new DemoDownloadInfo(
                parentNode.toString(),
                node.toString()
            ));
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                collectDownloadNodes((DefaultMutableTreeNode) node.getChildAt(i), downloadList);
            }
        }
    }

    private static class DemoDownloadInfo {
        String category;
        String demoName;
        
        DemoDownloadInfo(String category, String demoName) {
            this.category = category;
            this.demoName = demoName;
        }
    }

    // 添加过滤Demo的方法
    private void filterDemos(String searchText) {
        if (searchText.isEmpty()) {
            updateTree(); // 如果搜索文本为空,显示所有节点
            return;
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        root.removeAllChildren();
        
        searchText = searchText.toLowerCase();
        
        // 遍历并过滤Demo项
        for (DemoItem item : demoItems) {
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(item.getName());
            boolean hasMatchingChild = false;
            
            if (item.getChildren() != null) {
                for (DemoItem child : item.getChildren()) {
                    // 检查是否匹配搜索文本
                    if (child.getName().toLowerCase().contains(searchText)) {
                        DefaultMutableTreeNode demoNode = new DefaultMutableTreeNode(child.getName());
                        categoryNode.add(demoNode);
                        demoItemMap.put(child.getName(), child);
                        hasMatchingChild = true;
                    }
                }
            }
            
            if (hasMatchingChild) {
                root.add(categoryNode);
            }
        }
        
        ((DefaultTreeModel) tree.getModel()).reload();
        expandAll(true); // 展开所有节点以显示搜索结果
    }
}