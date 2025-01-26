package com.smart.bean;

import com.smart.enums.ReturnType;
import com.smart.enums.ThreadType;
import com.smart.enums.PaginationType;
import com.smart.enums.SQLOperationType;

import java.util.ArrayList;
import java.util.List;

public class ComponentProp implements java.io.Serializable {

    private String beanRef;

    private String method;

    // 是否同步异步
    private ThreadType threadType;

    private List<ParamProp> paramProps;

    //sql 组件
    private String sql;
    private ReturnType returnType ;
    private String dataSourceKey;  //数据源key

    // 脚本
    private String script;

    // 单个组件绑定参数
    private String bindKey;

    private PaginationType paginationType;

    private SQLOperationType operationType = SQLOperationType.QUERY; // 默认为查询

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getBindKey() {
        return bindKey;
    }

    public void setBindKey(String bindKey) {
        this.bindKey = bindKey;
    }

    public String getDataSourceKey() {
        return dataSourceKey;
    }

    public void setDataSourceKey(String dataSourceKey) {
        this.dataSourceKey = dataSourceKey;
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

    public ThreadType getThreadType() {
        return threadType;
    }

    public void setThreadType(ThreadType threadType) {
        this.threadType = threadType;
    }

    public List<ParamProp> getParamProps() {
        return paramProps;
    }

    public void setParamProps(List<ParamProp> paramProps) {
        this.paramProps = paramProps;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public ReturnType getReturnType() {
        return returnType;
    }

    public void setReturnType(ReturnType returnType) {
        this.returnType = returnType;
    }

    public PaginationType getPaginationType() {
        return paginationType;
    }

    public void setPaginationType(PaginationType paginationType) {
        this.paginationType = paginationType;
    }

    public SQLOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(SQLOperationType operationType) {
        this.operationType = operationType;
    }

}
