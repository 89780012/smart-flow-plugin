package com.smart.service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.intellij.openapi.components.Service;
import com.smart.cache.PluginCache;
import com.smart.settings.SmartPluginSettings;
import com.intellij.openapi.application.ApplicationManager;

import java.util.List;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

@Service
public final class ModelService {

    public static ModelService getInstance() {
        return ApplicationManager.getApplication().getService(ModelService.class);
    }
    
    public void fetchAvailableModels() {
        try {
            String tmpModelListUrl = SmartPluginSettings.API_DOMAIN + "/v1/models";
            String tmpApiKey = ""; // 内置的不需要
            SmartPluginSettings instance = SmartPluginSettings.getInstance();
            boolean enableOpenAI = instance.isEnableOpenAI();
            if(enableOpenAI){ //启用
                tmpModelListUrl = SmartPluginSettings.getInstance().getOpenAIBaseUrl() + "/v1/models";
                tmpApiKey = SmartPluginSettings.getInstance().getOpenAIAuthKey();
            }
            String result = "";
            HttpURLConnection conn = null;
            try {
                URI uri = new URI(tmpModelListUrl);
                URL url = uri.toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + tmpApiKey);

                // 读取响应
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    result = response.toString();
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            // 解析JSON响应
            JSONObject json = new JSONObject(result);
            
            try{
                // 获取data数组
                JSONArray modelsArray = json.getJSONArray("data");
                List<String> models = new ArrayList<>();
                // 遍历数组提取id
                for (int i = 0; i < modelsArray.length(); i++) {
                    JSONObject modelObj = modelsArray.getJSONObject(i);
                    models.add(modelObj.getString("id"));
                }

                PluginCache.availableModels = models;
            }catch (Exception e){
                e.printStackTrace();
                PluginCache.availableModels = Collections.emptyList();
            }
        } catch (Exception e) {
            // 处理异常情况
            PluginCache.availableModels = Collections.emptyList();
            e.printStackTrace();
        }
    }

    public List<String> getAvailableModels() {
        fetchAvailableModels();
        return PluginCache.availableModels;
    }

} 