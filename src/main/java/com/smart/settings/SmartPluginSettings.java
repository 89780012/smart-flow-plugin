package com.smart.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

@State(
    name = "com.smart.settings.SmartPluginSettings",
    storages = @Storage("SmartPluginSettings.xml")
)
public class SmartPluginSettings implements PersistentStateComponent<SmartPluginSettings> {
    public static String API_DOMAIN = "https://auth.smartflow.fun";
    public static String API_DOMAIN2 = "http://auth.smartflow.fun";

    private String licenseKey = "";
    private String expiresAt;
    private boolean enableRemoteStorage = false;
    private boolean archiveTabSelected = false;
    
    // MySQL相关配置
    private String dbType = "mysql";
    private String mysqlUrl = "";
    private String mysqlUsername = "";
    private String mysqlPassword = "";
    private boolean enableSqlAiAnalysis = false;

    // OpenAI相关配置
    private boolean enableOpenAI = false;
    private String openAIBaseUrl = "";
    private String openAIAuthKey = "";
    private String openAIModel = "gpt-3.5-turbo";
    private List<String> availableModels = new ArrayList<>();

    public static SmartPluginSettings getInstance() {
        return ApplicationManager.getApplication().getService(SmartPluginSettings.class);
    }

    @Nullable
    @Override
    public SmartPluginSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SmartPluginSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isArchiveTabSelected() {
        return archiveTabSelected;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isEnableRemoteStorage() {
        return enableRemoteStorage;
    }

    public void setEnableRemoteStorage(boolean enableRemoteStorage) {
        this.enableRemoteStorage = enableRemoteStorage;
    }

    public boolean getArchiveTabSelected() {
        return archiveTabSelected;
    }

    public void setArchiveTabSelected(boolean selected) {
        this.archiveTabSelected = selected;
    }

    // MySQL相关的getter和setter
    public String getMysqlUrl() {
        return mysqlUrl;
    }

    public void setMysqlUrl(String mysqlUrl) {
        this.mysqlUrl = mysqlUrl;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public void setMysqlUsername(String mysqlUsername) {
        this.mysqlUsername = mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public void setMysqlPassword(String mysqlPassword) {
        this.mysqlPassword = mysqlPassword;
    }

    public boolean isEnableSqlAiAnalysis() {
        return enableSqlAiAnalysis;
    }

    public void setEnableSqlAiAnalysis(boolean enableSqlAiAnalysis) {
        this.enableSqlAiAnalysis = enableSqlAiAnalysis;
    }

    // OpenAI相关的getter和setter
    public boolean isEnableOpenAI() {
        return enableOpenAI;
    }

    public void setEnableOpenAI(boolean enableOpenAI) {
        this.enableOpenAI = enableOpenAI;
    }

    public String getOpenAIBaseUrl() {
        return openAIBaseUrl;
    }

    public void setOpenAIBaseUrl(String openAIBaseUrl) {
        this.openAIBaseUrl = openAIBaseUrl;
    }

    public String getOpenAIAuthKey() {
        return openAIAuthKey;
    }

    public void setOpenAIAuthKey(String openAIAuthKey) {
        this.openAIAuthKey = openAIAuthKey;
    }

    public String getOpenAIModel() {
        return openAIModel;
    }

    public void setOpenAIModel(String openAIModel) {
        this.openAIModel = openAIModel;
    }

    public List<String> getAvailableModels() {
        return availableModels;
    }

    public void setAvailableModels(List<String> availableModels) {
        this.availableModels = availableModels;
    }

    public static class State {
        public boolean archiveTabSelected = false;
        public boolean enableOpenAI = false;
        public String openAIBaseUrl = "";
        public String openAIAuthKey = "";
        public String openAIModel = "gpt-3.5-turbo";
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
}