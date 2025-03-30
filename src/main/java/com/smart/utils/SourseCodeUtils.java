package com.smart.utils;

import com.fasterxml.jackson.databind.node.TextNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.smart.bean.ComponentInfo;
import com.smart.bean.ComponentItem;
import com.smart.bean.ComponentProp;
import com.smart.bean.ParamProp;
import com.smart.enums.*;
import com.smart.event.*;
import com.smart.bean.Connection;
import com.smart.cache.PluginCache;
import com.smart.ui.VisualLayoutPanel;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.swing.*;
import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SourseCodeUtils {
    // 针对于当前面板的属性
    private Map<String, Object> propertyMap = new HashMap();
    private List<ComponentInfo> components;
    private List<Connection> connections = new ArrayList<>();
    private JPanel canvasPanel;
    private VirtualFile file;
    private VisualLayoutPanel vPanel;
    // 添加原子变量控制显示状态
    private final AtomicBoolean isShowingMessage = new AtomicBoolean(false);


    public SourseCodeUtils(VirtualFile file,Map<String, Object> propertyMap){
        this.file = file;
        this.propertyMap = propertyMap;
        // 注册事件
        regisUIEvent(this.file.getPath());
    }

    public void initVPanel(VisualLayoutPanel vPanel){
        this.vPanel = vPanel;
        this.components = vPanel.getComponents();
        this.connections = vPanel.getConnections();
        this.canvasPanel = vPanel.getCanvasPanel();
    }


    public String getBizId(){
        return String.valueOf(propertyMap.get("id"));
    }

    public String getComponentId(String componentId){
        return getBizId() + "_" + componentId;
    }

    /**
     * 更新源代码
     */
    public void updateSourceCode() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            System.out.println(propertyMap);

            // 根节点
            Element rootElement = doc.createElement("biz");
            doc.appendChild(rootElement);
            appendTextElement(doc, rootElement, "id", String.valueOf(propertyMap.get("id")));
            appendTextElement(doc, rootElement, "name", String.valueOf(propertyMap.get("name")));
            appendTextElement(doc, rootElement, "url", String.valueOf(propertyMap.get("url")));
            appendTextElement(doc, rootElement, "protocol", String.valueOf(propertyMap.get("protocol")));
            appendTextElement(doc, rootElement, "method", String.valueOf(propertyMap.get("method")));
            appendTextElement(doc, rootElement, "global_sql_transaction", String.valueOf(propertyMap.get("global_sql_transaction")));

            // 入参处理
            Element paramsElement = doc.createElement("params");
            rootElement.appendChild(paramsElement);

            // 处理 queryParams
            Object queryParamsObj = propertyMap.get("params");
            if (queryParamsObj instanceof ObjectNode) {
                // 处理 queryParams - 创建queryParams节点
                ArrayNode queryParamsNode = (ArrayNode) ((ObjectNode) queryParamsObj).get("queryParams");
                if (queryParamsNode != null && queryParamsNode.size() > 0) {
                    Element queryParamsElement = doc.createElement("queryParams");
                    paramsElement.appendChild(queryParamsElement);
                    
                    for (JsonNode paramNode : queryParamsNode) {
                        Element paramElement = doc.createElement("queryParam");
                        queryParamsElement.appendChild(paramElement);

                        appendTextElement(doc, paramElement, "name", paramNode.get("name").asText());
                        appendTextElement(doc, paramElement, "type", paramNode.get("type").asText());
                        appendTextElement(doc, paramElement, "required", paramNode.get("required").asText());

                        if (paramNode.has("defaultValue")) {
                            appendTextElement(doc, paramElement, "defaultValue", paramNode.get("defaultValue").asText());
                        }
                        if (paramNode.has("description")) {
                            appendTextElement(doc, paramElement, "description", paramNode.get("description").asText());
                        }
                    }
                }

                // 处理 bodyParams - 创建bodyParams节点
                ArrayNode bodyParamsNode = (ArrayNode) ((ObjectNode) queryParamsObj).get("bodyParams");
                if (bodyParamsNode != null && bodyParamsNode.size() > 0) {
                    Element bodyParamsElement = doc.createElement("bodyParams");
                    paramsElement.appendChild(bodyParamsElement);
                    
                    for (JsonNode paramNode : bodyParamsNode) {
                        Element paramElement = doc.createElement("bodyParam");
                        bodyParamsElement.appendChild(paramElement);

                        appendTextElement(doc, paramElement, "name", paramNode.get("name").asText());
                        appendTextElement(doc, paramElement, "type", paramNode.get("type").asText());
                        appendTextElement(doc, paramElement, "required", paramNode.get("required").asText());

                        if (paramNode.has("defaultValue")) {
                            appendTextElement(doc, paramElement, "defaultValue", paramNode.get("defaultValue").asText());
                        }
                        if (paramNode.has("description")) {
                            appendTextElement(doc, paramElement, "description", paramNode.get("description").asText());
                        }
                    }
                }

                // 处理 jsonParams
                JsonNode jsonParamsNode = ((ObjectNode) queryParamsObj).get("jsonParams");
                if (jsonParamsNode != null && !jsonParamsNode.isNull()) {
                    Element jsonParamsElement = doc.createElement("jsonParams");
                    paramsElement.appendChild(jsonParamsElement);
                    appendTextElement(doc, jsonParamsElement, "content", jsonParamsNode.asText());
                }
            }

            // 修改返回值配置部分
            Element resultsElement = doc.createElement("results");
            rootElement.appendChild(resultsElement);

            // 从propertyMap获取output配置
            Object outputObj = propertyMap.get("output");
            if(outputObj == null){
                appendTextElement(doc, resultsElement, "responseStruct",
                        String.valueOf(ResponseStructType.STANDARD.getValue()));
            }
            if (outputObj instanceof ObjectNode) {
                ObjectNode outputConfig = (ObjectNode) outputObj;

                //没找到从哪来的显示名称，临时处理下
                String responseStructStr = outputConfig.get("responseStruct").asText();
                if(responseStructStr.contains("标准结构")){
                    responseStructStr = "1";
                }
                // 添加基础配置
                appendTextElement(doc, resultsElement, "responseStruct",
                        responseStructStr);

                // 添加字段定义
                JsonNode fieldsNode = outputConfig.get("fields");
                if (fieldsNode != null && fieldsNode.isArray()) {
                    for (JsonNode fieldNode : fieldsNode) {
                        Element resultElement = doc.createElement("result");
                        resultsElement.appendChild(resultElement);

                        appendTextElement(doc, resultElement, "name", fieldNode.get("name").asText());
                        appendTextElement(doc, resultElement, "type", String.valueOf(fieldNode.get("type").asInt()));
                        appendTextElement(doc, resultElement, "stepType", String.valueOf(fieldNode.get("stepType").asInt()));
                        appendTextElement(doc, resultElement, "description",
                                fieldNode.has("description") ? fieldNode.get("description").asText() : "");
                        appendTextElement(doc, resultElement, "example",
                                fieldNode.has("example") ? fieldNode.get("example").asText() : "");
                    }
                }
            }

            Element flowsElement = doc.createElement("flows");
            rootElement.appendChild(flowsElement);
            // 组件
            for (ComponentInfo info : components) {

                Element componentElement = doc.createElement("component");
                flowsElement.appendChild(componentElement);
                // -----------------基本属性------------------------
                appendTextElement(doc, componentElement, "id", info.getId());
                appendTextElement(doc, componentElement, "type", info.getType());
                appendTextElement(doc, componentElement, "name", info.getName());
                appendTextElement(doc, componentElement, "x", String.valueOf(info.getX()));
                appendTextElement(doc, componentElement, "y", String.valueOf(info.getY()));
                // -----------------基本属性------------------------
                ComponentInfo info2 = PluginCache.componentInfoMap.get(getComponentId(info.getId()));
                ComponentProp componentProp = info2.getComponentProp(); // 组件属性
                if (componentProp == null) {
                    continue;
                }

                Element propertyElement = doc.createElement("property");
                componentElement.appendChild(propertyElement);
                // -------------------bean组件----------------------
                if (componentProp.getBeanRef() != null && !componentProp.getBeanRef().isEmpty()) {
                    appendTextElement(doc, propertyElement, "beanRef", componentProp.getBeanRef());
                }
                if (componentProp.getMethod() != null && !componentProp.getMethod().isEmpty()) {
                    appendTextElement(doc, propertyElement, "method", componentProp.getMethod());
                }
                if (componentProp.getThreadType() != null) {
                    appendTextElement(doc, propertyElement, "threadType",
                            String.valueOf(componentProp.getThreadType().getValue()));
                }

                // -------------------sql组件----------------------
                if (componentProp.getSql() != null) {
                    appendTextElement(doc, propertyElement, "sql", String.valueOf(componentProp.getSql()));
                }
                if (componentProp.getReturnType() != null) {
                    appendTextElement(doc, propertyElement, "returnType",
                            String.valueOf(componentProp.getReturnType().getValue()));
                }
                if (componentProp.getDataSourceKey() != null && !componentProp.getDataSourceKey().isEmpty()) {
                    appendTextElement(doc, propertyElement, "dataSourceKey", componentProp.getDataSourceKey());
                }
                //分页参数
                if (componentProp.getPaginationType() != null) {
                    appendTextElement(doc, propertyElement, "paginationType", componentProp.getPaginationType().name());
                }

                //SQL操作类型
                if (componentProp.getOperationType()!=null){
                    appendTextElement(doc, propertyElement, "operationType", componentProp.getOperationType().name());
                }

                // 绑定参数
                if (componentProp.getBindKey() != null && !componentProp.getBindKey().isEmpty()) {
                    appendTextElement(doc, propertyElement, "bindKey", componentProp.getBindKey());
                }

                // 脚本
                if (componentProp.getScript() != null && !componentProp.getScript().isEmpty()) {
                    appendTextElement(doc, propertyElement, "script", componentProp.getScript());
                }


                List<ParamProp> paramProps = componentProp.getParamProps();
                // 添加参数
                if (paramProps != null && paramProps.size() > 0) {
                    Element paramsElement1 = doc.createElement("params");
                    propertyElement.appendChild(paramsElement1);
                    for (ParamProp param : paramProps) {
                        Element paramElement = doc.createElement("param");
                        paramsElement1.appendChild(paramElement);
                        if (param.getSeq() != null) {
                            appendTextElement(doc, paramElement, "seq", String.valueOf(param.getSeq()));
                        }
                        if (param.getParamType() != null) {
                            appendTextElement(doc, paramElement, "paramType",
                                    String.valueOf(param.getParamType().getValue()));
                        }
                        if (StringUtils.isNotEmpty(param.getKeyName())) {
                            appendTextElement(doc, paramElement, "keyName", String.valueOf(param.getKeyName()));
                        }
                        if (param.getDataType() != null) {
                            appendTextElement(doc, paramElement, "dataType",
                                    String.valueOf(param.getDataType().getValue()));
                        }
                        if (StringUtils.isNotEmpty(param.getVal())) {
                            appendTextElement(doc, paramElement, "val", String.valueOf(param.getVal()));
                        }
                        if (StringUtils.isNotEmpty(param.getVal2())) {
                            appendTextElement(doc, paramElement, "val2", String.valueOf(param.getVal2()));
                        }
                        if (param.getValueCategory() != null) {
                            appendTextElement(doc, paramElement, "valueCategory",
                                    String.valueOf(param.getValueCategory().getValue()));
                        }
                        if (StringUtils.isNotEmpty(param.getValDesc())) {
                            appendTextElement(doc, paramElement, "valDesc", String.valueOf(param.getValDesc()));
                        }
                    }
                }
            }

            // 连接线
            for (Connection conn : connections) {
                if (conn.startPoint != null && conn.endPoint != null) {
                    appendConnectionElement(doc, flowsElement, conn);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);

            final String content = writer.toString();
            ApplicationManager.getApplication().invokeLater(() -> {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadComponentsFromSource() {
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                // 检查文件是否存在且不为空
                if (file == null || file.getLength() == 0) {
                    // 创建基本的XML结构
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.newDocument();

                    Element rootElement = doc.createElement("biz");
                    doc.appendChild(rootElement);

                    String uuid = UUID.randomUUID().toString();
                    appendTextElement(doc, rootElement, "id", uuid);
                    appendTextElement(doc, rootElement, "url", "");
                    appendTextElement(doc, rootElement, "name", "");
                    appendTextElement(doc, rootElement, "protocol", HttpProtocol.JSON.getValue());
                    appendTextElement(doc, rootElement, "method", HttpMethod.GET.getValue());
                    appendTextElement(doc, rootElement, "global_sql_transaction", "false");


                    //增加results标签
                    appendTextElement(doc, rootElement, "results", "");
                    Element resultsElement = (Element) rootElement.getElementsByTagName("results").item(0);
                    appendTextElement(doc, resultsElement, "responseStruct", String.valueOf(ResponseStructType.STANDARD.getValue()));

                    propertyMap.put("id", uuid);
                    propertyMap.put("url", "");
                    propertyMap.put("name", "");
                    propertyMap.put("protocol", HttpProtocol.JSON.getValue());
                    propertyMap.put("method", HttpMethod.GET.getValue());
                    propertyMap.put("global_sql_transaction", false);

                    // 保存文件
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                    DOMSource source = new DOMSource(doc);
                    StringWriter writer = new StringWriter();
                    StreamResult result = new StreamResult(writer);
                    transformer.transform(source, result);

                    String content = writer.toString();
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ApplicationManager.getApplication().runWriteAction(() -> {
                            try {
                                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    });

                    return;
                }

                String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                parseContent(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 从源代码加载组件，维护好连接线和组件
     */
    public void loadComponentsFromSource(String content) {
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                parseContent(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    public void parseContent(String content) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try {
            doc = builder.parse(new InputSource(new StringReader(content)));
        } catch (Exception e) {
            // XML解析失败，可能是空文件或格式错误
            propertyMap.clear();
            propertyMap.put("params", new ObjectMapper().createObjectNode());
            return;
        }

        // 清空现有数据
        components.clear();
        connections.clear();
        propertyMap.clear();
        canvasPanel.removeAll();
        canvasPanel.revalidate();
        canvasPanel.repaint();

        // 解析基本属性
        Element bizElement = (Element) (doc.getElementsByTagName("biz").item(0));
        if (bizElement != null) {
            propertyMap.put("id", getElementTextContent(bizElement, "id"));
            propertyMap.put("url", getElementTextContent(bizElement, "url"));
            propertyMap.put("protocol", getElementTextContent(bizElement, "protocol"));
            propertyMap.put("method", getElementTextContent(bizElement, "method"));
            propertyMap.put("name", getElementTextContent(bizElement, "name"));

            String global_sql_transaction = getElementTextContent(bizElement, "global_sql_transaction");
            propertyMap.put("global_sql_transaction",  global_sql_transaction.equals("") ? false: Boolean.valueOf(global_sql_transaction));
        }

        // 解析入参 - 修改这部分逻辑以处理多种参数类型
        if (bizElement != null) {
            NodeList paramsNodeList = bizElement.getElementsByTagName("params");
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode paramsObject = mapper.createObjectNode();

            if (paramsNodeList != null && paramsNodeList.getLength() > 0) {
                Element paramsElement = (Element) paramsNodeList.item(0);
                
                // 处理 queryParams
                NodeList queryParamsNodes = paramsElement.getElementsByTagName("queryParams");
                if (queryParamsNodes != null && queryParamsNodes.getLength() > 0) {
                    Element queryParamsElement = (Element) queryParamsNodes.item(0);
                    NodeList queryParamNodes = queryParamsElement.getElementsByTagName("queryParam");
                    
                    ArrayNode queryParamsArray = mapper.createArrayNode();
                    for (int i = 0; i < queryParamNodes.getLength(); i++) {
                        Element paramElement = (Element) queryParamNodes.item(i);
                        ObjectNode paramNode = mapper.createObjectNode();
                        
                        String name = getElementTextContent(paramElement, "name");
                        paramNode.put("name", name);
                        
                        String typeValue = getElementTextContent(paramElement, "type");
                        paramNode.put("type", Integer.parseInt(typeValue));
                        
                        String requiredValue = getElementTextContent(paramElement, "required");
                        paramNode.put("required", Integer.parseInt(requiredValue));
                        
                        String defaultValue = getElementTextContent(paramElement, "defaultValue");
                        if (!defaultValue.isEmpty()) {
                            paramNode.put("defaultValue", defaultValue);
                        }
                        
                        String description = getElementTextContent(paramElement, "description");
                        if (!description.isEmpty()) {
                            paramNode.put("description", description);
                        }
                        
                        queryParamsArray.add(paramNode);
                    }
                    paramsObject.set("queryParams", queryParamsArray);
                } else {
                    // 如果没有找到queryParams节点，创建一个空数组
                    paramsObject.set("queryParams", mapper.createArrayNode());
                }
                
                // 处理 bodyParams
                NodeList bodyParamsNodes = paramsElement.getElementsByTagName("bodyParams");
                if (bodyParamsNodes != null && bodyParamsNodes.getLength() > 0) {
                    Element bodyParamsElement = (Element) bodyParamsNodes.item(0);
                    NodeList bodyParamNodes = bodyParamsElement.getElementsByTagName("bodyParam");
                    
                    ArrayNode bodyParamsArray = mapper.createArrayNode();
                    for (int i = 0; i < bodyParamNodes.getLength(); i++) {
                        Element paramElement = (Element) bodyParamNodes.item(i);
                        ObjectNode paramNode = mapper.createObjectNode();
                        
                        String name = getElementTextContent(paramElement, "name");
                        paramNode.put("name", name);
                        
                        String typeValue = getElementTextContent(paramElement, "type");
                        paramNode.put("type", Integer.parseInt(typeValue));
                        
                        String requiredValue = getElementTextContent(paramElement, "required");
                        paramNode.put("required", Integer.parseInt(requiredValue));
                        
                        String defaultValue = getElementTextContent(paramElement, "defaultValue");
                        if (!defaultValue.isEmpty()) {
                            paramNode.put("defaultValue", defaultValue);
                        }
                        
                        String description = getElementTextContent(paramElement, "description");
                        if (!description.isEmpty()) {
                            paramNode.put("description", description);
                        }
                        
                        bodyParamsArray.add(paramNode);
                    }
                    paramsObject.set("bodyParams", bodyParamsArray);
                } else {
                    // 如果没有找到bodyParams节点，创建一个空数组
                    paramsObject.set("bodyParams", mapper.createArrayNode());
                }
                
                // 处理 jsonParams
                NodeList jsonParamsNodes = paramsElement.getElementsByTagName("jsonParams");
                if (jsonParamsNodes != null && jsonParamsNodes.getLength() > 0) {
                    Element jsonParamsElement = (Element) jsonParamsNodes.item(0);
                    String jsonContent = getElementTextContent(jsonParamsElement, "content");
                    if (!jsonContent.isEmpty()) {
                        paramsObject.put("jsonParams", jsonContent);
                    }
                }
            }
            
            // 将解析后的参数对象存入propertyMap
            propertyMap.put("params", paramsObject);
        }

        // 添加组件
        NodeList componentNodes = doc.getElementsByTagName("component");
        for (int i = 0; i < componentNodes.getLength(); i++) {
            Element componentElement = (Element) componentNodes.item(i);
            String id = getElementTextContent(componentElement, "id");
            String name = getElementTextContent(componentElement, "name");
            int x = Integer.parseInt(getElementTextContent(componentElement, "x"));
            int y = Integer.parseInt(getElementTextContent(componentElement, "y"));
            String type = getElementTextContent(componentElement, "type");

            // 从componentElement找到下面的property节点
            NodeList propertyNodes = componentElement.getElementsByTagName("property");
            ComponentProp componentProp = new ComponentProp();
            if (propertyNodes.getLength() > 0) {
                // 只有一个property节点
                Element propertyElement = (Element) propertyNodes.item(0);

                // 子节点
                if (propertyElement.getChildNodes().getLength() > 0) {
                    String beanRef = getElementTextContent(propertyElement, "beanRef");
                    if (StringUtils.isNotEmpty(beanRef)) {
                        componentProp.setBeanRef(beanRef);
                    }
                    String method = getElementTextContent(propertyElement, "method");
                    if (StringUtils.isNotEmpty(method)) {
                        componentProp.setMethod(method);
                    }
                    String threadType = getElementTextContent(propertyElement, "threadType");
                    if (StringUtils.isNotEmpty(threadType)) {
                        ThreadType threadType1 = ThreadType.getByValue(Integer.parseInt(threadType));
                        componentProp.setThreadType(threadType1);
                    }
                    String dataSourceKey = getElementTextContent(propertyElement, "dataSourceKey");
                    if (StringUtils.isNotEmpty(dataSourceKey)) {
                        componentProp.setDataSourceKey(dataSourceKey);
                    }
                    String sql = getElementTextContent(propertyElement, "sql");
                    if (StringUtils.isNotEmpty(sql)) {
                        componentProp.setSql(sql);
                    }
                    String returnType = getElementTextContent(propertyElement, "returnType");
                    if (StringUtils.isNotEmpty(returnType)) {
                        ReturnType returnType1 = ReturnType.getByValue(Integer.parseInt(returnType));
                        componentProp.setReturnType(returnType1);
                    }
                    String bindKey = getElementTextContent(propertyElement, "bindKey");
                    if (StringUtils.isNotEmpty(bindKey)) {
                        componentProp.setBindKey(bindKey);
                    }

                    String operationType = getElementTextContent(propertyElement, "operationType");
                    if(StringUtils.isNotEmpty(operationType)){
                        SQLOperationType sqlOperationType = SQLOperationType.getByKey(operationType);
                        componentProp.setOperationType(sqlOperationType);
                    }
                    // 脚本参数
                    String script = getElementTextContent(propertyElement, "script");
                    if (StringUtils.isNotEmpty(script)) {
                        componentProp.setScript(script);
                    }

                    String paginationType = getElementTextContent(propertyElement, "paginationType");
                    if (StringUtils.isNotEmpty(paginationType)) {
                        componentProp.setPaginationType(PaginationType.valueOf(paginationType));
                    }
                }

                // 看propertyElement 是否有 paramProps 节点
                NodeList paramPropsNodes = propertyElement.getElementsByTagName("params");
                if (paramPropsNodes.getLength() > 0) { // 存在此节点，找到下面的属性
                    Element paramPropsElement = (Element) paramPropsNodes.item(0);
                    if (propertyElement.getChildNodes().getLength() > 0) {
                        NodeList paramPropNodes = paramPropsElement.getElementsByTagName("param");
                        List<ParamProp> paramProps = new ArrayList<>();
                        for (int j = 0; j < paramPropNodes.getLength(); j++) {
                            ParamProp paramProp = new ParamProp();
                            Element paramPropElement = (Element) paramPropNodes.item(j);
                            String paramType = getElementTextContent(paramPropElement, "paramType");
                            if (StringUtils.isNotEmpty(paramType)) {
                                paramProp.setParamType(ParamType.getByValue(Integer.parseInt(paramType)));
                            }
                            String keyName = getElementTextContent(paramPropElement, "keyName");
                            paramProp.setKeyName(keyName);
                            String dataType = getElementTextContent(paramPropElement, "dataType");
                            if (StringUtils.isNotEmpty(dataType)) {
                                DataType dataType1 = DataType.getByValue(Integer.parseInt(dataType));
                                paramProp.setDataType(dataType1);
                            }
                            String val = getElementTextContent(paramPropElement, "val");
                            paramProp.setVal(val);
                            String val2 = getElementTextContent(paramPropElement, "val2");
                            paramProp.setVal2(val2);
                            String valueCategory = getElementTextContent(paramPropElement, "valueCategory");
                            if (StringUtils.isNotEmpty(valueCategory)) {
                                ValueCategory valueCategory1 = ValueCategory
                                        .getByValue(Integer.parseInt(valueCategory));
                                paramProp.setValueCategory(valueCategory1);
                            }
                            String valDesc = getElementTextContent(paramPropElement, "valDesc");
                            paramProp.setValDesc(valDesc);
                            String seq = getElementTextContent(paramPropElement, "seq");
                            paramProp.setSeq(seq);
                            paramProps.add(paramProp);
                        }
                        componentProp.setParamProps(paramProps);
                    }
                }
            }

            // 组件
            ComponentInfo info = new ComponentInfo(id, name, x, y, type);
            info.setComponentProp(componentProp);
            PluginCache.componentInfoMap.put(getComponentId(info.getId()), info);

            components.add(info);

        }

        // 添加连接线
        NodeList connectionNodes = doc.getElementsByTagName("connection");
        for (int i = 0; i < connectionNodes.getLength(); i++) {
            Element connectionElement = (Element) connectionNodes.item(i);
            parseConnectionElement(connectionElement); // 使用封装的方法来解析连接线
        }

        // 获取biz节点 results 渲染
        if (bizElement != null) {
            // 从biz节点下获取results节点
            NodeList resultsNodes = bizElement.getElementsByTagName("results");
            if (resultsNodes.getLength() > 0) {
                Element resultsElement = (Element) resultsNodes.item(0);

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode outputConfig = mapper.createObjectNode();
                ArrayNode fieldsArray = outputConfig.putArray("fields");

                // 加载基础配置
                String responseStruct = getElementTextContent(resultsElement, "responseStruct");
                outputConfig.put("responseStruct",
                        responseStruct.isEmpty() ? "标准结构(code/message/data)" : responseStruct);

                // 加载字段定义
                NodeList resultNodes = resultsElement.getElementsByTagName("result");
                for (int i = 0; i < resultNodes.getLength(); i++) {
                    Element resultElement = (Element) resultNodes.item(i);

                    ObjectNode fieldNode = mapper.createObjectNode()
                            .put("name", getElementTextContent(resultElement, "name"))
                            .put("type", Integer.parseInt(getElementTextContent(resultElement, "type")))
                            .put("description", getElementTextContent(resultElement, "description"))
                            .put("example", getElementTextContent(resultElement, "example"));

                    String stepTypeStr = getElementTextContent(resultElement, "stepType");
                    if(StringUtils.isNotEmpty(stepTypeStr)){
                        fieldNode.put("stepType", Integer.parseInt(stepTypeStr));
                    }else{
                        fieldNode.put("stepType", StepType.UNSTEP.getValue());
                    }

                    fieldsArray.add(fieldNode);
                }

                propertyMap.put("output", outputConfig);
            } else {
                // 如果没有results节点，创建默认配置
                createDefaultOutputConfig();
            }
        } else {
            // 如果没有biz节点，创建默认配置
            createDefaultOutputConfig();
        }

        canvasPanel.removeAll();
        // 创建组件面板
        for (ComponentInfo info : components) {
            // 使用正确的图标创建组件面板
            String iconPath = PluginCache.type2icon.get(info.getType());

            // 根据组件类型获取正确的图标
            ComponentItem item = PluginCache.componentItemMap.get(info.getType());
            item.setName(info.getName());
            item.setIconPath(iconPath);
            vPanel.addComponentFromSourceCode(item, new Point(info.getX(), info.getY()), info.getId(), false); // 使用
            // false
            // 表示这不是新拖拽的组件
        }
        // 重绘画布
        canvasPanel.revalidate();
        canvasPanel.repaint();
    }

    // 抽取创建默认配置的方法
    private void createDefaultOutputConfig() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode outputConfig = mapper.createObjectNode();
        outputConfig.put("responseStruct", "1");
        outputConfig.putArray("fields");
        propertyMap.put("output", outputConfig);
    }

    private void appendTextElement(Document doc, Element parent, String tagName, String content) {
        Element element = doc.createElement(tagName);
        element.setTextContent(content);
        parent.appendChild(element);
    }

    /**
     * 获取元素文本内容
     * 
     * @param parent  父元素
     * @param tagName 标签名
     * @return 文本内
     */
    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }


    public void regisUIEvent(String filePath) {
        // 更新配置文件
        EventBus.getInstance().register(EventType.UPDATE_SOURCE_CODE + "_" + filePath, data -> {
            SwingUtilities.invokeLater(() -> {
                // 更新属性面板
                updateSourceCode();
            });
        });

        // 更新配置文件
        EventBus.getInstance().register(EventType.UPDATE_COMPONENT_SETTINGS + "_" + filePath, componentId -> {
            // 使用原子变量控制是否正在显示提示框
            if (!isShowingMessage.compareAndSet(false, true)) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                try {
                    updateComponentSettings(String.valueOf(componentId));
                } finally {
                    // 确保状态被重置
                    isShowingMessage.set(false);
                }
            });

        });
    }

    // 当biz页面退出时候解绑注册事件
    public void unregisterUIEvent(String filePath) {
        EventBus.getInstance().unregister(EventType.UPDATE_SOURCE_CODE + "_" + filePath);
        EventBus.getInstance().unregister(EventType.UPDATE_COMPONENT_SETTINGS + "_" + filePath);
    }

    // 更新组件设置
    public void updateComponentSettings(String componentId) {
        ComponentInfo componentToUpdate = PluginCache.componentInfoMap.get(getComponentId(componentId));

        if (componentToUpdate != null) {
            try {
                // 更新源代码
                updateSourceCode();
                showSuccessMessage();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(canvasPanel,
                        "保存配置失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println("未找到要更新的组件: " + componentId);
        }
    }

    // 将显示成功消息的逻辑抽取为单独的方法
    private void showSuccessMessage() {
        // 获取当前焦点所在的窗口
        Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        if (focusedWindow == null) {
            focusedWindow = SwingUtilities.getWindowAncestor(canvasPanel);
        }

        if (focusedWindow != null) {
            // 创建提示消息面板
            JLabel messageLabel = new JLabel("配置保存成功");
            messageLabel.setForeground(Color.WHITE);
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel messagePanel = new JPanel();
            messagePanel.setBackground(new Color(60, 179, 113, 255));
            messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            messagePanel.add(messageLabel);

            JDialog dialog = new JDialog(focusedWindow);
            dialog.setUndecorated(true);
            dialog.setBackground(new Color(0, 0, 0, 0));
            dialog.add(messagePanel);
            dialog.pack();

            // 设置位置到右上角
            int x = focusedWindow.getX() + focusedWindow.getWidth() - dialog.getWidth() - 20;
            int y = focusedWindow.getY() + 20;
            dialog.setLocation(x, y);

            dialog.setVisible(true);

            // 创建淡出效果
            Timer fadeTimer = new Timer(50, null);
            final float[] opacity = { 1.0f };

            fadeTimer.addActionListener(e -> {
                opacity[0] -= 0.05f;

                if (opacity[0] <= 0) {
                    fadeTimer.stop();
                    dialog.dispose();
                    return;
                }

                messagePanel.setBackground(new Color(60, 179, 113,
                        Math.max(0, Math.min(255, (int) (opacity[0] * 255)))));
                dialog.repaint();
            });

            // 使用单个计时器控制显示和淡出
            Timer startTimer = new Timer(1000, e -> fadeTimer.start());
            startTimer.setRepeats(false);
            startTimer.start();
        }
    }

    // 在生成连接线相关的 XML 时添加表达式信息
    private void appendConnectionElement(Document doc, Element flowsElement, Connection conn) {
        Element connectionElement = doc.createElement("connection");
        flowsElement.appendChild(connectionElement);

        // 添加现有的连接线属性...
        appendTextElement(doc, connectionElement, "id", conn.id);
        appendTextElement(doc, connectionElement, "from", conn.start.getId());
        appendTextElement(doc, connectionElement, "to", conn.end.getId());
        appendTextElement(doc, connectionElement, "startX", String.valueOf(conn.startPoint.x));
        appendTextElement(doc, connectionElement, "startY", String.valueOf(conn.startPoint.y));
        appendTextElement(doc, connectionElement, "endX", String.valueOf(conn.endPoint.x));
        appendTextElement(doc, connectionElement, "endY", String.valueOf(conn.endPoint.y));
        appendTextElement(doc, connectionElement, "controlX", String.valueOf(conn.controlPoint.x));
        appendTextElement(doc, connectionElement, "controlY", String.valueOf(conn.controlPoint.y));
        appendTextElement(doc, connectionElement, "label", conn.label);

        // 添加表达式相关的 XML
        if (conn.getExpression() != null && !conn.getExpression().isEmpty()) {
            Element expressionElement = doc.createElement("expression");
            expressionElement.setAttribute("language", conn.getExpressionLanguage());
            // 使用 CDATA 包装表达式内容
            CDATASection cdataSection = doc.createCDATASection(conn.getExpression());
            expressionElement.appendChild(cdataSection);
            connectionElement.appendChild(expressionElement);
        }
    }

    // 在解析连接线时添加表达式的解析
    private void parseConnectionElement(Element connectionElement) {
        String id = getElementTextContent(connectionElement, "id");
        String fromId = getElementTextContent(connectionElement, "from");
        String toId = getElementTextContent(connectionElement, "to");
        int startX = Integer.parseInt(getElementTextContent(connectionElement, "startX"));
        int startY = Integer.parseInt(getElementTextContent(connectionElement, "startY"));
        int endX = Integer.parseInt(getElementTextContent(connectionElement, "endX"));
        int endY = Integer.parseInt(getElementTextContent(connectionElement, "endY"));
        int controlX = Integer.parseInt(getElementTextContent(connectionElement, "controlX"));
        int controlY = Integer.parseInt(getElementTextContent(connectionElement, "controlY"));
        String label = getElementTextContent(connectionElement, "label");

        ComponentInfo fromComponent = PluginCache.componentInfoMap.get(getComponentId(String.valueOf(fromId)));
        ComponentInfo toComponent = PluginCache.componentInfoMap.get(getComponentId(String.valueOf(toId)));

        if (fromComponent != null && toComponent != null) {
            Connection conn = new Connection(fromComponent, toComponent,
                    new Point(startX, startY),
                    new Point(endX, endY));
            conn.id = id;
            conn.label = label;
            conn.controlPoint = new Point(controlX, controlY);

            // 解析表达式
            NodeList expressionNodes = connectionElement.getElementsByTagName("expression");
            if (expressionNodes.getLength() > 0) {
                Element expressionElement = (Element) expressionNodes.item(0);
                String language = expressionElement.getAttribute("language");
                String expression = expressionElement.getTextContent().trim();

                // 如果表达式内容被CDATA包裹,需要去除CDATA标记
                if (expression.startsWith("<![CDATA[") && expression.endsWith("]]>")) {
                    expression = expression.substring(9, expression.length() - 3).trim();
                }

                conn.setExpression(expression);
                conn.setExpressionLanguage(language);
            }

            connections.add(conn);
        }
    }

}
