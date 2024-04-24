package org.springframework.ai.zhipuai.autoconfigure;


import org.springframework.ai.zhipuai.util.ApiUtils;

class ZhipuAiParentProperties {

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
