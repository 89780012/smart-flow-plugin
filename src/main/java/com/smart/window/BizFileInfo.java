package com.smart.window;

import com.intellij.openapi.vfs.VirtualFile;
import com.smart.enums.DataType;
import com.smart.enums.RequireType;
import com.smart.enums.ParamType;
import java.util.ArrayList;
import java.util.List;

public class BizFileInfo {
    private final VirtualFile file;
    private String id;  // biz文件的唯一标识
    private String name; // 接口名称
    private String url;  // 接口URL
    private String protocol; // 协议类型(如 application/json)
    private String method;  // HTTP方法
    private ParamGroup params; // 参数组
    private Results results; // 返回结果配置
    private List<FlowComponent> flows; // 流程组件配置


    public BizFileInfo(VirtualFile file, String url, String method, String name) {
        this.file = file;
        this.url = url;
        this.method = method;
        this.name = name;
    }
    public BizFileInfo(VirtualFile file) {
        this.file = file;
        this.flows = new ArrayList<>();
    }

    // 参数组(包含查询参数、请求体参数、JSON参数)
    public static class ParamGroup {
        private List<ParamInfo> queryParams;  // 查询参数
        private List<ParamInfo> bodyParams;   // 请求体参数
        private JsonParams jsonParams;        // JSON格式参数

        public ParamGroup() {
            this.queryParams = new ArrayList<>();
            this.bodyParams = new ArrayList<>();
        }

        public List<ParamInfo> getQueryParams() {
            return queryParams;
        }

        public void setQueryParams(List<ParamInfo> queryParams) {
            this.queryParams = queryParams;
        }

        public List<ParamInfo> getBodyParams() {
            return bodyParams;
        }

        public void setBodyParams(List<ParamInfo> bodyParams) {
            this.bodyParams = bodyParams;
        }

        public JsonParams getJsonParams() {
            return jsonParams;
        }

        public void setJsonParams(JsonParams jsonParams) {
            this.jsonParams = jsonParams;
        }
    }

    // JSON参数结构
    public static class JsonParams {
        private String content;  // JSON内容

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    // 单个参数信息
    public static class ParamInfo {
        private String name;        // 参数名
        private String value;       // 参数值

        private DataType type;      // 参数类型
        private RequireType required; // 是否必填
        private String defaultValue; // 默认值
        private String description;  // 参数描述

        public ParamInfo(String name, String value, DataType type, RequireType required, String defaultValue) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public DataType getType() {
            return type;
        }

        public void setType(DataType type) {
            this.type = type;
        }

        public RequireType getRequired() {
            return required;
        }

        public void setRequired(RequireType required) {
            this.required = required;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // 返回结果配置
    public static class Results {
        private String responseStruct; // 响应结构
        private String successCode;    // 成功码
        private String description;    // 描述

        public String getResponseStruct() {
            return responseStruct;
        }

        public void setResponseStruct(String responseStruct) {
            this.responseStruct = responseStruct;
        }

        public String getSuccessCode() {
            return successCode;
        }

        public void setSuccessCode(String successCode) {
            this.successCode = successCode;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // 流程组件
    public static class FlowComponent {
        private String id;           // 组件ID
        private String type;         // 组件类型
        private String name;         // 组件名称
        private int x;               // X坐标
        private int y;               // Y坐标
        private ComponentProperty property; // 组件属性

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public ComponentProperty getProperty() {
            return property;
        }

        public void setProperty(ComponentProperty property) {
            this.property = property;
        }
    }

    // 组件属性
    public static class ComponentProperty {
        private String operationType;  // 操作类型
        private String beanRef;        // Bean引用
        private String method;         // 方法
        private List<ComponentParam> params; // 组件参数列表

        public ComponentProperty() {
            this.params = new ArrayList<>();
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public String getBeanRef() {
            return beanRef;
        }

        public void setBeanRef(String beanRef) {
            this.beanRef = beanRef;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public List<ComponentParam> getParams() {
            return params;
        }

        public void setParams(List<ComponentParam> params) {
            this.params = params;
        }
    }

    // 组件参数
    public static class ComponentParam {
        private int seq;      // 序号
        private String val;   // 值
        private String type;  // 类型

        public int getSeq() {
            return seq;
        }

        public void setSeq(int seq) {
            this.seq = seq;
        }

        public String getVal() {
            return val;
        }

        public void setVal(String val) {
            this.val = val;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // 基础 getters and setters
    public VirtualFile getFile() { return file; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public ParamGroup getParams() { return params; }
    public void setParams(ParamGroup params) { this.params = params; }
    public Results getResults() { return results; }
    public void setResults(Results results) { this.results = results; }
    public List<FlowComponent> getFlows() { return flows; }
    public void setFlows(List<FlowComponent> flows) { this.flows = flows; }
}