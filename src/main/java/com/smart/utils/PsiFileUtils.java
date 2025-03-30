package com.smart.utils;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.List;

public class PsiFileUtils {
    public static String extractClassName(PsiFile file) {
        PsiClass[] classes = PsiTreeUtil.getChildrenOfType(file, PsiClass.class);
        if (classes != null && classes.length > 0) {
            return classes[0].getName(); // 返回第一个类的名称
        }
        return null; // 若没有类，则返回 null
    }

    public static List<String> extractMethods(PsiFile file, String className) {
        List<String> methods = new ArrayList<>();
        PsiClass[] classes = PsiTreeUtil.getChildrenOfType(file, PsiClass.class);

        for (PsiClass psiClass : classes) {
            if (psiClass.getName().equals(className)) { // 确保类名匹配
                PsiMethod[] psiMethods = psiClass.getMethods();
                for (PsiMethod method : psiMethods) {
                    if (method.hasModifierProperty(PsiModifier.PUBLIC)) {
                        methods.add(method.getName()); // 将公共方法名称添加到列表中
                    }
                }
            }
        }
        return methods;
    }

    public static String getFullyQualifiedClassName(PsiFile file) {
        if (file != null && file.getName().endsWith(".java")) {
            String packageName = extractPackageName(file);
            String className = extractClassName(file);
            if (className != null) {
                return packageName.isEmpty() ? className : packageName + "." + className;
            }
        }
        return "";
    }
    
    private static String extractPackageName(PsiFile file) {
        String fileContent = file.getText();
        String[] lines = fileContent.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("package ")) {
                return line.trim().substring(8, line.trim().length() - 1);
            }
        }
        return "";
    }
    
}
