package com.smart.startup;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.smart.ai.agent.DatabaseAIService;
import com.smart.cache.PluginCache;
import com.smart.service.LicenseService;
import com.smart.service.ModelService;
import com.smart.settings.SmartPluginSettings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.application.ApplicationManager;

public class PluginStartupActivity implements StartupActivity {
    private static final Logger logger = Logger.getInstance(PluginStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        // 验证授权
        LicenseService.getInstance().validateLicense();

        // 初始化 PluginCache
        initPluginCache();

        // 如果未激活，显示通知
        LicenseService.getInstance().showActivationNotification(project);

        // 获取可用模型列表
        //ModelService.getInstance().fetchAvailableModels();
        SmartPluginSettings settings = SmartPluginSettings.getInstance();
        if (settings.isEnableSqlAiAnalysis()) {
            // 初始化 AI agent
            DatabaseAIService.init();
        }
    }

    private void initPluginCache() {
        SmartPluginSettings settings = SmartPluginSettings.getInstance();
        PluginCache.enableRemoteStorage = settings.isEnableRemoteStorage();
        PluginCache.enableSqlAiAnalysis = settings.isEnableSqlAiAnalysis();
    }

}