package org.springframework.ai.zhipuai.autoconfigure;

import org.springframework.ai.zhipuai.api.ApiUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(ZhipuAiConnectionProperties.CONFIG_PREFIX)
public class ZhipuAiConnectionProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhipuai";

    /**
     * Base URL where 智普AI API server is running.
     */
    private String baseUrl = ApiUtils.DEFAULT_BASE_URL;

    private String apiKey;

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

}
