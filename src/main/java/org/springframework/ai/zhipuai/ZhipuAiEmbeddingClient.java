package org.springframework.ai.zhipuai;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.embedding.EmbeddingResult;
import com.zhipu.oapi.service.v4.model.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.*;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.zhipuai.api.ZhipuAiEmbeddingOptions;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

public class ZhipuAiEmbeddingClient extends AbstractEmbeddingClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ZhipuAiEmbeddingOptions defaultOptions;

    private final MetadataMode metadataMode;

    /**
     * 智普 SDK library.
     */
    private final ClientV4 zhipuClient;

    private final RetryTemplate retryTemplate;

    public ZhipuAiEmbeddingClient(ClientV4 zhipuClient) {
        this(zhipuClient, MetadataMode.EMBED);
    }

    public ZhipuAiEmbeddingClient(ClientV4 zhipuClient, MetadataMode metadataMode) {
        this(zhipuClient, metadataMode, ZhipuAiEmbeddingOptions.builder().withModel(Constants.ModelEmbedding2).build(),
                RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    public ZhipuAiEmbeddingClient(ClientV4 zhipuClient, ZhipuAiEmbeddingOptions options) {
        this(zhipuClient, MetadataMode.EMBED, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    public ZhipuAiEmbeddingClient(ClientV4 zhipuClient, MetadataMode metadataMode,
                                  ZhipuAiEmbeddingOptions options, RetryTemplate retryTemplate) {
        Assert.notNull(zhipuClient, "ClientV4 must not be null");
        Assert.notNull(metadataMode, "metadataMode must not be null");
        Assert.notNull(options, "options must not be null");
        Assert.notNull(retryTemplate, "retryTemplate must not be null");

        this.zhipuClient = zhipuClient;
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
                    ? new com.zhipu.oapi.service.v4.embedding.EmbeddingRequest(this.defaultOptions.getModel(), inputContent, "")
                    : new com.zhipu.oapi.service.v4.embedding.EmbeddingRequest(Constants.ModelEmbedding2, inputContent, "");

            if (request.getOptions() != null && !EmbeddingOptions.EMPTY.equals(request.getOptions())) {
                apiRequest = ModelOptionsUtils.merge(request.getOptions(), apiRequest, com.zhipu.oapi.service.v4.embedding.EmbeddingRequest.class);
            }


            var apiEmbeddingResponse = zhipuClient.invokeEmbeddingsApi(apiRequest);;
            if (Objects.isNull(apiEmbeddingResponse)) {
                logger.warn("No embeddings returned for request: {}", request);
                return new EmbeddingResponse(List.of());
            }
            if (apiEmbeddingResponse.isSuccess()) {
                logger.error("embeddings error for request，code : {}，error : {}", apiEmbeddingResponse.getCode(), apiEmbeddingResponse.getMsg());
                return new EmbeddingResponse(List.of());
            }

            EmbeddingResult embeddingResult = apiEmbeddingResponse.getData();
            if (Objects.isNull(embeddingResult)) {
                logger.warn("No embeddings returned for request: {}", request);
                return new EmbeddingResponse(List.of());
            }

            var metadata = generateResponseMetadata(embeddingResult.getModel(), embeddingResult.getUsage());

            var embeddings = embeddingResult.getData()
                    .stream()
                    .map(e -> new Embedding(e.getEmbedding(), e.getIndex()))
                    .toList();

            return new EmbeddingResponse(embeddings, metadata);

        });
    }

    private EmbeddingResponseMetadata generateResponseMetadata(String model, Usage usage) {
        var metadata = new EmbeddingResponseMetadata();
        metadata.put("model", model);
        metadata.put("prompt-tokens", usage.getPromptTokens());
        metadata.put("completion-tokens", usage.getCompletionTokens());
        metadata.put("total-tokens", usage.getTotalTokens());
        return metadata;
    }

}
