package com.smart.service;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.smart.cache.PluginCache;
import com.smart.settings.SmartPluginSettings;
import com.smart.settings.SmartPluginSettingsConfigurable;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

import java.net.NetworkInterface;
import java.util.UUID;

public final class LicenseService {
    private static final String LICENSE_CHECK_URL = SmartPluginSettings.API_DOMAIN+ "/auth-codes/activate";
    private String expiresAt;
    private static final NotificationGroup NOTIFICATION_GROUP = 
            NotificationGroupManager.getInstance().getNotificationGroup("Smart Flow Notifications");

    public static LicenseService getInstance() {
        return ApplicationManager.getApplication().getService(LicenseService.class);
    }

    public boolean validateLicense() {
        return validateLicense(SmartPluginSettings.getInstance().getLicenseKey());
    }

    // 添加获取设备ID的辅助方法
    private String getDeviceId() {
        try {
            NetworkInterface network = NetworkInterface.getNetworkInterfaces().nextElement();
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (Exception e) {
            // 如果获取失败，返回一个默认值或随机值
            return UUID.randomUUID().toString();
        }
    }

    public boolean validateLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            return false;
        }

        try {
            // 这里替换为实际的授权验证逻辑
            String response = HttpUtil.post(LICENSE_CHECK_URL, JSONUtil.createObj()
                    .set("code", licenseKey)
                    .set("device_id", getDeviceId())
                    .toString());

            boolean valid = JSONUtil.parseObj(response).getBool("valid", false);
            PluginCache.isValidLicense = valid;
            
            if (valid) {
                // 保存有效的授权码和到期时间
                expiresAt = JSONUtil.parseObj(response).getStr("expires_at");
                SmartPluginSettings.getInstance().setLicenseKey(licenseKey);
                SmartPluginSettings.getInstance().setExpiresAt(expiresAt);
            }
            return valid;
        } catch (Exception e) {
            return false;
        }
    }

    public void showActivationNotification(Project project) {
        if (!PluginCache.isValidLicense) {
            Notification notification = NOTIFICATION_GROUP.createNotification(
                "Smart Plugin 需要激活",
                "插件未激活，点击此处进行激活",
                NotificationType.WARNING
            );
            
            notification.addAction(NotificationAction.create("马上激活", (e) -> {
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    project, 
                    SmartPluginSettingsConfigurable.class
                );
                notification.expire();
            }));
            
            Notifications.Bus.notify(notification, project);
        }
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}