package com.smart.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.smart.cache.PluginCache;
import com.smart.service.LicenseService;
import com.smart.settings.SmartPluginSettings;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PluginStartupActivity implements ProjectActivity {
    private void initPluginCache() {
        SmartPluginSettings settings = SmartPluginSettings.getInstance();
        PluginCache.enableRemoteStorage = settings.isEnableRemoteStorage();
    }

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 验证授权
        LicenseService.getInstance().validateLicense();

        // 初始化 PluginCache
        initPluginCache();

        // 如果未激活，显示通知
        LicenseService.getInstance().showActivationNotification(project);

        return null;
    }
}