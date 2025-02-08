package com.smart.service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.smart.cache.PluginCache;
import com.smart.settings.SmartPluginSettings;

import java.util.List;
import java.util.Collections;

@Service
public final class ModelService {
    
    private static final String MODEL_LIST_URL = SmartPluginSettings.API_DOMAIN + "/v1/models";
    
    public static ModelService getInstance() {
        return ServiceManager.getService(ModelService.class);
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
            // 使用Hutool发起HTTP请求
            String result = "";
            HttpURLConnection conn = null;
            try {
                URL url = new URL(tmpModelListUrl);
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
            JSONObject json = JSONUtil.parseObj(result);
            try{
                // 获取data数组
                JSONArray modelsArray = json.getJSONArray("data");
                // 提取每个model对象的id字段
                List<String> models = modelsArray.stream()
                        .map(obj -> ((JSONObject)obj).getStr("id"))
                        .toList();

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