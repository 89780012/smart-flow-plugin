package com.smart.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.smart.enums.DataType;
import com.smart.enums.RequireType;
import com.smart.enums.ParamType;
import com.smart.enums.StepType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BizFileUtils {

    public static void collectBizFiles(VirtualFile dir, List<BizFileInfo> bizFiles) {
        if (dir == null || !dir.exists()) return;
   
        VirtualFile[] children = dir.getChildren();
        if (children == null) return;
        
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                collectBizFiles(child, bizFiles);
            } else if ("biz".equals(child.getExtension())) {
                try {
                    BizFileInfo bizInfo = parseBizFile(child);
                    bizFiles.add(bizInfo);
                } catch (Exception e) {
                    // 如果解析失败，添加一个只有文件信息的BizFileInfo
                    bizFiles.add(new BizFileInfo(child));
                }
            }
        }
    }

    public static BizFileInfo parseBizFile(VirtualFile file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            try (InputStream is = file.getInputStream()) {
                Document doc = builder.parse(is);
                Element root = doc.getDocumentElement();
                
                // 解析biz文件的各个字段
                String name = getElementText(root, "name");
                String url = getElementText(root, "url");
                String protocol = getElementText(root, "protocol");
                String method = getElementText(root, "method");
                
                // 构建BizFileInfo对象
                BizFileInfo bizFileInfo = new BizFileInfo(file, url, method, name);
                bizFileInfo.setProtocol(protocol);
                
                // 解析参数信息
                NodeList paramsList = root.getElementsByTagName("params");

                //================queryParams================
                if (paramsList != null && paramsList.getLength() > 0) {
                    Element paramsElement = (Element) paramsList.item(0);
                    NodeList queryParamNodes = paramsElement.getElementsByTagName("queryParams");
                    NodeList bodyParamNodes = paramsElement.getElementsByTagName("bodyParams");
                    NodeList jsonParamNodes = paramsElement.getElementsByTagName("jsonParams");

                    List<BizFileInfo.ParamInfo> queryParamList = new ArrayList<>();
                    List<BizFileInfo.ParamInfo> bodyParamList = new ArrayList<>();
                    BizFileInfo.JsonParams jsonParams = new BizFileInfo.JsonParams();

                    if(queryParamNodes.getLength()>0){
                        Element queryParamElement = (Element) queryParamNodes.item(0);
                        NodeList subQueryParamList =queryParamElement.getElementsByTagName("queryParam");
                        for (int i = 0; i < subQueryParamList.getLength(); i++) {
                            Element queryParam = (Element) subQueryParamList.item(i);

                            String paramName = getElementText(queryParam, "name");
                            String paramValue = "";

                            // 解析类型为DataType枚举
                            String typeStr = getElementText(queryParam, "type");
                            DataType paramType = DataType.STRING; // 默认为String类型
                            try {
                                paramType = DataType.getByValue(Integer.parseInt(typeStr));
                            } catch (IllegalArgumentException ignored) {}

                            // 解析是否必填为RequireType枚举
                            String requiredStr = getElementText(queryParam, "required");
                            RequireType required = RequireType.no; // 默认为非必填
                            try {
                                int requiredValue = Integer.parseInt(requiredStr);
                                required = RequireType.getByValue(requiredValue);
                                if (required == null) {
                                    required = RequireType.no;
                                }
                            } catch (NumberFormatException ignored) {}

                            // 获取示例值
                            String defaultValue = getElementText(queryParam, "defaultValue");
                            queryParamList.add(new BizFileInfo.ParamInfo(paramName, paramValue, paramType, required, defaultValue));
                        }
                    }

                    if(bodyParamNodes.getLength()>0) {
                        Element bodyParamElement = (Element) bodyParamNodes.item(0);
                        NodeList subBodyParamList = bodyParamElement.getElementsByTagName("bodyParam");
                        for (int i = 0; i < subBodyParamList.getLength(); i++) {
                            Element bodyParam = (Element) subBodyParamList.item(i);

                            String paramName = getElementText(bodyParam, "name");
                            String paramValue = "";

                            // 解析类型为DataType枚举
                            String typeStr = getElementText(bodyParam, "type");
                            DataType paramType = DataType.STRING; // 默认为String类型
                            try {
                                paramType = DataType.getByValue(Integer.parseInt(typeStr));
                            } catch (IllegalArgumentException ignored) {}

                            // 解析是否必填为RequireType枚举
                            String requiredStr = getElementText(bodyParam, "required");
                            RequireType required = RequireType.no; // 默认为非必填
                            try {
                                int requiredValue = Integer.parseInt(requiredStr);
                                required = RequireType.getByValue(requiredValue);
                                if (required == null) {
                                    required = RequireType.no;
                                }
                            } catch (NumberFormatException ignored) {}

                            // 获取示例值
                            String defaultValue = getElementText(bodyParam, "defaultValue");
                            bodyParamList.add(new BizFileInfo.ParamInfo(paramName, paramValue, paramType, required, defaultValue));
                        }
                    }
                    //================bodyParams================
                    if(jsonParamNodes != null && jsonParamNodes.getLength() > 0) {
                        Element jsonParam = (Element) jsonParamNodes.item(0);
                        String content = getElementText(jsonParam, "content");
                        jsonParams.setContent(content);
                    }
                    BizFileInfo.ParamGroup params = new BizFileInfo.ParamGroup();
                    params.setQueryParams(queryParamList);
                    params.setBodyParams(bodyParamList);
                    params.setJsonParams(jsonParams);
                    bizFileInfo.setParams(params);
                }
                
                // 解析results部分
                NodeList resultsList = root.getElementsByTagName("results");
                if (resultsList != null && resultsList.getLength() > 0) {
                    Element resultsElement = (Element) resultsList.item(0);
                    
                    // 创建Results对象
                    BizFileInfo.Results results = new BizFileInfo.Results();
                    
                    // 设置responseStruct
                    String responseStruct = getElementText(resultsElement, "responseStruct");
                    results.setResponseStruct(responseStruct);
                    
                    // 解析result标签列表
                    NodeList resultNodes = resultsElement.getElementsByTagName("result");
                    List<BizFileInfo.Results.ResultInfo> resultInfoList = new ArrayList<>();
                    
                    for (int i = 0; i < resultNodes.getLength(); i++) {
                        Element resultElement = (Element) resultNodes.item(i);
                        BizFileInfo.Results.ResultInfo resultInfo = new BizFileInfo.Results.ResultInfo();
                        
                        // 设置基本属性
                        resultInfo.setName(getElementText(resultElement, "name"));
                        resultInfo.setDescription(getElementText(resultElement, "description"));
                        resultInfo.setExample(getElementText(resultElement, "example"));
                        
                        // 解析并设置type
                        String typeStr = getElementText(resultElement, "type");
                        try {
                            DataType type = DataType.getByValue(Integer.parseInt(typeStr));
                            resultInfo.setType(type != null ? type : DataType.STRING);
                        } catch (NumberFormatException e) {
                            resultInfo.setType(DataType.STRING); // 默认为String类型
                        }
                        
                        // 解析并设置stepType
                        String stepTypeStr = getElementText(resultElement, "stepType");
                        try {
                            StepType stepType = StepType.getByValue(Integer.parseInt(stepTypeStr));
                            resultInfo.setStepType(stepType != null ? stepType : StepType.UNSTEP);
                        } catch (NumberFormatException e) {
                            resultInfo.setStepType(StepType.UNSTEP); // 默认为不提级
                        }
                        
                        resultInfoList.add(resultInfo);
                    }
                    
                    results.setResult(resultInfoList);
                    bizFileInfo.setResults(results);
                }
                
                return bizFileInfo;
            }
        } catch (Exception e) {
            // 如果解析失败，返回只包含文件信息的对象
            return new BizFileInfo(file);
        }
    }

    private static String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    private static String formatEndpoint(BizFileInfo info) {
        if (info.getUrl() == null || info.getUrl().isEmpty()) {
            return "[No URL]";
        }
        return info.getUrl();
    }
} 