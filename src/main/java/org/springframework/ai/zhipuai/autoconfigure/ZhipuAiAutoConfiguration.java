package org.springframework.ai.zhipuai.autoconfigure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.core.cache.ICache;
import org.springframework.ai.autoconfigure.retry.SpringAiRetryAutoConfiguration;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.ai.zhipuai.ZhipuAiChatClient;
import org.springframework.ai.zhipuai.ZhipuAiEmbeddingClient;
import org.springframework.ai.zhipuai.ZhipuAiFineTuningClient;
import org.springframework.ai.zhipuai.ZhipuAiImageClient;
import org.springframework.ai.zhipuai.api.ZhipuAiImageApi;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * {@link AutoConfiguration Auto-configuration} for 智普AI Chat Client.
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@EnableConfigurationProperties({ ZhipuAiChatProperties.class, ZhipuAiConnectionProperties.class, ZhipuAiEmbeddingProperties.class, ZhipuAiImageProperties.class })
@ConditionalOnClass(ClientV4.class)
public class ZhipuAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ZhipuAiChatProperties.CONFIG_PREFIX, name = "enabled")
    public ZhipuAiChatClient zhipuAiChatClient(ZhipuAiConnectionProperties connectionProperties,
                                               ZhipuAiChatProperties chatProperties,
                                               List<FunctionCallback> toolFunctionCallbacks,
                                               FunctionCallbackContext functionCallbackContext,
                                               ObjectProvider<ICache> cacheProvider,
                                               ObjectProvider<RetryTemplate> retryTemplateProvider) {
        if (!CollectionUtils.isEmpty(toolFunctionCallbacks)) {
            chatProperties.getOptions().getFunctionCallbacks().addAll(toolFunctionCallbacks);
        }

        String apiKey = StringUtils.hasText(chatProperties.getApiKey()) ? chatProperties.getApiKey() : connectionProperties.getApiKey();
        Assert.hasText(apiKey, "ZhipuAI API key must be set");

        ClientV4 zhipuClient = new ClientV4.Builder(apiKey).tokenCache(cacheProvider.getIfAvailable()).build();

        RetryTemplate retryTemplate = retryTemplateProvider.getIfAvailable(() -> RetryTemplate.builder().build());
        return new ZhipuAiChatClient(zhipuClient, chatProperties.getOptions(), functionCallbackContext, retryTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ZhipuAiEmbeddingProperties.CONFIG_PREFIX, name = "enabled")
    public ZhipuAiEmbeddingClient zhipuAiEmbeddingClient(ZhipuAiConnectionProperties connectionProperties,
                                                         ZhipuAiEmbeddingProperties embeddingProperties,
                                                         ObjectProvider<ICache> cacheProvider,
                                                         ObjectProvider<RetryTemplate> retryTemplateProvider) {

        String apiKey = StringUtils.hasText(embeddingProperties.getApiKey()) ? embeddingProperties.getApiKey() : connectionProperties.getApiKey();
        Assert.hasText(apiKey, "ZhipuAI API key must be set");

        ClientV4 zhipuClient = new ClientV4.Builder(apiKey).tokenCache(cacheProvider.getIfAvailable()).build();
        RetryTemplate retryTemplate = retryTemplateProvider.getIfAvailable(() -> RetryTemplate.builder().build());
        return new ZhipuAiEmbeddingClient(zhipuClient, embeddingProperties.getMetadataMode(), embeddingProperties.getOptions(), retryTemplate);
    }
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ZhipuAiFineTuningProperties.CONFIG_PREFIX, name = "enabled")
    public ZhipuAiFineTuningClient zhipuAiFineTuningClient(ZhipuAiConnectionProperties connectionProperties,
                                                           ZhipuAiFineTuningProperties fineTuningProperties,
                                                           ObjectProvider<ICache> cacheProvider,
                                                           ObjectProvider<RetryTemplate> retryTemplateProvider) {

        String apiKey = StringUtils.hasText(fineTuningProperties.getApiKey()) ? fineTuningProperties.getApiKey() : connectionProperties.getApiKey();
        Assert.hasText(apiKey, "ZhipuAI API key must be set");

        ClientV4 zhipuClient = new ClientV4.Builder(apiKey).tokenCache(cacheProvider.getIfAvailable()).build();
        RetryTemplate retryTemplate = retryTemplateProvider.getIfAvailable(() -> RetryTemplate.builder().build());
        return new ZhipuAiFineTuningClient(zhipuClient, retryTemplate);
    }


    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ZhipuAiImageProperties.CONFIG_PREFIX, name = "enabled")
    public ZhipuAiImageClient zhipuAiImageClient(ZhipuAiConnectionProperties connectionProperties,
                                                 ZhipuAiImageProperties imageProperties,
                                                 RestClient.Builder restClientBuilder,
                                                 ResponseErrorHandler responseErrorHandler,
                                                 ObjectProvider<RetryTemplate> retryTemplateProvider) {

        String baseUrl = StringUtils.hasText(imageProperties.getBaseUrl()) ? imageProperties.getBaseUrl() : connectionProperties.getBaseUrl();
        String apiKey = StringUtils.hasText(imageProperties.getApiKey()) ? imageProperties.getApiKey() : connectionProperties.getApiKey();
        Assert.hasText(baseUrl, "ZhipuAI base URL must be set");
        Assert.hasText(apiKey, "ZhipuAI API key must be set");

        ZhipuAiImageApi zhipuAiImageApi = new ZhipuAiImageApi(baseUrl, apiKey, restClientBuilder, responseErrorHandler);

        RetryTemplate retryTemplate = retryTemplateProvider.getIfAvailable(() -> RetryTemplate.builder().build());
        return new ZhipuAiImageClient(zhipuAiImageApi, imageProperties.getOptions(), retryTemplate);
    }



    @Bean
    @ConditionalOnMissingBean
    public FunctionCallbackContext springAiFunctionManager(ApplicationContext context) {
        FunctionCallbackContext manager = new FunctionCallbackContext();
        manager.setApplicationContext(context);
        return manager;
    }

    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

}
