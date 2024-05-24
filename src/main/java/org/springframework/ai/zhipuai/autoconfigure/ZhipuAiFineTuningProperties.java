package org.springframework.ai.zhipuai.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(ZhipuAiFineTuningProperties.CONFIG_PREFIX)
public class ZhipuAiFineTuningProperties extends ZhipuAiParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhipuai.fine-tuning";

    /**
     * Enable 智普AI fine-tuning client.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
