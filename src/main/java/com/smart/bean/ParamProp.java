package com.smart.bean;

import com.smart.enums.DataType;
import com.smart.enums.ParamType;
import com.smart.enums.ValueCategory;

public class ParamProp implements java.io.Serializable {
    private String seq; // 序列号
    private ParamType paramType; // 参数类型 参数/返回
    private String keyName;     // 参数名
    private DataType dataType;  // 数据类型
    private String val;        // 参数值
    private String val2;        // 参数值2
    private ValueCategory valueCategory; // 参数值类型
    private String valDesc; // 参数描述

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public ParamType getParamType() {
        return paramType;
    }

    public void setParamType(ParamType paramType) {
        this.paramType = paramType;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public String getVal2() {
        return val2;
    }

    public void setVal2(String val2) {
        this.val2 = val2;
    }

    public ValueCategory getValueCategory() {
        return valueCategory;
    }

    public void setValueCategory(ValueCategory valueCategory) {
        this.valueCategory = valueCategory;
    }

    public String getValDesc() {
        return valDesc;
    }

    public void setValDesc(String valDesc) {
        this.valDesc = valDesc;
    }
}
