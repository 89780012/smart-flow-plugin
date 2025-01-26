package com.smart.ai;

import com.smart.bean.ComponentItem;
import java.util.*;

public class ComponentPattern {
//    private List<ComponentItem> components = new ArrayList<>();
//    private Map<String, String> context = new HashMap<>();
//
//    public List<ComponentItem> getComponents() {
//        return components;
//    }
//
//    public void setComponents(List<ComponentItem> components) {
//        this.components = components;
//    }
//
//    public Map<String, String> getContext() {
//        return context;
//    }
//
//    public void setContext(Map<String, String> context) {
//        this.context = context;
//    }
//
//    public double calculateSimilarity(Set<String> requiredTypes) {
//        Set<String> patternTypes = new HashSet<>();
//        components.forEach(comp -> patternTypes.add(comp.getType()));
//
//        // 计算交集大小
//        Set<String> intersection = new HashSet<>(patternTypes);
//        intersection.retainAll(requiredTypes);
//
//        // 计算相似度
//        return (double) intersection.size() / Math.max(patternTypes.size(), requiredTypes.size());
//    }
} 