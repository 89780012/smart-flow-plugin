package com.smart.utils;

import com.smart.window.BizFileInfo;
import com.smart.enums.DataType;
import com.smart.enums.RequireType;

import java.util.List;
import java.util.ArrayList;

public class BizMarkdownGenerator {
    
    // 生成单个接口的markdown文档
    public static String generateMarkdown(BizFileInfo bizFileInfo) {
        StringBuilder md = new StringBuilder();
        
        // 添加接口标题
        //md.append("# ").append(bizFileInfo.getName()).append("\n\n");
        
        // 添加接口基本信息
        md.append("#### 基本信息\n\n");
        md.append("- **接口URL：** `").append(bizFileInfo.getUrl()).append("`\n");
        md.append("- **请求方式：** `").append(bizFileInfo.getMethod()).append("`\n");
        md.append("- **Content-Type：** `").append(bizFileInfo.getProtocol()).append("`\n\n");
        
        // 添加请求参数
        md.append("#### 请求参数\n\n");
        
        // Query参数
        List<BizFileInfo.ParamInfo> queryParams = bizFileInfo.getParams().getQueryParams();
        if (!queryParams.isEmpty()) {
            md.append("##### Query参数\n\n");
            md.append("| 参数名 | 类型 | 是否必填 | 默认值 | 说明 |\n");
            md.append("|--------|------|----------|--------|------|\n");
            
            for (BizFileInfo.ParamInfo param : queryParams) {
                md.append("| ")
                  .append(param.getName()).append(" | ")
                  .append(param.getType().getDisplayName()).append(" | ")
                  .append(param.getRequired().getDisplayName()).append(" | ")
                  .append(param.getDefaultValue().isEmpty() ? "-" : param.getDefaultValue()).append(" | ")
                  .append(param.getDescription() == null ? "-" : param.getDescription())
                  .append(" |\n");
            }
            md.append("\n");
        }
        
        // Body参数
        List<BizFileInfo.ParamInfo> bodyParams = bizFileInfo.getParams().getBodyParams();
        if (!bodyParams.isEmpty()) {
            md.append("##### Body参数\n\n");
            md.append("| 参数名 | 类型 | 是否必填 | 默认值 | 说明 |\n");
            md.append("|--------|------|----------|--------|------|\n");
            
            for (BizFileInfo.ParamInfo param : bodyParams) {
                md.append("| ")
                  .append(param.getName()).append(" | ")
                  .append(param.getType().getDisplayName()).append(" | ")
                  .append(param.getRequired().getDisplayName()).append(" | ")
                  .append(param.getDefaultValue().isEmpty() ? "-" : param.getDefaultValue()).append(" | ")
                  .append(param.getDescription() == null ? "-" : param.getDescription())
                  .append(" |\n");
            }
            md.append("\n");
        }
        
        // JSON参数
        BizFileInfo.JsonParams jsonParams = bizFileInfo.getParams().getJsonParams();
        if (jsonParams != null && jsonParams.getContent() != null && !jsonParams.getContent().isEmpty()) {
            md.append("##### JSON参数\n\n");
            md.append("```json\n");
            md.append(jsonParams.getContent());
            md.append("\n```\n\n");
        }
        
        // 添加返回值说明
        md.append("#### 返回值说明\n\n");
        
        String responseStruct = bizFileInfo.getResults().getResponseStruct();
        List<BizFileInfo.Results.ResultInfo> results = bizFileInfo.getResults().getResult();
        
        if ("1".equals(responseStruct)) {
            // 标准返回结构
            md.append("##### 返回结构\n\n");
            md.append("```json\n");
            md.append("{\n");
            md.append("    \"code\": \"返回码\",\n");
            md.append("    \"message\": \"返回信息\",\n");
            md.append("    \"data\": {\n");
            if (!results.isEmpty()) {
                md.append("        // 详细字段见下方返回字段说明\n");
            }
            md.append("    }\n");
            md.append("}\n");
            md.append("```\n\n");
            
            if (!results.isEmpty()) {
                md.append("##### data字段说明\n\n");
                md.append("| 字段名 | 类型 | 层级 | 说明 | 示例值 |\n");
                md.append("|--------|------|------|------|--------|\n");
                
                for (BizFileInfo.Results.ResultInfo result : results) {
                    md.append("| ")
                      .append(result.getName()).append(" | ")
                      .append(result.getType().getDisplayName()).append(" | ")
                      .append(result.getStepType().getDisplayName()).append(" | ")
                      .append(result.getDescription() == null ? "-" : result.getDescription()).append(" | ")
                      .append(result.getExample() == null || result.getExample().isEmpty() ? "-" : result.getExample())
                      .append(" |\n");
                }
                md.append("\n");
            }
        } else if ("2".equals(responseStruct)) {
            // 直接返回结构
            if (!results.isEmpty()) {
                md.append("##### 返回字段说明\n\n");
                md.append("| 字段名 | 类型 | 层级 | 说明 | 示例值 |\n");
                md.append("|--------|------|------|------|--------|\n");
                
                for (BizFileInfo.Results.ResultInfo result : results) {
                    md.append("| ")
                      .append(result.getName()).append(" | ")
                      .append(result.getType().getDisplayName()).append(" | ")
                      .append(result.getStepType().getDisplayName()).append(" | ")
                      .append(result.getDescription() == null ? "-" : result.getDescription()).append(" | ")
                      .append(result.getExample() == null || result.getExample().isEmpty() ? "-" : result.getExample())
                      .append(" |\n");
                }
                md.append("\n");
                
                // 添加示例返回结构
                md.append("##### 返回示例\n\n");
                md.append("```json\n");
                md.append("{\n");
                // 生成示例返回结构
                generateExampleResponse(results, md, 1);
                md.append("}\n");
                md.append("```\n\n");
            }
        }
        
        return md.toString();
    }
    
    private static void generateExampleResponse(List<BizFileInfo.Results.ResultInfo> results,
                                              StringBuilder md, 
                                              int indent) {
        String indentStr = "    ".repeat(indent);
        
        for (BizFileInfo.Results.ResultInfo result : results) {
            md.append(indentStr).append("\"").append(result.getName()).append("\": ");
            
            // 根据字段类型生成示例值
            if (result.getExample() != null && !result.getExample().isEmpty()) {
                // 如果有示例值，直接使用
                if (result.getType() == DataType.STRING) {
                    md.append("\"").append(result.getExample()).append("\"");
                } else {
                    md.append(result.getExample());
                }
            } else {
                // 没有示例值，根据类型生成默认值
                switch (result.getType()) {
                    case STRING:
                        md.append("\"string\"");
                        break;
                    case INTEGER:
                    case LONG:
                        md.append("0");
                        break;
                    case FLOAT:
                    case DOUBLE:
                        md.append("0.0");
                        break;
                    case BOOLEAN:
                        md.append("false");
                        break;
                    case DATE:
                        md.append("\"2024-01-01 00:00:00\"");
                        break;
                    case ARRAY:
                        md.append("[]");
                        break;
                    case OBJECT:
                        md.append("{}");
                        break;
                    default:
                        md.append("null");
                }
            }
            md.append(",\n");
        }
        
        // 删除最后一个逗号和换行
        if (!results.isEmpty()) {
            md.setLength(md.length() - 2);
            md.append("\n");
        }
    }
} 