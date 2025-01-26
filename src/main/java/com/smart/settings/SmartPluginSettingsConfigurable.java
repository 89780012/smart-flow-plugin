package com.smart.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SmartPluginSettingsConfigurable implements Configurable {
    private SmartPluginSettingsComponent settingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Smart Flow Settings";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return getSettingsComponent().getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new SmartPluginSettingsComponent();
        reset();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        SmartPluginSettingsComponent component = getSettingsComponent();
        SmartPluginSettings settings = SmartPluginSettings.getInstance();
        
        return !component.getLicenseKey().equals(settings.getLicenseKey()) ||
               component.isEnableRemoteStorage() != settings.isEnableRemoteStorage() ||
               component.isEnableSqlAiAnalysis() != settings.isEnableSqlAiAnalysis() ||
               !component.getMysqlUrl().equals(settings.getMysqlUrl()) ||
               !component.getMysqlUsername().equals(settings.getMysqlUsername()) ||
               !component.getMysqlPassword().equals(settings.getMysqlPassword()) ||
               component.isOpenAIEnabled() != settings.isEnableOpenAI() ||
               !component.getOpenAIBaseUrl().equals(settings.getOpenAIBaseUrl()) ||
               !component.getOpenAIAuthKey().equals(settings.getOpenAIAuthKey()) ||
               !component.getOpenAIModel().equals(settings.getOpenAIModel()) ||
               !component.getAvailableModels().equals(settings.getAvailableModels());
    }

    @Override
    public void apply() {
        SmartPluginSettings settings = SmartPluginSettings.getInstance();
        SmartPluginSettingsComponent component = getSettingsComponent();
        
        settings.setLicenseKey(component.getLicenseKey());
        settings.setEnableRemoteStorage(component.isEnableRemoteStorage());
        settings.setEnableSqlAiAnalysis(component.isEnableSqlAiAnalysis());
        settings.setMysqlUrl(component.getMysqlUrl());
        settings.setMysqlUsername(component.getMysqlUsername());
        settings.setMysqlPassword(component.getMysqlPassword());
        settings.setEnableOpenAI(component.isOpenAIEnabled());
        settings.setOpenAIBaseUrl(component.getOpenAIBaseUrl());
        settings.setOpenAIAuthKey(component.getOpenAIAuthKey());
        settings.setOpenAIModel(component.getOpenAIModel());
        settings.setAvailableModels(component.getAvailableModels());
    }

    @Override
    public void reset() {
        SmartPluginSettings settings = SmartPluginSettings.getInstance();
        SmartPluginSettingsComponent component = getSettingsComponent();
        
        component.setLicenseKey(settings.getLicenseKey());
        component.setEnableRemoteStorage(settings.isEnableRemoteStorage());
        component.setEnableSqlAiAnalysis(settings.isEnableSqlAiAnalysis());
        component.setMysqlUrl(settings.getMysqlUrl());
        component.setMysqlUsername(settings.getMysqlUsername());
        component.setMysqlPassword(settings.getMysqlPassword());
        component.setOpenAIEnabled(settings.isEnableOpenAI());
        component.setOpenAIBaseUrl(settings.getOpenAIBaseUrl());
        component.setOpenAIAuthKey(settings.getOpenAIAuthKey());
        component.setOpenAIModel(settings.getOpenAIModel());
        component.setAvailableModels(settings.getAvailableModels());
        component.updateActivationStatus();
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }

    private SmartPluginSettingsComponent getSettingsComponent() {
        if (settingsComponent == null) {
            settingsComponent = new SmartPluginSettingsComponent();
        }
        return settingsComponent;
    }
} 