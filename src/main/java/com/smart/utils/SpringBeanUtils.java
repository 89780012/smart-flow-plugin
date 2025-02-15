package com.smart.utils;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SpringBeanUtils {

    // 定义 FlowController 注解
    public static final String[] SPRING_ANNOTATIONS = {
        // 简单名称
        "FlowComponent",
        // 全限定名 (根据实际包名修改)
        "cc.xiaonuo.flow.annotation.FlowComponent"
    };

    private static BeanDefinition findSpringBeanAnnotation(@NotNull PsiClass psiClass) {
        // 如果类是接口或抽象类，则不是Bean
        if (psiClass.isInterface() || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return new BeanDefinition(false, null);
        }

        // 检查直接注解
        for (String annotationName : SPRING_ANNOTATIONS) {
            PsiAnnotation annotation = psiClass.getAnnotation(annotationName);
            if (annotation != null) {
                //System.out.println("找到直接注解: " + annotationName + " 在类 " + psiClass.getQualifiedName());
                return extractBeanDefinition(annotation, psiClass);
            }
        }

        // 检查元注解
//        PsiAnnotation[] classAnnotations = psiClass.getAnnotations();
//        for (PsiAnnotation annotation : classAnnotations) {
//            PsiJavaCodeReferenceElement element = annotation.getNameReferenceElement();
//            if (element == null) continue;
//
//            PsiElement resolved = element.resolve();
//            if (resolved instanceof PsiClass) {
//                PsiClass annotationClass = (PsiClass) resolved;
//                for (String springAnnotation : SPRING_ANNOTATIONS) {
//                    if (annotationClass.hasAnnotation(springAnnotation)) {
//                        System.out.println("找到元注解: " + springAnnotation + " 在类 " + psiClass.getQualifiedName());
//                        return extractBeanDefinition(annotation, psiClass);
//                    }
//                }
//            }
//        }
//
//        // 检查是否包含@Bean方法
//        if (containsBeanMethod(psiClass)) {
//            System.out.println("找到@Bean方法在类 " + psiClass.getQualifiedName());
//            return new BeanDefinition(true, getDefaultBeanName(psiClass));
//        }

        return new BeanDefinition(false, null);
    }

    private static BeanDefinition extractBeanDefinition(PsiAnnotation annotation, PsiClass psiClass) {
        String beanName = null;
        
        // 1. 先尝试获取默认值
        PsiAnnotationMemberValue defaultValue = annotation.findDeclaredAttributeValue(null);
        if (defaultValue != null && defaultValue.getText() != null && !defaultValue.getText().isEmpty()) {
            beanName = defaultValue.getText().replace("\"", "");
            if ("{}".equals(beanName)) {
                beanName = null;
            }
        }
        
        // 2. 尝试获取value属性
        if (beanName == null || beanName.isEmpty()) {
            PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("value");
            if (value != null && value.getText() != null && !value.getText().isEmpty()) {
                beanName = value.getText().replace("\"", "");
                if ("{}".equals(beanName)) {
                    beanName = null;
                }
            }
        }
        
        // 3. 尝试获取name属性
        if (beanName == null || beanName.isEmpty()) {
            PsiAnnotationMemberValue nameValue = annotation.findDeclaredAttributeValue("name");
            if (nameValue != null && nameValue.getText() != null && !nameValue.getText().isEmpty()) {
                beanName = nameValue.getText().replace("\"", "");
                if ("{}".equals(beanName)) {
                    beanName = null;
                }
            }
        }
        
        // 4. 如果没有指定名称，使用默认命名规则
        if (beanName == null || beanName.isEmpty()) {
            beanName = getDefaultBeanName(psiClass);
        }
        
       //System.out.println("提取的Bean名称: " + beanName + " 来自类 " + psiClass.getQualifiedName());
        return new BeanDefinition(true, beanName);
    }

    private static String getDefaultBeanName(PsiClass psiClass) {
        String className = psiClass.getName();
        if (className == null || className.isEmpty()) {
            throw new IllegalStateException("类名不能为空: " + psiClass.getQualifiedName());
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private static class BeanDefinition {
        final boolean isBean;
        final String name;

        BeanDefinition(boolean isBean, String name) {
            this.isBean = isBean;
            this.name = name;
        }
    }

    public static Map<String, PsiClass> getAllSpringBeanNames(@NotNull Project project) {
        if (project.isDisposed()) {
            return Collections.emptyMap();
        }
        return ReadAction.compute(() -> {
            Map<String, PsiClass> injectedBeans = Collections.synchronizedMap(new HashMap<>());
            
            Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(
                JavaFileType.INSTANCE, 
                GlobalSearchScope.projectScope(project)
            );
            
            //System.out.println("开始扫描Spring Bean，找到Java文件数量: " + javaFiles.size());
            for (VirtualFile file : javaFiles) {
                ReadAction.run(() -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile instanceof PsiJavaFile) {
                        PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
                        for (PsiClass psiClass : classes) {
                            //System.out.println("注册Bean: " + " -> " + psiClass.getQualifiedName());
                            try {
                                BeanDefinition beanDef = findSpringBeanAnnotation(psiClass);
                                //System.out.println("111注册Bean: " + beanDef.name + " -> " + psiClass.getQualifiedName());
                                if (beanDef.isBean && beanDef.name != null && !beanDef.name.isEmpty()) {
                                    synchronized (injectedBeans) {
                                        //System.out.println("222注册Bean: " + beanDef.name + " -> " + psiClass.getQualifiedName());
                                        injectedBeans.put(beanDef.name, psiClass);
                                    }
                                    // 检查@Bean方法返回的类型
                                    //findBeanMethods(psiClass, injectedBeans);
                                }
                            } catch (Exception e) {
                                //System.out.println("处理类时出错: " + psiClass.getQualifiedName() + ", 错误: " + e.getMessage());
                            }
                        }
                    }
                });
            }
            
            //System.out.println("扫描完成，找到Bean数量: " + injectedBeans.size());
            return new HashMap<>(injectedBeans);
        });
    }

    private static boolean containsBeanMethod(@NotNull PsiClass psiClass) {
        for (PsiMethod method : psiClass.getMethods()) {
            for (String annotationName : SPRING_ANNOTATIONS) {
                if (method.hasAnnotation(annotationName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void findBeanMethods(@NotNull PsiClass psiClass, Map<String, PsiClass> injectedBeans) {
        for (PsiMethod method : psiClass.getAllMethods()) {
            PsiAnnotation flowAnnotation = null;
            // 查找方法上的 FlowComponent 注解
            for (String annotationName : SPRING_ANNOTATIONS) {
                flowAnnotation = method.getAnnotation(annotationName);
                if (flowAnnotation != null) {
                    break;
                }
            }

            if (flowAnnotation != null) {
                String beanName = null;
                
                // 1. 先尝试获取默认值
                PsiAnnotationMemberValue defaultValue = flowAnnotation.findDeclaredAttributeValue(null);
                if (defaultValue != null && defaultValue.getText() != null && !defaultValue.getText().isEmpty()) {
                    beanName = defaultValue.getText().replace("\"", "");
                    if ("{}".equals(beanName)) {
                        beanName = null;
                    }
                }
                
                // 2. 如果没有默认值，尝试获取name属性
                if (beanName == null || beanName.isEmpty()) {
                    PsiAnnotationMemberValue nameValue = flowAnnotation.findDeclaredAttributeValue("name");
                    if (nameValue != null && nameValue.getText() != null && !nameValue.getText().isEmpty()) {
                        beanName = nameValue.getText().replace("\"", "");
                        if ("{}".equals(beanName)) {
                            beanName = null;
                        }
                    }
                }
                
                // 3. 如果还没有，尝试获取value属性
                if (beanName == null || beanName.isEmpty()) {
                    PsiAnnotationMemberValue value = flowAnnotation.findDeclaredAttributeValue("value");
                    if (value != null && value.getText() != null && !value.getText().isEmpty()) {
                        beanName = value.getText().replace("\"", "");
                        if ("{}".equals(beanName)) {
                            beanName = null;
                        }
                    }
                }
                
                // 4. 如果以上都没有，使用方法名作为bean名称
                if (beanName == null || beanName.isEmpty()) {
                    beanName = method.getName();
                }
                
                PsiType returnType = method.getReturnType();
                if (returnType instanceof PsiClassType) {
                    PsiClass returnClass = ((PsiClassType) returnType).resolve();
                    if (returnClass != null) {
                        injectedBeans.put(beanName, returnClass);
                    }
                }
            }
        }
    }

}
