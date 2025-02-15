package com.smart.archive;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.smart.cache.PluginCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.options.ShowSettingsUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ArchiveDialog extends DialogWrapper {

    private final Project project;
    private final ArchiveManager archiveManager;
    private JBTable table;
    private ArchiveTableModel tableModel;
    private JComboBox<String> timeRangeCombo;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private List<Map<String, Object>> allArchives = new ArrayList<>();
    private boolean isLoading = false;
    private JPanel loadingPanel;
    private JLabel statusLabel;

    public ArchiveDialog(Project project, ArchiveManager archiveManager) {
        super(project, true);
        this.project = project;
        this.archiveManager = archiveManager;
        setModal(false);
        init();
        setTitle("存档管理");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(720, 450));

        // 添加 loading panel
        mainPanel.add(createLoadingPanel(), BorderLayout.CENTER);
        
        // 添加VIP提示面板
        if (!PluginCache.isValidLicense) {
            JPanel tipPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            tipPanel.setBackground(new Color(255, 243, 205)); // 浅黄色背景
            tipPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            
            JLabel tipLabel = new JLabel("开通VIP,即可使用云端存储功能,随时随地管理您的存档!");
            tipLabel.setFont(tipLabel.getFont().deriveFont(Font.BOLD, 14));
            tipLabel.setForeground(new Color(133, 100, 4)); // 深黄色文字
            
            JButton upgradeBtn = new JButton("立即开通");
            upgradeBtn.setFont(upgradeBtn.getFont().deriveFont(Font.BOLD));
            upgradeBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
            upgradeBtn.setFocusPainted(false);
            upgradeBtn.addActionListener(e -> {
                // 打开设置页面
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    project,
                    "com.smart.settings.SmartPluginSettingsConfigurable"
                );
                // 关闭当前对话框
                hide();
            });
            
            tipPanel.add(tipLabel);
            tipPanel.add(Box.createHorizontalStrut(10));
            tipPanel.add(upgradeBtn);
            
            mainPanel.add(tipPanel, BorderLayout.NORTH);
        }

        // 创建内容面板(包含过滤面板和表格)
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // 创建过滤面板
        JPanel filterPanel = createFilterPanel();
        contentPanel.add(filterPanel, BorderLayout.NORTH);

        // 创建表格
        createTable();
        JBScrollPane scrollPane = new JBScrollPane(table);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 创建分页面板
        JPanel pagePanel = createPagePanel();
        mainPanel.add(pagePanel, BorderLayout.SOUTH);

        // 异步加载初始数据
        loadArchivesAsync();

        return mainPanel;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(JBUI.Borders.empty(5));

        panel.add(new JBLabel("时间范围:"));

        timeRangeCombo = new JComboBox<>(new String[]{
                "最近一天",
                "最近三天", 
                "最近一周",
                "最近一月",
                "最近三月"
        });

        timeRangeCombo.addActionListener(e -> {
            loadArchives();
        });

        panel.add(timeRangeCombo);

        // 添加刷新按钮
        JButton refreshBtn = new JButton("刷新");
        refreshBtn.addActionListener(e -> loadArchives());
        panel.add(refreshBtn);

        // 添加批量下载按钮
        JButton batchDownloadBtn = new JButton("从远端批量下载");
        batchDownloadBtn.addActionListener(e -> batchDownload());
        panel.add(batchDownloadBtn);

        return panel;
    }

    private void createTable() {
        tableModel = new ArchiveTableModel();
        table = new JBTable(tableModel);

        // 设置表格样式
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);

        // 禁用表格选择
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.setFocusable(false);

        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(250); // 时间列
        table.getColumnModel().getColumn(1).setPreferredWidth(250); // 描述列
        table.getColumnModel().getColumn(2).setPreferredWidth(200); // 类型列
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // 下载状态列
        table.getColumnModel().getColumn(4).setPreferredWidth(100); // 上传状态列
        table.getColumnModel().getColumn(5).setPreferredWidth(300); // 操作列

        // 设置时间列渲染器
        DefaultTableCellRenderer timeRenderer = new DefaultTableCellRenderer() {
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat parseFormat = new SimpleDateFormat("yyyyMMddHHmmss");

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                if (value instanceof String) {
                    try {
                        // 解析原始格式
                        Date date = parseFormat.parse((String)value);
                        // 转换为显示格式
                        value = displayFormat.format(date);
                    } catch (ParseException e) {
                        // 如果解析失败,保持原值
                    }
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(timeRenderer);

        // 设置类型列的header renderer
        table.getColumnModel().getColumn(2).setHeaderRenderer(new DefaultTableCellRenderer() {
            private final JCheckBox checkBox = new JCheckBox();
            {
                checkBox.setHorizontalAlignment(JLabel.CENTER);
                checkBox.setBackground(table.getTableHeader().getBackground());
            }
            
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                panel.setBackground(table.getTableHeader().getBackground());
                panel.add(checkBox);
                panel.add(new JLabel("类型"));
                return panel;
            }
        });

        // 添加header点击事件
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col == 2) { // 类型列
                    // 获取当前页的数据
                    int start = (currentPage - 1) * PAGE_SIZE;
                    int end = Math.min(start + PAGE_SIZE, allArchives.size());
                    List<Map<String, Object>> pageData = allArchives.subList(start, end);
                    
                    // 检查当前页是否全部选中
                    boolean allSelected = pageData.stream()
                        .filter(archive -> "2".equals(archive.get("type")))
                        .allMatch(archive -> Boolean.TRUE.equals(archive.get("checked")));
                    
                    // 切换选中状态
                    boolean newState = !allSelected;
                    
                    // 更新当前页的选中状态
                    pageData.stream()
                        .filter(archive -> "2".equals(archive.get("type")))
                        .forEach(archive -> archive.put("checked", newState));
                    
                    // 刷新表格显示
                    table.repaint();
                }
            }
        });

        // 修改类型列渲染器
        DefaultTableCellRenderer typeRenderer = new DefaultTableCellRenderer() {
            @Override 
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                Map<String, Object> archive = allArchives.get((currentPage - 1) * PAGE_SIZE + row);
                
                // 添加复选框
                JCheckBox checkBox = new JCheckBox();
                checkBox.setOpaque(false);
                
                // 根据type设置复选框状态
                if ("2".equals(archive.get("type"))) {
                    checkBox.setEnabled(true);
                    // 从存储中获取选中状态
                    Boolean checked = (Boolean) archive.get("checked");
                    checkBox.setSelected(checked != null && checked);
                } else {
                    checkBox.setEnabled(false);
                    checkBox.setSelected(false);
                }
                panel.add(checkBox);
                
                // 添加类型文本
                String transText;
                if ("2".equals(archive.get("type"))) {
                    if (Boolean.TRUE.equals(archive.get("remote_only"))) {
                        transText = "云端";
                    } else {
                        transText = "本地、云端";
                    }
                } else {
                    transText = "本地";
                }
                panel.add(new JLabel(transText));
                
                return panel;
            }
        };
        table.getColumnModel().getColumn(2).setCellRenderer(typeRenderer);

        // 添加下载状态列渲染器
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Map<String, Object> archive = allArchives.get((currentPage - 1) * PAGE_SIZE + row);

                label.setOpaque(true);
                label.setBackground(Color.WHITE);
                
                String downloadStatus = (String) archive.get("downloadStatus");
                if (downloadStatus == null) {
                    label.setText("--");
                } else if ("downloading".equals(downloadStatus)) {
                    label.setText("下载中...");
                    label.setForeground(Color.BLUE);
                } else if ("completed".equals(downloadStatus)) {
                    label.setText("下载完成");
                    label.setForeground(new Color(0, 128, 0));
                }
                
                label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            }
        });

        // 设置操作列渲染器
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Map<String, Object> archive = allArchives.get((currentPage - 1) * PAGE_SIZE + row);

                label.setOpaque(true);
                label.setBackground(Color.WHITE);
                
                String uploadStatus = (String) archive.get("uploadStatus");
                if (uploadStatus == null) {
                    label.setText("--");
                } else if ("uploading".equals(uploadStatus)) {
                    label.setText("上传中...");
                    label.setForeground(Color.BLUE);
                } else if ("completed".equals(uploadStatus)) {
                    label.setText("上传完成");
                    label.setForeground(new Color(0, 128, 0));
                }
                
                label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            }
        });

        table.getColumnModel().getColumn(5).setCellRenderer(new OperationColumnRenderer());
        // 添加类型列的点击事件处理
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                
                // 处理类型列的点击
                if (col == 2) {
                    Map<String, Object> archive = allArchives.get((currentPage - 1) * PAGE_SIZE + row);
                    if ("2".equals(archive.get("type"))) {
                        // 切换选中状态
                        Boolean checked = (Boolean) archive.get("checked");
                        archive.put("checked", !Boolean.TRUE.equals(checked));
                        // 刷新表格
                        table.repaint();
                    }
                    return;
                }
                
                // 原有的操作列点击处理
                if (row < 0 || col != 5 || row >= table.getRowCount()) {
                    return;
                }
                
                // 获取当前行的操作按钮
                @SuppressWarnings("unchecked")
                List<JButton> operations = (List<JButton>) table.getClientProperty("operations_" + row);
                if (operations == null) {
                    return;
                }
                
                // 获取点击位置
                Rectangle cellRect = table.getCellRect(row, col, true);
                Point clickPoint = e.getPoint();
                clickPoint.translate(-cellRect.x - 5, -cellRect.y);
                
                // 计算点击了哪个操作
                int x = 0;
                Map<String, Object> archive = allArchives.get((currentPage - 1) * PAGE_SIZE + row);
                
                for (JButton button : operations) {
                    int width = button.getPreferredSize().width;
                    if (clickPoint.x >= x && clickPoint.x < x + width) {
                        if ("还原".equals(button.getText())) {
                            restoreArchive((String)archive.get("id"));
                        } else if ("下载".equals(button.getText())) {
                            downloadArchive(archive);
                        } else if ("上传".equals(button.getText())) {
                            uploadArchive(archive);
                        }
                        break;
                    }
                    x += width + 10; // 考虑间距
                }
            }
        });
    }

    private PaginationPanel paginationPanel;
    private JPanel createPagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 添加状态标签
        statusLabel = new JLabel("");
        statusLabel.setBorder(JBUI.Borders.empty(0, 5, 0, 0)); // 添加左边距
        panel.add(statusLabel, BorderLayout.WEST);
        
        // 添加分页控件
        paginationPanel = new PaginationPanel(() -> updateTable());
        panel.add(paginationPanel, BorderLayout.CENTER);
        
        return panel;
    }

    private void updateTable() {
        if (paginationPanel != null) {
            currentPage = Math.max(1, paginationPanel.getCurrentPage());
            int start = (currentPage - 1) * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, allArchives.size());
            
            List<Map<String, Object>> pageData = allArchives.subList(start, end);
            tableModel.setData(pageData);
            
            // 更新分页数据
            paginationPanel.updateData(allArchives.size());
        }
    }

    private void loadArchivesAsync() {
        if (isLoading) return;
        
        isLoading = true;
        loadingPanel.setVisible(true);
        statusLabel.setText("加载中...");
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "加载存档数据", false) {
            private List<Map<String, Object>> loadedArchives;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // 计算时间范围
                String startTime = null;
                String selectedRange = (String) timeRangeCombo.getSelectedItem();
                if (selectedRange != null) {
                    Calendar calendar = Calendar.getInstance();
                    switch (selectedRange) {
                        case "最近一天":
                            calendar.add(Calendar.DAY_OF_YEAR, -1);
                            break;
                        case "最近三天":
                            calendar.add(Calendar.DAY_OF_YEAR, -3);
                            break;
                        case "最近一周":
                            calendar.add(Calendar.DAY_OF_YEAR, -7);
                            break;
                        case "最近一月":
                            calendar.add(Calendar.MONTH, -1);
                            break;
                        case "最近三月":
                            calendar.add(Calendar.MONTH, -3);
                            break;
                    }
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    startTime = dateFormat.format(calendar.getTime());
                }

                // 获取数据
                loadedArchives = archiveManager.getArchives(startTime);
                
                // 保存当前已下载文件的状态
                Map<String, String> downloadStatuses = new HashMap<>();
                if (allArchives != null) {
                    allArchives.forEach(archive -> {
                        String time = (String) archive.get("time");
                        String status = (String) archive.get("downloadStatus");
                        if (status != null) {
                            downloadStatuses.put(time, status);
                        }
                    });
                }
                
                // 恢复下载状态
                loadedArchives.forEach(archive -> {
                    String time = (String) archive.get("time");
                    String status = downloadStatuses.get(time);
                    if (status != null) {
                        archive.put("downloadStatus", status);
                    }
                });

                // 排序
                Collections.sort(loadedArchives, (a, b) -> {
                    String timeA = (String) a.get("time");
                    String timeB = (String) b.get("time");
                    return timeB.compareTo(timeA);
                });
            }

            @Override
            public void onSuccess() {
                allArchives = loadedArchives;
                currentPage = 1;
                updateTable();
                loadingPanel.setVisible(false);
                isLoading = false;
                statusLabel.setText("已完成");
            }

            @Override
            public void onFinished() {
                loadingPanel.setVisible(false);
                isLoading = false;
                statusLabel.setText("已完成");
            }
        });
    }

    private void loadArchives() {
        loadArchivesAsync();
    }

    public boolean isArchiveDateInRange(String archiveDateString, Date startDate, Date endDate) {
        // 置日期格式，确保与输入日期字符串的格式一致
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss"); // 根据需要调整格式

        try {
            // 将字符串转换为日期对象
            Date archiveDate = dateFormat.parse(archiveDateString);

            // 返回比较结果
            return archiveDate.after(startDate) && archiveDate.before(endDate);
        } catch (ParseException e) {
            // 处理解析异常
            e.printStackTrace();
            return false; // 或根据要处理错误
        }
    }

    public void restoreArchive(String archiveId) {
        // 添加确认对话框
        int choice = Messages.showYesNoDialog(
            "确定要还原该存档吗?",
            "确认还原",
            Messages.getQuestionIcon()
        );
        
        if (choice == Messages.YES) {
            boolean success = archiveManager.restore(archiveId);
            if(success) {
               //成功不提示
            } else {
                Messages.showErrorDialog("还原失败,请重试", "错误");
            }
        }
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{};
    }

    public void hide() {
        PluginCache.archiveTab.setSelected(false);
        getWindow().setVisible(false);  // 只隐藏窗口，不销毁
    }

    public void show() {
        if (!isVisible()) {
            getWindow().setVisible(true);
            // 重新显示时刷新数据
            loadArchives();
        }
    }

    @Override
    protected void dispose() {
        super.dispose();
        PluginCache.archiveTab.setSelected(false);
    }

    // 添加一个方法检查窗口状态
    public boolean isValid() {
        return getWindow() != null && !isDisposed();
    }

    // 修改下载方法
    private void downloadArchive(Map<String, Object> archive) {
        // 检查VIP状态
        if (!PluginCache.isValidLicense) {
            Messages.showWarningDialog(
                "请先开通VIP,即可使用云端存储功能",
                "需要开通VIP"
            );
            return;
        }

        String storedUrl = (String)archive.get("stored_url");
        String time = (String)archive.get("time");
        String description = (String)archive.get("description");

        // 如果本地已存在该文件,弹出确认对话框
        if (!Boolean.TRUE.equals(archive.get("remote_only"))) {
            int choice = Messages.showYesNoDialog(
                "本地已存在该存档,是否覆盖?",
                "确认覆盖",
                Messages.getQuestionIcon()
            );
            if (choice != Messages.YES) {
                return;
            }
        }

        // 更新状态为下载中
        archive.put("downloadStatus", "downloading");
        table.repaint();

        // 构建本地保存路径
        String fileId = archiveManager.getFileId();
        String pluginDir = PathManager.getPluginsPath() + "/smart-flow-plugin/archives" + "/" + fileId;

        //检查pluginDir 目录
        File dir = new File(pluginDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String targetPath = pluginDir + "/" + time + ".xml";
        
        // 创建进度对话框
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "下载存档", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    URI uri = new URI(storedUrl);
                    URL url = uri.toURL();
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    int fileSize = conn.getContentLength();
                    

                    try (InputStream in = conn.getInputStream();
                         FileOutputStream out = new FileOutputStream(targetPath)) {
                        
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        int totalBytesRead = 0;
                        
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            
                            // 更新进度
                            if (fileSize > 0) {
                                indicator.setFraction((double) totalBytesRead / fileSize);
                            }
                            
                            if (indicator.isCanceled()) {
                                // 删除未完成的文件
                                new File(targetPath).delete();
                                // 重置下载状态
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    archive.remove("downloadStatus");
                                    table.repaint();
                                });
                                return;
                            }
                        }
                    }
                    
                    // 下载完成后更新状态和刷新列表
                    ApplicationManager.getApplication().invokeLater(() -> {
                        archive.put("downloadStatus", "completed");
                        table.repaint();

                        Notifications.Bus.notify(new Notification(
                            "Smart Flow Notifications",
                            "下载成功",
                            "远程存档已成功下载到本地",
                            NotificationType.INFORMATION
                        ));

                        //更新本地文件
                        try {
                            // 更新manifest
                            ArchiveEntry entry = new ArchiveEntry(
                                    fileId,
                                    targetPath,
                                    description,
                                    time,
                                    "1"
                            );
                            archiveManager.updateManifest(pluginDir , entry);
                            // 下载完成后刷新列表
                            loadArchives();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        archive.remove("downloadStatus");
                        table.repaint();
                        Messages.showErrorDialog("下载失败: " + e.getMessage(), "错误");
                    });
                }
            }
        });
    }

    // 修改操作列渲染器
    private class OperationColumnRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            Map<String, Object> archive = allArchives.get((currentPage - 1) * PAGE_SIZE + row);
            
            List<JButton> operations = new ArrayList<>();
            
            // 如果不是仅云端记录,添加还原按钮
            if (!Boolean.TRUE.equals(archive.get("remote_only"))) {
                JButton restoreBtn = createOperationButton("还原");
                panel.add(restoreBtn);
                operations.add(restoreBtn);
            }
            
            // 如果是云端记录且是VIP用户,添加下载按钮
            if ("2".equals(archive.get("type"))) {
                if (panel.getComponentCount() > 0) {
                    panel.add(Box.createHorizontalStrut(5));
                }
                JButton downloadBtn = createOperationButton("下载");
                if (!PluginCache.isValidLicense) {
                    downloadBtn.setEnabled(false);
                    downloadBtn.setToolTipText("开通VIP后可下载云端存档");
                }
                panel.add(downloadBtn);
                operations.add(downloadBtn);
            }
            
            // 如果是本地记录且是VIP用户,添加上传按钮
            if ("1".equals(archive.get("type"))) {
                if (panel.getComponentCount() > 0) {
                    panel.add(Box.createHorizontalStrut(5));
                }
                JButton uploadBtn = createOperationButton("上传");
                if (!PluginCache.isValidLicense) {
                    uploadBtn.setEnabled(false);
                    uploadBtn.setToolTipText("开通VIP后可上传到云端");
                }
                panel.add(uploadBtn);
                operations.add(uploadBtn);
            }
            
            table.putClientProperty("operations_" + row, operations);
            
            return panel;
        }
        
        private JButton createOperationButton(String text) {
            JButton button = new JButton(text);
            // 设置按钮样式
            button.setMargin(new Insets(1, 8, 1, 8));
            button.setFocusPainted(false);
            button.setFont(button.getFont().deriveFont(12f));
            return button;
        }
    }

    // 添加批量下载方法
    private void batchDownload() {
        // 检查VIP状态
        if (!PluginCache.isValidLicense) {
            Messages.showWarningDialog(
                "请先开通VIP,即可使用云端存储功能",
                "需要开通VIP"
            );
            return;
        }

        // 获取所有勾选的云端记录
        List<Map<String, Object>> checkedArchives = allArchives.stream()
            .filter(archive -> 
                "2".equals(archive.get("type")) && 
                Boolean.TRUE.equals(archive.get("checked")))
            .collect(Collectors.toList());

        if (checkedArchives.isEmpty()) {
            Messages.showInfoMessage("请先选择需要下载的云端存档", "提示");
            return;
        }

        // 创建进度对话框
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "批量下载存档", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                int total = checkedArchives.size();
                int current = 0;

                for (Map<String, Object> archive : checkedArchives) {
                    if (indicator.isCanceled()) break;

                    // 更新状态为下载中
                    ApplicationManager.getApplication().invokeLater(() -> {
                        archive.put("downloadStatus", "downloading");
                        table.repaint();
                    });
                    
                    String storedUrl = (String)archive.get("stored_url");
                    String time = (String)archive.get("time");
                    String description = (String)archive.get("description");

                    indicator.setText("正在下载: " + time);
                    indicator.setFraction((double) current / total);

                    try {
                        // 构建本地保存路径
                        String fileId = archiveManager.getFileId();
                        String pluginDir = PathManager.getPluginsPath() + "/smart-flow-plugin/archives/" + fileId;
                        
                        //检查目录
                        File dir = new File(pluginDir);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        String targetPath = pluginDir + "/" + time + ".xml";

                        // 下载文件
                        URI uri = new URI(storedUrl);
                        URL url = uri.toURL();
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        
                        try (InputStream in = conn.getInputStream();
                             FileOutputStream out = new FileOutputStream(targetPath)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }

                        // 更新manifest
                        ArchiveEntry entry = new ArchiveEntry(
                            fileId,
                            targetPath,
                            description,
                            time,
                            "1"
                        );
                        archiveManager.updateManifest(pluginDir, entry);

                        current++;

                        // 更新状态为完成
                        ApplicationManager.getApplication().invokeLater(() -> {
                            archive.put("downloadStatus", "completed");
                            table.repaint();
                        });

                    } catch (Exception e) {
                        System.out.println("Failed to download archive: " + time);
                        // 继续下载其他文件
                    }
                }

                int finalCurrent = current;
                ApplicationManager.getApplication().invokeLater(() -> {
                    loadArchives();
                    Notifications.Bus.notify(new Notification(
                        "Smart Flow Notifications",
                        "批量下载完成",
                        String.format("成功下载 %d/%d 个文件", finalCurrent, total),
                        finalCurrent == total ? NotificationType.INFORMATION : NotificationType.WARNING
                    ));
                });
            }
        });
    }

    private JComponent createLoadingPanel() {
        loadingPanel = new JPanel(new BorderLayout());
        JLabel loadingLabel = new JLabel("正在加载数据...", SwingConstants.CENTER);
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(14f));
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
        loadingPanel.setVisible(false);
        return loadingPanel;
    }

    // 添加上传方法
    private void uploadArchive(Map<String, Object> archive) {
        // 检查VIP状态
        if (!PluginCache.isValidLicense) {
            Messages.showWarningDialog(
                "请先开通VIP,即可使用云端存储功能",
                "需要开通VIP"
            );
            return;
        }

        // 更新状态为上传中
        archive.put("uploadStatus", "uploading");
        table.repaint();

        String filePath = (String) archive.get("id");
        String time = (String) archive.get("time");
        String description = (String) archive.get("description");
        String fileId = archiveManager.getFileId();

        // 在后台线程中执行上传
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                archiveManager.uploadToRemote(new File(filePath), fileId, time, description);
                
                // 上传成功后更新状态
                ApplicationManager.getApplication().invokeLater(() -> {
                    archive.put("uploadStatus", "completed");
                    archive.put("type", "2"); // 更新为云端类型
                    archive.remove("remote_only");
                    table.repaint();
                });
                
            } catch (Exception e) {
                // 上传失败时清除状态
                ApplicationManager.getApplication().invokeLater(() -> {
                    archive.remove("uploadStatus");
                    table.repaint();
                });
            }
        });
    }
} 