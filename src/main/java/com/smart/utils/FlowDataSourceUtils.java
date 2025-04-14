package com.smart.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowDataSourceUtils {

    // 从flow.xml 中获取属性配置
    public static JComboBox<String> getDataSourceComboBox( Project project, VirtualFile file) {
        JComboBox<String> comboBox = new JComboBox<>();
        List<String> dataSources = loadDataSourcesFromConfig(project,file);
        for (String ds : dataSources) {
            comboBox.addItem(ds);
        }
        return comboBox;
    }

    // 从flow.xml加载文件上传配置的完整信息
    public static List<Map<String, String>> loadFileUploadConfigs(Project project, VirtualFile file) {
        List<Map<String, String>> fileUploadConfigs = new ArrayList<>();
        try {
            File configFile = findFlowXmlFile(project, file);
            
            if (configFile != null && configFile.exists()) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(configFile);
                doc.getDocumentElement().normalize();

                NodeList fileUploadNodes = doc.getElementsByTagName("fileUpload");
                for (int i = 0; i < fileUploadNodes.getLength(); i++) {
                    Element fileUploadElement = (Element) fileUploadNodes.item(i);
                    String id = fileUploadElement.getAttribute("id");
                    String name = fileUploadElement.getAttribute("name");
                    String type = fileUploadElement.getAttribute("type");

                    Map<String, String> configMap = new HashMap<>();
                    configMap.put("id", id);
                    configMap.put("name", name);
                    configMap.put("type", type);
                    fileUploadConfigs.add(configMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Messages.showErrorDialog("加载文件上传配置失败: " + e.getMessage(), "错误");
        }
        return fileUploadConfigs;
    }

    // 获取flow.xml文件 - 公开此方法使其可被其他类调用
    public static File findFlowXmlFile(Project project, VirtualFile file) {
        File configFile = null;
        try {
            // 获取当前文件所在的模块
            Module currentModule = ProjectFileIndex.getInstance(project).getModuleForFile(file);

            if (currentModule != null) {
                // 获取模块根路径
                VirtualFile[] contentRoots = ModuleRootManager.getInstance(currentModule).getContentRoots();
                if (contentRoots.length > 0) {
                    // 先尝试resources目录
                    File resourcePath = new File(contentRoots[0].getPath() + "/src/main/resources/flow.xml");
                    if (resourcePath.exists()) {
                        configFile = resourcePath;
                    } else {
                        // 再尝试模块根目录
                        File rootPath = new File(contentRoots[0].getPath() + "/flow.xml");
                        if (rootPath.exists()) {
                            configFile = rootPath;
                        }
                    }
                }
            }

            // 如果在模块中没找到,fallback到项目根目录(兼容旧结构)
            if (configFile == null) {
                String projectPath = project.getBasePath();
                configFile = new File(projectPath + "/src/main/resources/flow.xml");

                if (!configFile.exists()) {
                    configFile = new File(projectPath + "/flow.xml");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configFile;
    }

    private static List<String> loadDataSourcesFromConfig(Project project, VirtualFile file) {
        List<String> dataSources = new ArrayList<>();
        try {
            File configFile = findFlowXmlFile(project, file);

            if (configFile != null && configFile.exists()) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(configFile);
                doc.getDocumentElement().normalize();

                NodeList dataSourceNodes = doc.getElementsByTagName("dataSource");
                for (int i = 0; i < dataSourceNodes.getLength(); i++) {
                    Element dataSourceElement = (Element) dataSourceNodes.item(i);
                    String id = dataSourceElement.getAttribute("id");
                    if (id != null && !id.isEmpty()) {
                        dataSources.add(id);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Messages.showErrorDialog("加载数据源配置失败: " + e.getMessage(), "错误");
        }

        return dataSources;
    }
}
