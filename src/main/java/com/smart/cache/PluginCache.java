package com.smart.cache;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.smart.archive.ArchiveManager;
import com.smart.bean.ComponentInfo;
import com.smart.bean.ComponentItem;
import com.smart.utils.SourseCodeUtils;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PluginCache {

    public static Project project;

    //组件名称--图标
    public static Map<String, String> componentIconMap = new ConcurrentHashMap<>();

    //组件类型--图标
    public static Map<String, String> type2icon = new ConcurrentHashMap<>();

    //id->组件
    public static Map<String, ComponentInfo> componentInfoMap = new ConcurrentHashMap<>();

    //缓存Spring Bean类
    public static Map<String, PsiClass> springBeanClasses = new ConcurrentHashMap<>();

    public static Map<String, ComponentItem> componentItemMap = new ConcurrentHashMap<>();

    //授权标志
    public static boolean isValidLicense = false;

    public static ArchiveManager archiveManager; //存档管理器

    //存档按钮
    public static JToggleButton archiveTab;


    //filePath: SourceCodeUtils
    public static Map<String, SourseCodeUtils> sourceCodeUtilMap = new ConcurrentHashMap<String,SourseCodeUtils>();

    //远程存储
    public static boolean enableRemoteStorage = false;

    // 在类中添加新字段
    public static List<String> availableModels;


    public static void updateGlobalBizId(String oldBizId, String newBizId) {
        Set<String> bizIds = new HashSet<>(componentInfoMap.keySet());
        for (String bizId : bizIds) {
            if (bizId.startsWith(oldBizId)) {
                // 创建新的键
                String newKey = newBizId + bizId.substring(oldBizId.length());
                // 获取旧键的值
                ComponentInfo value = componentInfoMap.get(bizId);
                // 将值移动到新键
                componentInfoMap.put(newKey, value);
                // 删除旧键
                componentInfoMap.remove(bizId);
            }
        }
    }

    public static String currentModel="默认模式";

}
