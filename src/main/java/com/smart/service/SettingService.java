package com.smart.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
@State(
    name = "SmartFlowSettings",
    storages = {@Storage("smartflow-settings.xml")}
)
public final class SettingService implements PersistentStateComponent<SettingService.State> {
    private State myState = new State();

    public static SettingService getInstance(Project project) {
        return project.getService(SettingService.class);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public static class State {
        public String baseUrl = "";
        public boolean useHttps = false;
    }

    public String getBaseUrl() {
        return myState.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        myState.baseUrl = baseUrl;
    }

    public boolean isUseHttps() {
        return myState.useHttps;
    }

    public void setUseHttps(boolean useHttps) {
        myState.useHttps = useHttps;
    }

} 