package org.springframework.ai.zhipuai.autoconfigure;


import org.springframework.ai.zhipuai.util.ApiUtils;

class ZhipuAiParentProperties {

    private String apiKey;

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

}
