package com.smart.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BizFileUtils {

    private static void collectBizFiles(VirtualFile dir, List<BizFileInfo> bizFiles) {
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
                
                // 构建显示信息
                String displayMethod = method + " " + protocol;
                
                return new BizFileInfo(file, url, displayMethod, name);
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