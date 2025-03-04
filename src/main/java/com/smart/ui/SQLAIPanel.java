package com.smart.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.smart.bean.ChatMessage;
import com.smart.service.OpenAIService;
import javax.swing.*;


public class SQLAIPanel extends AIPanel {
    private Document editorDocument;

    private static final String SQL_SYSTEM_PROMPT_TEMPLATE_BASIC =
            "你是一个数据库专家，专注于MyBatis框架，能够帮助用户编写和优化MyBatis Mapper中的增删改查（CRUD）语句。\n\n" +
                    "当前正在编辑的SQL代码如下：\n" +
                    "```sql\n%s\n```\n\n" +
                    "请注意以下几点：\n" +
                    "1. 提供的SQL应该简洁、高效且易于维护\n" +
                    "2. 支持标准SQL语法，并适用于MyBatis\n" +
                    "3. 回复尽量简洁，不要出现多余的话\n" +
                    "4. 请根据用户的需求，生成对应的MyBatis Mapper XML形式的CRUD语句\n\n" +
                    "请基于已写的SQL代码来回答用户的问题。";

    private static final String[][] SQL_QUICK_ACTIONS = {
            {"优化SQL", "帮我优化当前SQL查询，并提供性能建议"},
            {"解释SQL", "请解释这段SQL的执行过程"},
            {"添加注释", "请为这段SQL添加详细注释"},
            {"转换格式", "将SQL转换为更易读的格式"},
            {"MyBatis用法", "如何在MyBatis的Mapper中使用这段SQL，并给出XML的示例"}
    };


    public SQLAIPanel(Project project, Document editorDocument) {
        super(project, null, null);
        this.editorDocument = editorDocument;
        
        String editorContent = editorDocument.getText().trim();
        String systemPrompt;

        systemPrompt = String.format(
                SQL_SYSTEM_PROMPT_TEMPLATE_BASIC,
                editorContent.isEmpty() ? "-- 暂无SQL代码" : editorContent
        );
        
        this.openAIService = new OpenAIService(systemPrompt);
    }

    

    public void welcome() {
        // 添加欢迎消息
        SwingUtilities.invokeLater(() -> {
            String welcomeMessage = "👋 你好！我是SQL AI助手，我可以帮你：\n\n" +
                    "1. 编写和优化SQL查询语句\n" +
                    "2. 分析SQL在MyBatis的xml中最佳使用方法\n" +
                    "3. 解答SQL相关问题\n\n" +
                    "我已经了解了你当前正在编辑的SQL内容，你可以直接向我提问。";

            ChatMessage welcomeChatMessage = new ChatMessage(welcomeMessage, false);
            addMessageToUI(welcomeChatMessage);
        });
    }

    // 重写发送消息前的钩子方法
    @Override
    protected void beforeSendMessage() {
        String editorContent = editorDocument.getText().trim();
        String newSystemPrompt;

        newSystemPrompt = String.format(
                SQL_SYSTEM_PROMPT_TEMPLATE_BASIC,
                editorContent.isEmpty() ? "-- 暂无SQL代码" : editorContent
        );
        
        updateSystemPrompt(newSystemPrompt);
    }

    @Override
    protected String[][] getQuickActions() {
        return SQL_QUICK_ACTIONS;
    }


} 