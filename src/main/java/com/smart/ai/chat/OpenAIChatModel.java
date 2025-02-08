package com.smart.ai.chat;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenAIChatModel implements ChatModel {
    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final double temperature;
    private final Duration timeout;
    private final boolean logRequests;
    private final boolean logResponses;
    
    private final HttpClient client;

    private OpenAIChatModel(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.apiKey = builder.apiKey;
        this.modelName = builder.modelName;
        this.temperature = builder.temperature;
        this.timeout = builder.timeout;
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
        
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public String generate(String prompt) {
        try {
            JSONObject requestBody = new JSONObject()
                    .put("model", modelName)
                    .put("messages", new JSONObject[]{ 
                        new JSONObject()
                            .put("role", "user")
                            .put("content", prompt)
                    })
                    .put("temperature", temperature);

            if(logRequests) {
                System.out.println("OpenAI Request: " + requestBody);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(timeout)
                    .build();

            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            if(logResponses) {
                System.out.println("OpenAI Response: " + response.body());
            }

            if(response.statusCode() != 200) {
                throw new RuntimeException("API调用失败: " + response.body());
            }

            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (Exception e) {
            throw new RuntimeException("生成回答失败: " + e.getMessage(), e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private double temperature = 0.7;
        private Duration timeout = Duration.ofSeconds(60);
        private boolean logRequests = false;
        private boolean logResponses = false;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAIChatModel build() {
            return new OpenAIChatModel(this);
        }
    }
} 