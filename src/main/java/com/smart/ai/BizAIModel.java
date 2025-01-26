package com.smart.ai;

import com.smart.bean.ComponentItem;
import com.smart.cache.PluginCache;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BizAIModel {
    // 存储biz文件的组件模式
    private List<ComponentPattern> patterns = new ArrayList<>();
    // 组件类型到其使用模式的映射
    private Map<String, List<ComponentUsage>> componentUsages = new HashMap<>();
    // 组件类型关键词映射
    private Map<String, Set<String>> typeKeywords = new HashMap<>();
    
    public BizAIModel() {
        initTypeKeywords();
    }
    
    private void initTypeKeywords() {
        // 初始化各组件类型的关键词
//        typeKeywords.put("print", new HashSet<>(Arrays.asList("打印", "输出", "显示", "print", "log", "日志")));
//        typeKeywords.put("sql", new HashSet<>(Arrays.asList("数据库", "查询", "sql", "database", "query", "select")));
//        typeKeywords.put("assign", new HashSet<>(Arrays.asList("赋值", "设置", "assign", "set", "变量")));
//        typeKeywords.put("groovy", new HashSet<>(Arrays.asList("脚本", "计算", "groovy", "script", "compute")));
//        typeKeywords.put("date", new HashSet<>(Arrays.asList("日期", "时间", "date", "time", "datetime")));
        // 添加更多组件类型的关键词
    }
    
    public void learnFromBiz(List<String> bizContent) {
        ComponentPattern pattern = parseBizContent(bizContent);
        if (pattern != null) {
            patterns.add(pattern);
//            pattern.getComponents().forEach(comp -> {
//                componentUsages.computeIfAbsent(comp.getType(), k -> new ArrayList<>())
//                    .add(new ComponentUsage(comp, pattern.getContext()));
//            });
        }
    }
    
    private ComponentPattern parseBizContent(List<String> content) {
        ComponentPattern pattern = new ComponentPattern();
        Map<String, String> context = new HashMap<>();
        List<ComponentItem> components = new ArrayList<>();
        
        // 解析biz文件内容
        for (String line : content) {
            // 解析组件定义
            if (line.contains("component")) {
                ComponentItem component = parseComponent(line);
                if (component != null) {
                    components.add(component);
                }
            }
            // 解析上下文信息
            if (line.contains("context") || line.contains("property")) {
                parseContext(line, context);
            }
        }
        
//        pattern.setComponents(components);
//        pattern.setContext(context);
        return pattern;
    }
    
    private ComponentItem parseComponent(String line) {
        // 使用正则表达式解析组件定义
        Pattern pattern = Pattern.compile("component\\s+([\\w-]+)\\s*\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(line);
        
        if (matcher.find()) {
            String type = matcher.group(1);
            String props = matcher.group(2);
            
            // 从PluginCache获取组件模板
            ComponentItem template = PluginCache.componentItemMap.get(type);
            if (template != null) {
                ComponentItem component = new ComponentItem(
                    template.getName(),
                    template.getIconPath(),
                    type,
                    template.isBeanEdit(),
                    template.getBeanRef(),
                    template.getMethod()
                );
                // 解析组件属性
                parseComponentProperties(props, component);
                return component;
            }
        }
        return null;
    }
    
    private void parseComponentProperties(String props, ComponentItem component) {
        // 解析组件的属性设置
        Pattern propPattern = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]+)\"");
        Matcher propMatcher = propPattern.matcher(props);
        
        while (propMatcher.find()) {
            String key = propMatcher.group(1);
            String value = propMatcher.group(2);
            // 设置组件属性
            switch (key) {
                case "name":
                    component.setName(value);
                    break;
                case "beanRef":
                    component.setBeanRef(value);
                    break;
                case "method":
                    component.setMethod(value);
                    break;
            }
        }
    }
    
    private void parseContext(String line, Map<String, String> context) {
        Pattern pattern = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(line);
        
        while (matcher.find()) {
            context.put(matcher.group(1), matcher.group(2));
        }
    }
    
    public List<ComponentItem> generateComponents(String prompt) {
        List<ComponentItem> result = new ArrayList<>();
        Set<String> requiredTypes = analyzePrompt(prompt);
        
        // 尝试找到最佳匹配的模式
        Optional<ComponentPattern> bestPattern = findBestMatchingPattern(requiredTypes);
        
        if (bestPattern.isPresent()) {
            result.addAll(generateFromPattern(bestPattern.get(), prompt));
        } else {
            // 如果没有匹配的模式,基于单个组件生成
            requiredTypes.forEach(type -> {
                ComponentItem component = generateSingleComponent(type, prompt);
                if (component != null) {
                    result.add(component);
                }
            });
        }
        
        return result;
    }
    
    private Set<String> analyzePrompt(String prompt) {
        Set<String> types = new HashSet<>();
        String lowerPrompt = prompt.toLowerCase();
        
        // 遍历所有组件类型的关键词
        typeKeywords.forEach((type, keywords) -> {
            for (String keyword : keywords) {
                if (lowerPrompt.contains(keyword.toLowerCase())) {
                    types.add(type);
                    break;
                }
            }
        });
        
        return types;
    }
    
    private Optional<ComponentPattern> findBestMatchingPattern(Set<String> requiredTypes) {
//        return patterns.stream()
//            .filter(pattern -> {
//                Set<String> patternTypes = pattern.getComponents().stream()
//                    .map(ComponentItem::getType)
//                    .collect(Collectors.toSet());
//                return patternTypes.containsAll(requiredTypes);
//            })
//            .max(Comparator.comparingInt(pattern ->
//                (int) pattern.getComponents().stream()
//                    .map(ComponentItem::getType)
//                    .filter(requiredTypes::contains)
//                    .count()
//            ));
        return null;
    }
    
    private List<ComponentItem> generateFromPattern(ComponentPattern pattern, String prompt) {
//        List<ComponentItem> result = new ArrayList<>();
//
//        // 基于模式生成组件
//        for (ComponentItem templateComponent : pattern.getComponents()) {
//            ComponentItem newComponent = new ComponentItem(
//                templateComponent.getName(),
//                templateComponent.getIconPath(),
//                templateComponent.getType(),
//                templateComponent.isBeanEdit(),
//                templateComponent.getBeanRef(),
//                templateComponent.getMethod()
//            );
//
//            // 根据提示词调整组件属性
//            customizeComponent(newComponent, prompt);
//            result.add(newComponent);
//        }
//
//        return result;
        return null;
    }
    
    private ComponentItem generateSingleComponent(String type, String prompt) {
        // 从缓存中获取组件模板
        ComponentItem template = PluginCache.componentItemMap.get(type);
        if (template == null) {
            return null;
        }
        
        // 创建新组件
        ComponentItem component = new ComponentItem(
            template.getName(),
            template.getIconPath(),
            type,
            template.isBeanEdit(),
            template.getBeanRef(),
            template.getMethod()
        );
        
        // 根据提示词调整组件属性
        customizeComponent(component, prompt);
        return component;
    }
    
    private void customizeComponent(ComponentItem component, String prompt) {
        // 根据提示词调整组件名称和属性
        String type = component.getType();
        String name = generateComponentName(type, prompt);
        component.setName(name);
        
        // 根据组件类型设置特定属性
        switch (type) {
            case "print":
                component.setBeanRef("console");
                component.setMethod("log");
                break;
            case "sql":
                component.setBeanRef("sqlExecutor");
                break;
            case "assign":
                component.setBeanRef("variableService");
                component.setMethod("assign");
                break;
            // 添加更多组件类型的定制逻辑
        }
    }
    
    private String generateComponentName(String type, String prompt) {
        // 生成组件名称
        String baseName = type.substring(0, 1).toUpperCase() + type.substring(1);
        return baseName + "_" + System.currentTimeMillis() % 1000;
    }
}

//class ComponentPattern {
//    private List<ComponentItem> components = new ArrayList<>();
//    private Map<String, String> context = new HashMap<>();
//
//    public List<ComponentItem> getComponents() {
//        return components;
//    }
//
//    public Map<String, String> getContext() {
//        return context;
//    }
//}

class ComponentUsage {
    private ComponentItem component;
    private Map<String, String> context;
    
    public ComponentUsage(ComponentItem component, Map<String, String> context) {
        this.component = component;
        this.context = context;
    }
} 