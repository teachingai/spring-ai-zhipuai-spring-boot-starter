package org.springframework.ai.zhipuai.autoconfigure;

import com.zhipu.oapi.Constants;
import org.springframework.ai.zhipuai.api.ZhipuAiImageOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(ZhipuAiImageProperties.CONFIG_PREFIX)
public class ZhipuAiImageProperties extends ZhipuAiParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhipuai.image";

    /**
     * Enable 智普AI image client.
     */
    private boolean enabled = true;

    /**
     * Client lever 智普AI options. Use this property to configure generative temperature,
     * topK and topP and alike parameters. The null values are ignored defaulting to the
     * generative's defaults.
     */
    @NestedConfigurationProperty
    private ZhipuAiImageOptions options = ZhipuAiImageOptions.builder()
            .withModel(Constants.ModelCogView)
            .build();

    public ZhipuAiImageOptions getOptions() {
        return this.options;
    }

    public void setOptions(ZhipuAiImageOptions options) {
        this.options = options;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
