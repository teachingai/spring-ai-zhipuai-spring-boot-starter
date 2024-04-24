package org.springframework.ai.zhipuai.autoconfigure;

import org.springframework.ai.zhipuai.api.ZhipuAiApi;
import org.springframework.ai.zhipuai.api.ZhipuAiChatOptions;
import org.springframework.ai.zhipuai.util.ApiUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(ZhipuAiChatProperties.CONFIG_PREFIX)
public class ZhipuAiChatProperties extends ZhipuAiParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhipuai.chat";


    /**
     * Enable ZhipuAi chat client.
     */
    private boolean enabled = true;

    /**
     * Client lever ZhipuAi options. Use this property to configure generative temperature,
     * topK and topP and alike parameters. The null values are ignored defaulting to the
     * generative's defaults.
     */
    @NestedConfigurationProperty
    private ZhipuAiChatOptions options = ZhipuAiChatOptions.builder()
            .withModel(ZhipuAiApi.ChatModel.GLM_3_TURBO.getValue())
            .withMaxToken(ApiUtils.DEFAULT_MAX_TOKENS)
            .withDoSample(Boolean.TRUE)
            .withTemperature(ApiUtils.DEFAULT_TEMPERATURE)
            .withTopP(ApiUtils.DEFAULT_TOP_P)
            .build();

    public ZhipuAiChatOptions getOptions() {
        return this.options;
    }

    public void setOptions(ZhipuAiChatOptions options) {
        this.options = options;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
