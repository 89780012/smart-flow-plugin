package com.smart.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.smart.bean.ChatMessage;
import com.smart.bean.Tool;
import com.smart.cache.PluginCache;
import com.smart.service.OpenAIService;
import com.smart.ui.message.MessageBubble;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;


public class AIPanel extends JPanel {
    private final Project project;
    private final VisualLayoutPanel visualLayoutPanel;
    public JPanel chatHistoryPanel;
    public JTextArea inputArea;
    private JPanel mainPanel;
    private JButton sendButton;
    public JScrollPane scrollPane;

    private StringBuilder currentResponse;

    private MessageBubble currentMessageBubble;
    private JPanel currentWrapperPanel;

    private JPanel quickActionsPanel;
    protected static final String[][] DEFAULT_QUICK_ACTIONS = {
            {"如何使用js请求接口", "如何使用js请求该流程接口, 请构造出示例"},
            {"如何使用curl请求接口", "如何使用curl请求该流程接口, 请构造出示例"},
            {"校验流程", "帮我检查当前流程。需要有开始节点;需要有结束节点;节点如果有两条连接线,只能有一条没有条件表达式;从开始到结束节点至少有一条通路;最后需要将问题点整理总结,尽量精简"},
            {"解释代码", "请解释一下当前流程,尽量精简"},
    };

    public OpenAIService openAIService;
    public String fileType;
    private MessageBubble loadingBubble;
    private JPanel loadingPanel;
    private JPanel inputPanel;
    private JPanel inputContainerPanel;
    private JPanel functionalButtonsPanel;

    private JComboBox<String> modeComboBox;
    private static final String[] MODES = {"默认模式"};

    public AIPanel(Project project, VisualLayoutPanel visualLayoutPanel, VirtualFile currentFile) {
        this.project = project;
        this.visualLayoutPanel = visualLayoutPanel;

        // 获取文件内容和类型
        String fileContent = "";
        fileType = "";
        if (currentFile != null) {
            fileType = currentFile.getExtension();
            try {
                fileContent = new String(currentFile.contentsToByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 构建系统提示词
        String systemPrompt = String.format(
                "你是一个专业的编程助手,精通各种编程语言和技术栈。" +
                        "请基于以下代码上下文来回答问题:\n\n" +
                        "文件类型: %s\n" +
                        "文件内容:\n" +
                        "```%s\n%s\n```\n\n" +
                        "请注意:\n" +
                        "1. 提供简洁专业的解答\n" +
                        "2. 如果是流程数据则从文件内容获取, 不要乱造数据",
                fileType,
                fileType,
                fileContent.isEmpty() ? "// 暂无代码" : fileContent
        );

        this.openAIService = new OpenAIService(systemPrompt);

        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));

        createUI();
        add(mainPanel, BorderLayout.CENTER);

        setupEventListeners();

    }
    public void welcome(){
        SwingUtilities.invokeLater(() -> {
            String welcomeMessage = String.format(
                    "👋 你好！我是智能编程助手，我已经了解了当前文件的内容。\n\n" +
                            "我可以帮你:\n" +
                            "1. 分析和优化当前代码\n" +
                            "2. 解答编程相关问题\n" +
                            "3. 提供代码示例和最佳实践\n" +
                            "4. 解决编码过程中的问题\n\n" +
                            "当前文件类型: %s\n" +
                            "你可以直接向我提问,或使用快捷操作按钮。",
                    fileType.isEmpty() ? "未知" : fileType.toUpperCase()
            );

            ChatMessage welcomeChatMessage = new ChatMessage(welcomeMessage, false);
            addMessageToUI(welcomeChatMessage);
        });
    }

    private void createUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(JBColor.background());

        // 1. 创建输入面板
        inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(JBColor.background());
        inputPanel.setBorder(JBUI.Borders.empty(8, 12));  // 只保留内边距

        // 2. 创建输入区域
        inputArea = new JTextArea();
        inputArea.setRows(3);  // 设置初始行数
        inputArea.setLineWrap(true);  // 启用自动换行
        inputArea.setWrapStyleWord(true);  // 按单词换行
        inputArea.setFont(JBUI.Fonts.label(12));
        inputArea.setBorder(JBUI.Borders.empty(6));  // 输入区域的内边距

        // 3. 创建滚动面板
        JScrollPane inputScrollPane = new JScrollPane(
                inputArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        inputScrollPane.setBorder(null);
        inputScrollPane.setViewportBorder(null);

        // 4. 创建包装面板并添加边框
        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setBackground(JBColor.background());
        scrollWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new JBColor(new Color(200, 200, 200), new Color(70, 70, 70)), 1),
                JBUI.Borders.empty(8)  // 内边距
        ));
        scrollWrapper.add(inputScrollPane, BorderLayout.CENTER);

        sendButton = new JButton(new ImageIcon(getClass().getResource("/icons/send.png")));
        sendButton.setPreferredSize(new Dimension(36,36));
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setFocusPainted(false);
        // 5. 组装输入面板
        inputPanel.add(scrollWrapper, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // 6. 创建快操作面板
        quickActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        quickActionsPanel.setBackground(JBColor.background());
        quickActionsPanel.setBorder(JBUI.Borders.empty(8, 12, 4, 12));

        // 使用 getQuickActions() 获取操作列表
        for (String[] action : getQuickActions()) {
            JButton actionButton = createQuickActionButton(action[0], action[1]);
            quickActionsPanel.add(actionButton);
        }

        // 7. 创建聊天历史面板
        chatHistoryPanel = new JPanel();
        chatHistoryPanel.setLayout(new BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS));
        chatHistoryPanel.setBackground(JBColor.background());
        chatHistoryPanel.setBorder(JBUI.Borders.empty(12, 6));
        chatHistoryPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        scrollPane = new JBScrollPane(chatHistoryPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(JBColor.background());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // 8. 创建分隔线
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new JBColor(new Color(230, 230, 230, 50), new Color(60, 60, 60, 50)));

        // 创建输入区域容器面板
        inputContainerPanel = new JPanel(new BorderLayout());
        inputContainerPanel.setBackground(JBColor.background());


        // 创建功能按钮面板
        functionalButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        functionalButtonsPanel.setBackground(JBColor.background());

        // 创建模式切换按钮和下拉框
        JButton modeButton = new JButton(new ImageIcon(getClass().getResource("/icons/model.png")));
        modeButton.setPreferredSize(new Dimension(28, 28));
        modeButton.setBorderPainted(false);
        modeButton.setContentAreaFilled(false);
        modeButton.setFocusPainted(false);
        modeButton.setToolTipText("切换模式");

        // 创建下拉框
        modeComboBox = new JComboBox<>(MODES);
        modeComboBox.setSelectedItem(PluginCache.currentModel);
        modeComboBox.setPreferredSize(new Dimension(120, 28));
        modeComboBox.setBackground(JBColor.background());
        modeComboBox.setForeground(JBColor.foreground());

        // 添加下拉框事件监听
        modeComboBox.addActionListener(e -> {
            String selectedMode = (String) modeComboBox.getSelectedItem();
            PluginCache.currentModel = selectedMode;
            modeButton.setToolTipText("当前模式: " + selectedMode);
        });

        // 创建内容面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(modeComboBox, BorderLayout.CENTER);
        contentPanel.setBorder(JBUI.Borders.empty(4));
        contentPanel.setBackground(JBColor.background());

        // 使用JBPopupFactory创建弹出层
        modeButton.addActionListener(e -> {
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(contentPanel, modeComboBox)
                .setRequestFocus(true)
                .setFocusable(true)
                .setResizable(false)
                .setMovable(false)
                .setTitle("选择模式")
                .createPopup()
                .show(new RelativePoint(modeButton, new Point(0, -contentPanel.getPreferredSize().height)));
        });

        // 添加清除消息按钮
        JButton clearButton = new JButton(new ImageIcon(getClass().getResource("/icons/clear-message.png")));
        clearButton.setPreferredSize(new Dimension(26, 26));
        clearButton.setBorderPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setFocusPainted(false);
        clearButton.setToolTipText("清除所有消息");
        clearButton.addActionListener(e -> {
            chatHistoryPanel.removeAll();
            welcome();
        });

        functionalButtonsPanel.add(clearButton);
        functionalButtonsPanel.add(modeButton);

        // 组装输入容器面板
        inputContainerPanel.add(inputPanel, BorderLayout.CENTER);
        inputContainerPanel.add(functionalButtonsPanel, BorderLayout.SOUTH);

        // 9. 创建底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 1));
        bottomPanel.setBackground(JBColor.background());
        bottomPanel.add(quickActionsPanel, BorderLayout.NORTH);
        bottomPanel.add(separator, BorderLayout.CENTER);
        bottomPanel.add(inputContainerPanel, BorderLayout.SOUTH);

        // 10. 组装主面板
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupEventListeners() {
        sendButton.addActionListener(e -> handleUserInput());

        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    handleUserInput();
                }
            }
        });
    }

    public void regisTool(Tool tool){
        openAIService.registerTool(tool);
    }


    private void handleUserInput() {
        if (!PluginCache.isValidLicense) {
            ChatMessage vipMessage = new ChatMessage("请开通VIP再尝试", false);
            addMessageToUI(vipMessage);
            return;
        }

        String userInput = inputArea.getText().trim();
        if (userInput.isEmpty()) {
            return;
        }

        ChatMessage userMessage = new ChatMessage(userInput, true);
        addMessageToUI(userMessage);
        inputArea.setText("");

        // 显示加载状态
        loadingBubble = new MessageBubble("正在思考中...", false);
        loadingPanel = createMessageWrapper(loadingBubble, false);
        chatHistoryPanel.add(loadingPanel);
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        currentResponse = new StringBuilder();
        currentMessageBubble = new MessageBubble("", false);
        currentWrapperPanel = createMessageWrapper(currentMessageBubble, false);

        // 在发送消息前调用钩子方法
        beforeSendMessage();

        // 创建UI更新计时器
        uiUpdateTimer = new Timer(UI_UPDATE_DELAY, e -> {
            updateUIWithBuffer();
        });
        uiUpdateTimer.start();

        openAIService.sendMessage(userInput, new OpenAIService.ChatCallback() {
            @Override
            public void onResponse(String response) {
                SwingUtilities.invokeLater(() -> {
                    // 收到第一个响应时移除加载状态
                    if (loadingPanel != null) {
                        chatHistoryPanel.remove(loadingPanel);
                        loadingPanel = null;
                        chatHistoryPanel.add(currentWrapperPanel);
                        chatHistoryPanel.revalidate();
                        chatHistoryPanel.repaint();
                    }
                    // 将响应添加到缓冲区
                    responseBuffer.append(response);
                });
            }

            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    // 移除加载状态
                    if (loadingPanel != null) {
                        chatHistoryPanel.remove(loadingPanel);
                        loadingPanel = null;
                    }
                    if (currentWrapperPanel != null) {
                        chatHistoryPanel.remove(currentWrapperPanel);
                    }
                    ChatMessage errorMessage = new ChatMessage("错误: " + error, false);
                    addMessageToUI(errorMessage);
                });

                // 停止计时器
                if (uiUpdateTimer != null) {
                    uiUpdateTimer.stop();
                }
            }
        });
    }

    public void addMessageToUI(ChatMessage message) {
        MessageBubble bubble = new MessageBubble(message.getContent(), message.isUser());
        JPanel wrapperPanel = createMessageWrapper(bubble, message.isUser());

        chatHistoryPanel.add(wrapperPanel);
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        // 自动滚动到底部
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public JPanel createMessageWrapper(Component messageComponent, boolean isUser) {
        JPanel wrapperPanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                // 限制最大高度为首选高度
                Dimension preferred = getPreferredSize();
                return new Dimension(super.getMaximumSize().width, preferred.height);
            }
        };
        wrapperPanel.setOpaque(false);
        wrapperPanel.setBorder(JBUI.Borders.empty(8, 0)); // 只保留上下间距
        wrapperPanel.add(messageComponent, BorderLayout.CENTER);
        // 设置对齐方式
        wrapperPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        return wrapperPanel;
    }

    public JButton createQuickActionButton(String text, String action) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制圆角背景
                if (getModel().isPressed()) {
                    g2.setColor(new JBColor(new Color(0, 122, 255, 30), new Color(88, 166, 255, 30)));
                } else if (getModel().isRollover()) {
                    g2.setColor(new JBColor(new Color(0, 122, 255, 20), new Color(88, 166, 255, 20)));
                } else {
                    g2.setColor(new JBColor(new Color(0, 122, 255, 10), new Color(88, 166, 255, 10)));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                // 绘制边框
                g2.setColor(new JBColor(new Color(0, 122, 255, 40), new Color(88, 166, 255, 40)));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                // 绘制文字
                FontMetrics fm = g2.getFontMetrics();
                Rectangle textRect = new Rectangle(0, 0, getWidth(), getHeight());
                String text = getText();
                g2.setColor(new JBColor(new Color(0, 122, 255), new Color(88, 166, 255)));
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, x, y);

                g2.dispose();
            }
        };

        // 设置按钮属性
        button.setFont(JBUI.Fonts.label(11));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(
                button.getFontMetrics(button.getFont()).stringWidth(text) + 24,
                26
        ));

        // 点击事件
        button.addActionListener(e -> {
            inputArea.setText(action);
            handleUserInput();
        });

        return button;
    }

    // 添加 protected 方法供子类更新系统提示词
    protected void updateSystemPrompt(String newPrompt) {
        if (openAIService != null) {
            openAIService.updateSystemPrompt(newPrompt);
        }
    }

    // 添加 protected 方法供子类在发送消息前更新上下文
    protected void beforeSendMessage() {
        // 默认空实现,子类可以重写此方法
    }

    // 添加 protected 方法获取快捷操作
    protected String[][] getQuickActions() {
        return DEFAULT_QUICK_ACTIONS;
    }

    // 新增:使用缓冲区更新UI
    private void updateUIWithBuffer() {
        if (responseBuffer.length() > 0) {
            currentResponse.append(responseBuffer);

            if (currentMessageBubble != null) {
                currentMessageBubble.updateContent(currentResponse.toString());

                // 仅在有新内容时滚动
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                if (vertical.getValue() + vertical.getVisibleAmount() >= vertical.getMaximum() - 10) {
                    SwingUtilities.invokeLater(() -> {
                        vertical.setValue(vertical.getMaximum());
                    });
                }
            }

            // 清空缓冲区
            responseBuffer.setLength(0);
        }
    }

    // 添加响应缓冲区和计时器
    private StringBuilder responseBuffer = new StringBuilder();
    private Timer uiUpdateTimer;
    private static final int UI_UPDATE_DELAY = 50; // 100ms更新一次UI

    private String getDefaultSystemPrompt() {
        return String.format(
                "你是一个专业的编程助手,精通各种编程语言和技术栈。" +
                        "请基于以下代码上下文来回答问题:\n\n" +
                        "文件类型: %s\n" +
                        "文件内容:\n" +
                        "```%s\n%s\n```\n\n" +
                        "请注意:\n" +
                        "1. 提供简洁专业的解答\n" +
                        "2. 给出可执行的代码示例\n" +
                        "3. 说明代码的关键逻辑\n" +
                        "4. 尽量精简",
                fileType,
                fileType,
                "// 暂无代码"
        );
    }
} 