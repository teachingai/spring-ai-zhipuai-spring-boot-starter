package org.springframework.ai.zhipuai.autoconfigure;

import org.springframework.ai.autoconfigure.retry.SpringAiRetryAutoConfiguration;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.ai.zhipuai.ZhipuAiChatClient;
import org.springframework.ai.zhipuai.ZhipuAiEmbeddingClient;
import org.springframework.ai.zhipuai.api.ZhipuAiApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * {@link AutoConfiguration Auto-configuration} for 智普AI Chat Client.
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@EnableConfigurationProperties({ ZhipuAiChatProperties.class, ZhipuAiConnectionProperties.class, ZhipuAiEmbeddingProperties.class })
@ConditionalOnClass(ZhipuAiApi.class)
public class ZhipuAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZhipuAiApi zhipuAiApi(ZhipuAiConnectionProperties properties, RestClient.Builder restClientBuilder, ResponseErrorHandler responseErrorHandler) {

        Assert.hasText(properties.getApiKey(), "ZhipuAI API key must be set");
        Assert.hasText(properties.getBaseUrl(), "ZhipuAI base URL must be set");

        return new ZhipuAiApi(properties.getBaseUrl(), properties.getApiKey(), restClientBuilder, responseErrorHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ZhipuAiChatProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
    public ZhipuAiChatClient zhipuAiChatClient(ZhipuAiApi zhipuAiApi,
                                                 ZhipuAiChatProperties chatProperties,
                                                 List<FunctionCallback> toolFunctionCallbacks,
                                                 FunctionCallbackContext functionCallbackContext,
                                                 RetryTemplate retryTemplate) {
        if (!CollectionUtils.isEmpty(toolFunctionCallbacks)) {
            chatProperties.getOptions().getFunctionCallbacks().addAll(toolFunctionCallbacks);
        }
        return new ZhipuAiChatClient(zhipuAiApi, chatProperties.getOptions(), functionCallbackContext, retryTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ZhipuAiEmbeddingProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
    public ZhipuAiEmbeddingClient zhipuAiEmbeddingClient(ZhipuAiApi zhipuAiApi,
                                                         ZhipuAiEmbeddingProperties embeddingProperties,
                                                         RetryTemplate retryTemplate) {

        return new ZhipuAiEmbeddingClient(zhipuAiApi, embeddingProperties.getMetadataMode(), embeddingProperties.getOptions(), retryTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionCallbackContext springAiFunctionManager(ApplicationContext context) {
        FunctionCallbackContext manager = new FunctionCallbackContext();
        manager.setApplicationContext(context);
        return manager;
    }

}
