package com.smart.ui.message;

import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageBubble extends JPanel {
    private final JTextPane textPane;
    private final boolean isUser;
    private static final int RADIUS = 10;
    private static final Color USER_BG = new JBColor(new Color(0, 122, 255, 20), new Color(88, 166, 255, 20));
    private static final Color AI_BG = new JBColor(new Color(240, 240, 240), new Color(50, 50, 50));
    private static final Color CODE_BG = new JBColor(new Color(245, 245, 245), new Color(43, 43, 43));
    private static final int AVATAR_SIZE = 32;
    private static final int AVATAR_GAP = 8;
    
    private static ImageIcon userAvatar;
    private static ImageIcon aiAvatar;
    
    private static final int MAX_CODE_HEIGHT = 200; // 代码块最大高度
    private static final int MIN_CODE_HEIGHT = 80;  // 代码块最小高度
     
    static {
        // 加载头像资源
        try {
            ImageIcon originalUserAvatar = new ImageIcon(MessageBubble.class.getResource("/icons/user-avatar.png"));
            ImageIcon originalAiAvatar = new ImageIcon(MessageBubble.class.getResource("/icons/ai-avatar.png"));
            
            // 缩放头像
            userAvatar = new ImageIcon(originalUserAvatar.getImage().getScaledInstance(
                AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH));
            aiAvatar = new ImageIcon(originalAiAvatar.getImage().getScaledInstance(
                AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public MessageBubble(String content, boolean isUser) {
        this.isUser = isUser;
        setLayout(new BorderLayout(0, AVATAR_GAP));
        setOpaque(false);
        
        // 创建头像和名称面板
        JPanel avatarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        avatarPanel.setOpaque(false);
        avatarPanel.setBorder(JBUI.Borders.empty(4, 0)); // 添加上下边距使整体垂直居中
        
        // 创建头像标签
        JLabel avatarLabel = new JLabel(isUser ? userAvatar : aiAvatar);
        avatarLabel.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER); // 垂直居中
        
        // 创建名称标签
        JLabel nameLabel = new JLabel(isUser ? "我" : "智能机器人");
        // 使用粗体字
        nameLabel.setFont(JBUI.Fonts.label(12).asBold());
        nameLabel.setVerticalAlignment(SwingConstants.CENTER); // 垂直居中
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        nameLabel.setForeground(new JBColor(
            new Color(60, 60, 60), // 深色模式下的颜色调深一些
            new Color(200, 200, 200) // 浅色模式下的颜色调亮一些
        ));
        
        // 创建一个包装面板来确保名称标签垂直居中
        JPanel nameLabelWrapper = new JPanel(new BorderLayout());
        nameLabelWrapper.setOpaque(false);
        nameLabelWrapper.add(nameLabel, BorderLayout.CENTER);
        
        // 添加头像和名称到面板
        avatarPanel.add(avatarLabel);
        avatarPanel.add(nameLabelWrapper);
        
        // 创建消息内容面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        textPane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true; // 确保宽度跟随父容器
            }
            
            @Override
            public Dimension getPreferredSize() {
                // 获取实际内容大小
                Dimension size = super.getPreferredSize();
                // 如果没有内容，保持最小高度
                if (getText().isEmpty()) {
                    size.height = getFontMetrics(getFont()).getHeight() + 16; // 16是内边距
                }
                return size;
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制圆角背景
                g2.setColor(isUser ? USER_BG : AI_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
                
                super.paintComponent(g);
                g2.dispose();
            }
        };
        
        // 设置文本自动换行
        textPane.setEditable(false);
        textPane.setBorder(JBUI.Borders.empty(8));
        textPane.setOpaque(false);
        ((StyledEditorKit)textPane.getEditorKit()).getViewFactory().create(textPane.getDocument().getDefaultRootElement()).setSize(0, 0);
        
        // 设置样���
        StyledDocument doc = textPane.getStyledDocument();
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        Style regular = doc.addStyle("regular", defaultStyle);
        StyleConstants.setFontFamily(regular, "Dialog");
        StyleConstants.setFontSize(regular, 12);
        
        Style codeStyle = doc.addStyle("code", regular);
        StyleConstants.setBackground(codeStyle, CODE_BG);
        StyleConstants.setFontFamily(codeStyle, "JetBrains Mono");
        StyleConstants.setFontSize(codeStyle, 12);
        
        contentPanel.add(textPane, BorderLayout.CENTER);
        
        // 使用 BorderLayout 添加组件
        add(avatarPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        
        updateContent(content);
    }
    
    private void addCodeBlock(StyledDocument doc, String code) throws BadLocationException {
        // 创建代码面板
        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new JBColor(new Color(200, 200, 200, 100), 
                                                     new Color(80, 80, 80, 100)), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        codePanel.setBackground(CODE_BG);
        
        // 创建代码文本区域
        JTextArea codeArea = new JTextArea(code);
        // 设置字体,使用支持中文的等宽字体
        Font codeFont = new Font("JetBrains Mono", Font.PLAIN, 12);
        // 如果 JetBrains Mono 不支持中文,则回退到其他字体
        if (!codeFont.canDisplay('中')) {  // 测试是否支持中文
            codeFont = new Font("Monospaced", Font.PLAIN, 12);  // 使用系统等宽字体
        }
        codeArea.setFont(codeFont);
        codeArea.setEditable(false);
        codeArea.setBackground(CODE_BG);
        codeArea.setForeground(textPane.getForeground());
        
        // 设置文本组件的编码
        codeArea.putClientProperty("charset", "UTF-8");
        // 确保正确处理换行
        codeArea.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        
        // 创建滚动面板
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // 计算代码块高度
        FontMetrics fm = codeArea.getFontMetrics(codeArea.getFont());
        int lineHeight = fm.getHeight();
        int lineCount = code.split("\n").length;
        int preferredHeight = Math.min(lineHeight * lineCount + 20, MAX_CODE_HEIGHT);
        
        // 创建头部面板（包含展开/折叠按钮和复制按钮）
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        // 创建右侧按钮面板
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonsPanel.setOpaque(false);
        
        // 创建复制按钮
        JButton copyButton = new JButton("复制") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getForeground());
                
                // 绘制复制图标
                int size = 10;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                // 绘制重叠的矩形表示复制
                g2.drawRect(x, y, size-3, size-3);
                g2.drawRect(x+3, y-3, size-3, size-3);
                
                g2.dispose();
            }
        };
        
        copyButton.setPreferredSize(new Dimension(40, 20));
        copyButton.setBorderPainted(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setFocusPainted(false);
        copyButton.setForeground(new JBColor(new Color(120, 120, 120), new Color(180, 180, 180)));
        
        // 添加复制按钮事件
        copyButton.addActionListener(e -> {
            try {
                // 获取系统剪贴板
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Clipboard clipboard = toolkit.getSystemClipboard();
                
                // 将代码文本复制到剪贴板
                StringSelection strSel = new StringSelection(code);
                clipboard.setContents(strSel, null);
                
                // 提示复制成功，使用气泡通知
                // Notifications.Bus.notify(new Notification(
                //     "Smart Flow Notifications",
                //     "复制成功",
                //     "内容已复制到剪贴板",
                //     NotificationType.INFORMATION
                // ));

                SwingUtilities.invokeLater(() -> {
                    JBPopupFactory.getInstance()
                        .createHtmlTextBalloonBuilder("已复制到剪贴板", MessageType.INFO, null)
                        .setFadeoutTime(1500) // 1.5秒后消失
                        .setAnimationCycle(200) // 动画时长
                        .createBalloon()
                        .show(RelativePoint.getSouthOf(copyButton), Balloon.Position.below);
                });

                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 创建展开/折叠按钮
        JButton toggleButton = new JButton("展开") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getForeground());

                int size = 8;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                if (getText().equals("展开")) {
                    g2.drawLine(x, y + size/2, x + size/2, y);
                    g2.drawLine(x + size/2, y, x + size, y + size/2);
                } else {
                    g2.drawLine(x, y, x + size/2, y + size/2);
                    g2.drawLine(x + size/2, y + size/2, x + size, y);
                }

                g2.dispose();
            }
        };

        toggleButton.setPreferredSize(new Dimension(40, 20));
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setForeground(new JBColor(new Color(120, 120, 120), new Color(180, 180, 180)));

        boolean needsToggle = lineCount * lineHeight > MAX_CODE_HEIGHT;
        toggleButton.setVisible(needsToggle);

        // 设置初始高度
        scrollPane.setPreferredSize(new Dimension(100, needsToggle ? MIN_CODE_HEIGHT : preferredHeight));

        // 添加按钮事件
        toggleButton.addActionListener(e -> {
            int newHeight = toggleButton.getText().equals("展开") ? MAX_CODE_HEIGHT : MIN_CODE_HEIGHT;
            toggleButton.setText(toggleButton.getText().equals("展开") ? "收起" : "展开");
            scrollPane.setPreferredSize(new Dimension(100, newHeight));
            revalidate();
            repaint();
        });
        
        // 添加展开/折叠按钮到按钮面板
        buttonsPanel.add(copyButton);
        buttonsPanel.add(toggleButton);
        
        // 将按钮面板添加到头部面板
        headerPanel.add(buttonsPanel, BorderLayout.EAST);

        // 组装代码面板
        codePanel.add(headerPanel, BorderLayout.NORTH);
        codePanel.add(scrollPane, BorderLayout.CENTER);
        
        // 将代码面板插入到文档中
        doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
        textPane.insertComponent(codePanel);
        doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
    }
    
    public void updateContent(String content) {
        try {
            StyledDocument doc = textPane.getStyledDocument();
            doc.remove(0, doc.getLength());
            
            // 创建基本样式
            Style defaultStyle = doc.getStyle("regular");
            Style boldStyle = doc.addStyle("bold", defaultStyle);
            StyleConstants.setBold(boldStyle, true);
            
            Style italicStyle = doc.addStyle("italic", defaultStyle);
            StyleConstants.setItalic(italicStyle, true);
            
            Style linkStyle = doc.addStyle("link", defaultStyle);
            StyleConstants.setForeground(linkStyle, new JBColor(new Color(0, 122, 255), new Color(88, 166, 255)));
            StyleConstants.setUnderline(linkStyle, true);
            
            // 处理 Markdown 语法
            int lastEnd = 0;
            
            // 处理代码块 ```
            Pattern codePattern = Pattern.compile("```(.*?)```", Pattern.DOTALL);
            Matcher codeMatcher = codePattern.matcher(content);
            
            while (codeMatcher.find()) {
                // 处理代码块之前的文本
                processMarkdownText(doc, content.substring(lastEnd, codeMatcher.start()));
                
                // 使用新的代码块处理方法
                String code = codeMatcher.group(1).trim();
                addCodeBlock(doc, code);
                
                lastEnd = codeMatcher.end();
            }
            
            // 处理剩余文本
            if (lastEnd < content.length()) {
                processMarkdownText(doc, content.substring(lastEnd));
            }
            
            // 更新布局
            invalidate();
            revalidate();
            repaint();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void processMarkdownText(StyledDocument doc, String text) throws BadLocationException {
        // 处理行内代码
        Pattern inlineCodePattern = Pattern.compile("`([^`]+)`");
        // 处理粗体
        Pattern boldPattern = Pattern.compile("\\*\\*([^*]+)\\*\\*");
        // 处理斜体
        Pattern italicPattern = Pattern.compile("\\*([^*]+)\\*");
        // 处理链接
        Pattern linkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
        
        int lastEnd = 0;
        String remaining = text;
        
        while (!remaining.isEmpty()) {
            // 查找最近的 Markdown 标记
            int nextMark = Integer.MAX_VALUE;
            String markType = null;
            
            Matcher codeMatcher = inlineCodePattern.matcher(remaining);
            if (codeMatcher.find() && codeMatcher.start() < nextMark) {
                nextMark = codeMatcher.start();
                markType = "code";
            }
            
            Matcher boldMatcher = boldPattern.matcher(remaining);
            if (boldMatcher.find() && boldMatcher.start() < nextMark) {
                nextMark = boldMatcher.start();
                markType = "bold";
            }
            
            Matcher italicMatcher = italicPattern.matcher(remaining);
            if (italicMatcher.find() && italicMatcher.start() < nextMark) {
                nextMark = italicMatcher.start();
                markType = "italic";
            }
            
            Matcher linkMatcher = linkPattern.matcher(remaining);
            if (linkMatcher.find() && linkMatcher.start() < nextMark) {
                nextMark = linkMatcher.start();
                markType = "link";
            }
            
            if (markType == null) {
                // 没有找到标记，添加剩余文本
                doc.insertString(doc.getLength(), remaining, doc.getStyle("regular"));
                break;
            }
            
            // 添加标记之前的普通文本
            if (nextMark > 0) {
                doc.insertString(doc.getLength(), remaining.substring(0, nextMark), doc.getStyle("regular"));
            }
            
            // 处理对应的标记
            switch (markType) {
                case "code":
                    Matcher m1 = inlineCodePattern.matcher(remaining);
                    m1.find();
                    doc.insertString(doc.getLength(), m1.group(1), doc.getStyle("code"));
                    remaining = remaining.substring(m1.end());
                    break;
                case "bold":
                    Matcher m2 = boldPattern.matcher(remaining);
                    m2.find();
                    doc.insertString(doc.getLength(), m2.group(1), doc.getStyle("bold"));
                    remaining = remaining.substring(m2.end());
                    break;
                case "italic":
                    Matcher m3 = italicPattern.matcher(remaining);
                    m3.find();
                    doc.insertString(doc.getLength(), m3.group(1), doc.getStyle("italic"));
                    remaining = remaining.substring(m3.end());
                    break;
                case "link":
                    Matcher m4 = linkPattern.matcher(remaining);
                    m4.find();
                    doc.insertString(doc.getLength(), m4.group(1), doc.getStyle("link"));
                    remaining = remaining.substring(m4.end());
                    break;
            }
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        Container parent = getParent();
        if (parent != null) {
            // 获取父容器宽度并减去边距
            int maxWidth = parent.getWidth() - 20;
            // 考虑头像和间距,计算文本区域最大宽度
            int contentMaxWidth = maxWidth - 16; // 只减去内边距
            
            if (size.width > maxWidth) {
                // 设置文本面板宽度以触发换行
                textPane.setSize(contentMaxWidth, 10);  // 设置一个较小的初始高度
                
                // 获取实际需要的高度
                int preferredHeight = textPane.getPreferredSize().height;
                textPane.setSize(contentMaxWidth, preferredHeight);
                
                // 新计算整体大小
                size = super.getPreferredSize();
                size.width = maxWidth;
            }
        }
        return size;
    }
}