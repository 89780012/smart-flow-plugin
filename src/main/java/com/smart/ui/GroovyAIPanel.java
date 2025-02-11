package com.smart.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.smart.bean.ChatMessage;
import com.smart.service.OpenAIService;

import javax.swing.*;

public class GroovyAIPanel extends AIPanel {
    private Document editorDocument;
    private static final String GROOVY_SYSTEM_PROMPT_TEMPLATE = 
        "你是一个Groovy脚本专家，可以帮助用户编写和优化Groovy代码。" +
        "请注意以下几点：\n" +
        "1. 提供的代码应该简洁、易读且高效\n" +
        "2. 代码中可以使用内置变量 $vars.get(key) 访问上下文信息\n" +
        "3. 代码中可以使用内置变量 $vars.put(key, value) 设置上下文信息\n" +
        "4. 尽量提供详细的代码注释和使用说明\n\n" +
        "当前正在编辑的Groovy代码如下：\n" +
        "```groovy\n%s\n```\n" +
        "请基于以上代码上下文来回答用户的问题。";

    private static final String[][] GROOVY_QUICK_ACTIONS = {
        {"优化Groovy", "帮我优化当前Groovy代码, 缺陷部分则修复"},
        {"解释Groovy", "请解释这段Groovy代码的功能"},
        {"添加注释", "请为这段Groovy代码添加详细注释"},
    };

    public GroovyAIPanel(Project project, Document editorDocument) {
        super(project, null,null);
        this.editorDocument = editorDocument;
        
        // 获取编辑器内容并构建完整的系统提示词
        String editorContent = editorDocument.getText().trim();
        String systemPrompt = String.format(
            GROOVY_SYSTEM_PROMPT_TEMPLATE,
            editorContent.isEmpty() ? "// 暂无代码" : editorContent
        );
        
        // 设置带有代码上下文的系统提示词
        this.openAIService = new OpenAIService(systemPrompt);

    }

    public void welcome(){
        // 添加欢迎消息
        SwingUtilities.invokeLater(() -> {
            String welcomeMessage = "👋 你好！我是Groovy AI助手，我可以帮你：\n\n" +
                    "1. 分析和优化当前Groovy代码\n" +
                    "2. 解答Groovy相关的问题\n" +
                    "3. 提供代码示例和最佳实践\n" +
                    "4. 帮助处理编码过程中遇到的问题\n\n" +
                    "我已经了解了你当前正在编辑的代码内容，你可以直接向我提问。";

            ChatMessage welcomeChatMessage = new ChatMessage(welcomeMessage, false);
            addMessageToUI(welcomeChatMessage);
        });
    }

    
    // 重写发送消息前的钩子方法
    @Override
    protected void beforeSendMessage() {
        // 获取最新的编辑器内容
        String editorContent = editorDocument.getText().trim();
        
        // 构建新的系统提示词
        String newSystemPrompt = String.format(
            GROOVY_SYSTEM_PROMPT_TEMPLATE,
            editorContent.isEmpty() ? "// 暂无代码" : editorContent
        );
        
        // 更新系统提示词
        updateSystemPrompt(newSystemPrompt);
    }

    @Override
    protected String[][] getQuickActions() {
        return GROOVY_QUICK_ACTIONS;
    }
}