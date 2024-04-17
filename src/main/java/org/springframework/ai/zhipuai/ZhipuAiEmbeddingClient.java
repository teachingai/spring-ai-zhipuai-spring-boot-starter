package org.springframework.ai.zhipuai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.*;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.zhipuai.api.ZhipuAiApi;
import org.springframework.ai.zhipuai.api.ZhipuAiEmbeddingOptions;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ZhipuAiEmbeddingClient extends AbstractEmbeddingClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ZhipuAiEmbeddingOptions defaultOptions;

    private final MetadataMode metadataMode;

    /**
     * Low-level 智普 API library.
     */
    private final ZhipuAiApi zhipuAiApi;

    private final RetryTemplate retryTemplate;

    public ZhipuAiEmbeddingClient(ZhipuAiApi zhipuAiApi) {
        this(zhipuAiApi, MetadataMode.EMBED);
    }

    public ZhipuAiEmbeddingClient(ZhipuAiApi zhipuAiApi, MetadataMode metadataMode) {
        this(zhipuAiApi, metadataMode, ZhipuAiEmbeddingOptions.builder()
                        .withModel(ZhipuAiApi.EmbeddingModel.EMBED.getValue()).build(),
                RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    public ZhipuAiEmbeddingClient(ZhipuAiApi zhipuAiApi, ZhipuAiEmbeddingOptions options) {
        this(zhipuAiApi, MetadataMode.EMBED, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    public ZhipuAiEmbeddingClient(ZhipuAiApi zhipuAiApi, MetadataMode metadataMode,
                                  ZhipuAiEmbeddingOptions options, RetryTemplate retryTemplate) {
        Assert.notNull(zhipuAiApi, "ZhipuAiApi must not be null");
        Assert.notNull(metadataMode, "metadataMode must not be null");
        Assert.notNull(options, "options must not be null");
        Assert.notNull(retryTemplate, "retryTemplate must not be null");

        this.zhipuAiApi = zhipuAiApi;
        this.metadataMode = metadataMode;
        this.defaultOptions = options;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public List<Double> embed(Document document) {
        Assert.notNull(document, "Document must not be null");
        return this.embed(document.getFormattedContent(this.metadataMode));
    }

    @Override
    public EmbeddingResponse call(org.springframework.ai.embedding.EmbeddingRequest request) {
        return this.retryTemplate.execute(ctx -> {

            Assert.notEmpty(request.getInstructions(), "At least one text is required!");
            if (request.getInstructions().size() != 1) {
                logger.warn( "ZhipuAi Embedding does not support batch embedding. Will make multiple API calls to embed(Document)");
            }
            var inputContent = CollectionUtils.firstElement(request.getInstructions());
            var apiRequest = (this.defaultOptions != null)
                    ? new ZhipuAiApi.EmbeddingRequest(inputContent, this.defaultOptions.getModel())
                    : new ZhipuAiApi.EmbeddingRequest(inputContent, ZhipuAiApi.EmbeddingModel.EMBED.getValue());

            if (request.getOptions() != null && !EmbeddingOptions.EMPTY.equals(request.getOptions())) {
                apiRequest = ModelOptionsUtils.merge(request.getOptions(), apiRequest, ZhipuAiApi.EmbeddingRequest.class);
            }

            var apiEmbeddingResponse = this.zhipuAiApi.embeddings(apiRequest).getBody();

            if (apiEmbeddingResponse == null) {
                logger.warn("No embeddings returned for request: {}", request);
                return new EmbeddingResponse(List.of());
            }

            var metadata = generateResponseMetadata(apiEmbeddingResponse.model(), apiEmbeddingResponse.usage());

            var embeddings = apiEmbeddingResponse.data()
                    .stream()
                    .map(e -> new Embedding(e.embedding(), e.index()))
                    .toList();

            return new EmbeddingResponse(embeddings, metadata);

        });
    }

    private EmbeddingResponseMetadata generateResponseMetadata(String model, ZhipuAiApi.Usage usage) {
        var metadata = new EmbeddingResponseMetadata();
        metadata.put("model", model);
        metadata.put("prompt-tokens", usage.promptTokens());
        metadata.put("total-tokens", usage.totalTokens());
        return metadata;
    }

}
