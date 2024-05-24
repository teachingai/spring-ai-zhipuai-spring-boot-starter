package org.springframework.ai.zhipuai.autoconfigure;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.zhipuai.api.ZhipuAiEmbeddingOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(ZhipuAiEmbeddingProperties.CONFIG_PREFIX)
public class ZhipuAiEmbeddingProperties extends ZhipuAiParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhipuai.embedding";

    public static final String DEFAULT_EMBEDDING_MODEL = ZhipuAiApi.EmbeddingModel.EMBED.getValue();

    /**
     * Enable 智普AI embedding client.
     */
    private boolean enabled = true;

    public MetadataMode metadataMode = MetadataMode.EMBED;

    /**
     * Client lever 智普AI options. Use this property to configure generative temperature,
     * topK and topP and alike parameters. The null values are ignored defaulting to the
     * generative's defaults.
     */
    @NestedConfigurationProperty
    private ZhipuAiEmbeddingOptions options = ZhipuAiEmbeddingOptions.builder()
            .withModel(DEFAULT_EMBEDDING_MODEL)
            .build();

    public ZhipuAiEmbeddingOptions getOptions() {
        return this.options;
    }

    public void setOptions(ZhipuAiEmbeddingOptions options) {
        this.options = options;
    }

    public MetadataMode getMetadataMode() {
        return this.metadataMode;
    }

    public void setMetadataMode(MetadataMode metadataMode) {
        this.metadataMode = metadataMode;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
