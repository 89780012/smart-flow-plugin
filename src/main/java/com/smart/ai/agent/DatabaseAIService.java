package com.smart.ai.agent;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.Messages;
import com.smart.cache.PluginCache;
import com.smart.settings.SmartPluginSettings;
import com.smart.settings.SmartPluginSettingsConfigurable;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.chain.Chain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.sql.*;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseAIService {

    private static ChatLanguageModel chatModel = null;
    // SQL生成提示模板
    private static final String SQL_GENERATE_PROMPT = """
            你是一个SQL专家，请根据以下数据库结构和问题生成对应的MySQL查询语句。
            
            数据库表结构：
            %s
            
            规则：
            1. 只返回SQL语句本身,不要包含任何其他内容
            2. 不要添加任何注释或说明
            3. 使用标准的MySQL语法
            4. 确保SQL的安全性和性能
            5. 只能使用上述提供的表和字段
            6. 返回格式示例: SELECT column FROM table WHERE condition;
            7. SQL需要格式化输出
            问题: %s
            """;

    // 结果解释提示模板
    private static final String ANSWER_GENERATE_PROMPT = """
            基于以下信息，请用自然语言回答用户的问题：
            
            原始问题: %s
            执行的SQL: %s
            查询结果: %s
            
            请提供清晰、准确的回答，并确保：
            1. 使用自然、友好的语言
            2. 直接回答用户的问题
            3. 如果结果为空，请说明可能的原因
            
            回答:
            """;

    public static void init() {

        try {
            // 加载默认Mysql 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL驱动加载失败", e);
        }
    }

    public static String askQuestion(String question) {
        try {
            String apiDomain =  SmartPluginSettings.API_DOMAIN2 + "/v1";;
            String licenseURL =  SmartPluginSettings.getInstance().getLicenseKey();
            String model = SmartPluginSettings.getInstance().getOpenAIModel();
            boolean enableOpenAI = SmartPluginSettings.getInstance().isEnableOpenAI();

            // 如果启用了OpenAI，则使用OpenAI的API
            if(PluginCache.isValidLicense && enableOpenAI && !SmartPluginSettings.getInstance().getOpenAIBaseUrl().isEmpty()){
                apiDomain = SmartPluginSettings.getInstance().getOpenAIBaseUrl() + "/v1";
                licenseURL =  SmartPluginSettings.getInstance().getOpenAIAuthKey();
                model = SmartPluginSettings.getInstance().getOpenAIModel();
                chatModel = OpenAiChatModel.builder()
                        .baseUrl(apiDomain)
                        .apiKey(licenseURL)
                        .modelName(model)
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(60))
                        .logRequests(true)
                        .logResponses(true)
                        .build();
            }else{
                chatModel = OpenAiChatModel.builder()
                        .baseUrl(apiDomain)
                        .apiKey(licenseURL + "@db")
                        .modelName(model)
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(60))
                        .logRequests(true)
                        .logResponses(true)
                        .build();
            }

            // 如果model为空，则给出提示
            if(StringUtils.isBlank(model)){
                SwingUtilities.invokeLater(() -> {
                    int result = Messages.showDialog(
                        "未配置AI模型，请先在设置中配置模型信息。",
                        "配置提示",
                        new String[]{"前往设置", "取消"},
                        0,
                        Messages.getWarningIcon()
                    );
                    
                    if (result == 0) {
                        // 打开设置界面
                        ShowSettingsUtil.getInstance().showSettingsDialog(
                            PluginCache.project,
                            SmartPluginSettingsConfigurable.class
                        );
                    }
                });
                throw new RuntimeException("未配置AI模型，请先在设置中配置模型信息。");
            }

            // 生成SQL查询
            String sqlQuery = generateSQLQuery(question);

            // 验证SQL
            // sqlQuery = validateAndCleanSql(sqlQuery);

            // 执行查询
            //String queryResult = executeQuery(sqlQuery);

            // 生成回答
            //String answer = generateAnswer(question, sqlQuery, queryResult);

            //return formatResponse(sqlQuery, sqlQuery);
            return sqlQuery;

        } catch (Exception e) {
            return "处理查询时发生错误: " + e.getMessage();
        }
    }

    // 新增获取数据库结构的方法
    private static String getDatabaseStructure() {
        StringBuilder structure = new StringBuilder();
        String dbUrl = SmartPluginSettings.getInstance().getMysqlUrl();
        String dbUsername = SmartPluginSettings.getInstance().getMysqlUsername();
        String dbPassword = SmartPluginSettings.getInstance().getMysqlPassword();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String[] types = {"TABLE"};
            String dbName = conn.getCatalog();

            try (ResultSet tables = metaData.getTables(dbName, null, "%", types)) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    structure.append("表名: ").append(tableName).append("\n");
                    structure.append("列信息:\n");

                    try (ResultSet columns = metaData.getColumns(dbName, null, tableName, "%")) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            String columnType = columns.getString("TYPE_NAME");
                            String comment = columns.getString("REMARKS");

                            structure.append("- ").append(columnName)
                                    .append(" (").append(columnType).append(")");
                            if (comment != null && !comment.isEmpty()) {
                                structure.append(" 说明: ").append(comment);
                            }
                            structure.append("\n");
                        }
                    }
                    structure.append("\n");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取数据库结构失败: " + e.getMessage());
        }
        return structure.toString();
    }


    private static String generateSQLQuery(String question) {
        try {
            // 获取数据库表结构信息
            if(StringUtils.isBlank(PluginCache.dbCache)){
                PluginCache.dbCache = getDatabaseStructure();
            }
            String prompt = String.format(SQL_GENERATE_PROMPT, PluginCache.dbCache, question);
            String response = chatModel.generate(prompt);

            // 添加日志
            System.out.println("AI Response: " + response);
            
            // 增加响应格式处理
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("AI返回内容为空");
            }

            // 提取SQL语句
            String sql = extractSqlFromResponse(response);
            
            // 验证提取的SQL
            if (sql == null || sql.trim().isEmpty()) {
                throw new RuntimeException("无法从AI响应中提取有效的SQL语句");
            }

            return sql;
        } catch (Exception e) {
            System.err.println("SQL生成异常: " + e.getMessage());
            System.err.println("异常详情: " + e);
            throw new RuntimeException("SQL生成失败: " + e.getMessage(), e);
        }
    }

    private static String validateAndCleanSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new RuntimeException("生成的SQL语句为空");
        }

        sql = sql.trim();

        // 安全检查
        String sqlLower = sql.toLowerCase();
        if (sqlLower.contains("drop") ||
                sqlLower.contains("delete") ||
                sqlLower.contains("update") ||
                sqlLower.contains("truncate") ||
                sqlLower.contains("alter") ||
                sqlLower.contains("create")) {
            throw new RuntimeException("不允许执行非查询操作");
        }

        if (!sqlLower.startsWith("select")) {
            throw new RuntimeException("只允许执行SELECT查询");
        }

        return sql;
    }

    private static String extractSqlFromResponse(String response) {
        // 尝试多种模式匹配SQL
        String sql = null;
        
        // 1. 标准SELECT语句匹配
        Pattern selectPattern = Pattern.compile("(?i)(SELECT\\s+.*?)(;|$)");
        Matcher selectMatcher = selectPattern.matcher(response);
        if (selectMatcher.find()) {
            sql = selectMatcher.group(1).trim();
        }
        
        // 2. 如果上面没匹配到,尝试提取```sql```块中的内容
        if (sql == null) {
            Pattern codeBlockPattern = Pattern.compile("```sql\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
            Matcher codeBlockMatcher = codeBlockPattern.matcher(response);
            if (codeBlockMatcher.find()) {
                sql = codeBlockMatcher.group(1).trim();
            }
        }
        
        // 3. 如果还是没有,直接使用整个响应
        if (sql == null) {
            sql = response.trim();
        }
        
        // 清理SQL语句
        if (sql != null) {
            // 移除多余的反引号
            sql = sql.replaceAll("```", "");
            // 移除sql关键字标记
            sql = sql.replaceAll("(?i)^sql\\s+", "");
            // 确保以分号结尾
            if (!sql.endsWith(";")) {
                sql = sql + ";";
            }
        }
        
        return sql;
    }

    private static String executeQuery(String sqlQuery) throws SQLException {
        StringBuilder result = new StringBuilder();
        String dbUrl = SmartPluginSettings.getInstance().getMysqlUrl();
        String dbUsername = SmartPluginSettings.getInstance().getMysqlUsername();
        String dbPassword = SmartPluginSettings.getInstance().getMysqlPassword();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sqlQuery)) {

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // 添加列头
                for (int i = 1; i <= columnCount; i++) {
                    result.append(metaData.getColumnName(i)).append("\t");
                }
                result.append("\n");

                // 添加数据行
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        result.append(rs.getString(i)).append("\t");
                    }
                    result.append("\n");
                }
            }
        }
        return result.toString();
    }

    private static String generateAnswer(String question, String sqlQuery, String queryResult) {
        try {
            String prompt = String.format(ANSWER_GENERATE_PROMPT, question, sqlQuery, queryResult);
            return chatModel.generate(prompt);
        } catch (Exception e) {
            throw new RuntimeException("生成回答失败: " + e.getMessage());
        }
    }

    private static String formatResponse(String sqlQuery, String answer) {
        return String.format("""
                执行的SQL查询：
                %s
                
                回答：
                %s
                """, sqlQuery, answer);
    }

}
