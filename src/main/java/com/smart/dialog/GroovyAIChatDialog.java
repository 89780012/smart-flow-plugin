package com.smart.dialog;

import com.smart.service.OpenAIService;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GroovyAIChatDialog extends JDialog {
    private JTextPane chatArea;
    private JTextArea inputArea;
    private String groovyCode;
    private Window owner;
    private Parser markdownParser = Parser.builder().build();
    private HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    private CodeOutputCallback codeOutputCallback;
    private CommonDialog parentDialog;

    private OpenAIService openAIService;

    public GroovyAIChatDialog(Window owner, String groovyCode, CodeOutputCallback callback,CommonDialog parentDialog) {
        super(owner);
        this.owner = owner;
        this.groovyCode = groovyCode;
        this.codeOutputCallback = callback;
        this.parentDialog = parentDialog;

        this.openAIService = new OpenAIService(
                "你是一个智能编程助手,可以帮助用户解决编程相关问题。请用简洁专业的方式回答问题。"
        );

        setTitle("AI助手");

        setModal(false);
        createUI();
        updateLocation();

        owner.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentMoved(java.awt.event.ComponentEvent e) {
                updateLocation();
            }
        });
    }

    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(250, 250, 250));
        chatArea.setFont(new Font("Dialog", Font.PLAIN, 13));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setPreferredSize(new Dimension(400, 400));
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        inputArea = new JTextArea(3, 20);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(new Font("Dialog", Font.PLAIN, 13));

        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });

        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        bottomPanel.add(inputScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton sendButton = new JButton("发送消息");
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setSize(450, 600);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void appendAssistantMessage(String message, SimpleAttributeSet style) {
        try {
            javax.swing.text.Document doc = chatArea.getDocument();
            String[] lines = message.split("\n");
            StringBuilder currentBlock = new StringBuilder();
            StringBuilder codeBlock = new StringBuilder();
            boolean inCodeBlock = false;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (line.trim().startsWith("```")) {
                    if (currentBlock.length() > 0) {
                        appendFormattedText(doc, currentBlock.toString(), style);
                        currentBlock.setLength(0);
                    }

                    if (inCodeBlock) {
                        inCodeBlock = false;
                        if (codeBlock.length() > 0) {
                            String code = codeBlock.toString().trim();
                            appendCodeBlock(doc, code);
                        }
                        codeBlock.setLength(0);
                    } else {
                        inCodeBlock = true;
                    }
                    continue;
                }

                if (inCodeBlock) {
                    codeBlock.append(line).append("\n");
                } else {
                    currentBlock.append(line).append("\n");
                }
            }

            if (currentBlock.length() > 0) {
                appendFormattedText(doc, currentBlock.toString(), style);
            }

            chatArea.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendUserMessage(String message) {
        try {
            javax.swing.text.Document doc = chatArea.getDocument();
            SimpleAttributeSet userStyle = new SimpleAttributeSet();
            StyleConstants.setAlignment(userStyle, StyleConstants.ALIGN_RIGHT);
            StyleConstants.setBackground(userStyle, new Color(227, 242, 253));
            StyleConstants.setForeground(userStyle, Color.BLACK);

            if (doc.getLength() > 0) {
                doc.insertString(doc.getLength(), "\n", null);
            }

            doc.insertString(doc.getLength(), message + "\n", userStyle);
            ((StyledDocument)doc).setParagraphAttributes(doc.getLength() - message.length() - 1,
                    message.length() + 1, userStyle, false);

            chatArea.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void appendFormattedText(javax.swing.text.Document doc, String text, SimpleAttributeSet baseStyle)
            throws Exception {
        // 解析Markdown
        Node document = markdownParser.parse(text);
        String html = htmlRenderer.render(document);

        // 创建不同样式
        SimpleAttributeSet boldStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setBold(boldStyle, true);

        SimpleAttributeSet italicStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setItalic(italicStyle, true);

        // 处理基本Markdown语法
        String[] lines = html.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            // 移除HTML标签，保留基本格式
            line = line.replaceAll("<[/]?p>", "")
                    .replaceAll("<[/]?em>", "*")
                    .replaceAll("<[/]?strong>", "**")
                    .trim();

            if (line.contains("**")) {
                // 处理粗体
                String[] parts = line.split("\\*\\*");
                for (int i = 0; i < parts.length; i++) {
                    if (i % 2 == 0) {
                        doc.insertString(doc.getLength(), parts[i], baseStyle);
                    } else {
                        doc.insertString(doc.getLength(), parts[i], boldStyle);
                    }
                }
            } else if (line.contains("*")) {
                // 处理斜体
                String[] parts = line.split("\\*");
                for (int i = 0; i < parts.length; i++) {
                    if (i % 2 == 0) {
                        doc.insertString(doc.getLength(), parts[i], baseStyle);
                    } else {
                        doc.insertString(doc.getLength(), parts[i], italicStyle);
                    }
                }
            } else {
                doc.insertString(doc.getLength(), line, baseStyle);
            }
            doc.insertString(doc.getLength(), "\n", baseStyle);
        }
    }

    private void appendCodeBlock(javax.swing.text.Document doc, String code) throws Exception {
        SimpleAttributeSet codeStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(codeStyle, "Monospaced");
        StyleConstants.setFontSize(codeStyle, 12);
        StyleConstants.setBackground(codeStyle, new Color(246, 248, 250));
        StyleConstants.setForeground(codeStyle, new Color(36, 41, 47));

        doc.insertString(doc.getLength(), "\n", null);
        doc.insertString(doc.getLength(), code + "\n", codeStyle);
        doc.insertString(doc.getLength(), "\n", null);
    }

    private void updateLocation() {
        Point ownerLoc = owner.getLocation();
        Dimension ownerSize = owner.getSize();
        setLocation(ownerLoc.x + ownerSize.width, ownerLoc.y);
    }

    private void sendMessage() {
        String message = inputArea.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        appendUserMessage(message);
        inputArea.setText("");

        groovyCode = this.parentDialog.getCode();

        String prompt = "当前Groovy代码上下文:\n```groovy\n" + groovyCode + "\n```\n\n用户问题:\n" + message + "\n 请确保使用markdown格式返回";

        try {
            SimpleAttributeSet assistantStyle = new SimpleAttributeSet();
            StyleConstants.setAlignment(assistantStyle, StyleConstants.ALIGN_LEFT);
            StyleConstants.setBackground(assistantStyle, new Color(245, 245, 245));
            StyleConstants.setForeground(assistantStyle, Color.BLACK);

            int responseStartPosition = chatArea.getDocument().getLength();
            StringBuilder responseBuffer = new StringBuilder();

            openAIService.sendMessage(prompt, new OpenAIService.ChatCallback() {
                @Override
                public void onResponse(String response) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            responseBuffer.append(response);

                            if (chatArea.getDocument().getLength() > responseStartPosition) {
                                chatArea.getDocument().remove(responseStartPosition,
                                        chatArea.getDocument().getLength() - responseStartPosition);
                            }

                            appendAssistantMessage(responseBuffer.toString(), assistantStyle);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // 在UI上显示错误
                            try {
                                appendFormattedText(chatArea.getDocument(), 
                                    "错误: 处理响应时发生异常 - " + e.getMessage() + "\n", 
                                    assistantStyle);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            String errorMessage = "错误: " + error;
                            if (error.contains("IllegalStateException")) {
                                errorMessage += "\n可能是API响应格式不正确,请稍后重试";
                            }
                            appendFormattedText(chatArea.getDocument(), errorMessage + "\n", assistantStyle);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(GroovyAIChatDialog.this,
                                "显示错误信息时发生异常: " + e.getMessage(),
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateGroovyCode(String code) {
        this.groovyCode = code;
    }

    // 添加回调接口定义
    public interface CodeOutputCallback {
        void onCodeOutput(String code);
    }
}