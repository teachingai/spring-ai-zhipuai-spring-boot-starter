package org.springframework.ai.zhipuai;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.image.CreateImageRequest;
import com.zhipu.oapi.service.v4.image.ImageApiResponse;
import com.zhipu.oapi.service.v4.image.ImageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.*;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.zhipuai.api.ZhipuAiImageOptions;
import org.springframework.ai.zhipuai.metadata.ZhipuAiImageGenerationMetadata;
import org.springframework.ai.zhipuai.metadata.ZhipuAiImageResponseMetadata;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ZhipuAiImageClient implements ImageClient {

    private final static Logger logger = LoggerFactory.getLogger(ZhipuAiImageClient.class);

    private ZhipuAiImageOptions defaultOptions;

    private final ClientV4 zhipuClient;

    public final RetryTemplate retryTemplate;

    public ZhipuAiImageClient(ClientV4 zhipuClient) {
        this(zhipuClient, ZhipuAiImageOptions.builder()
                .withModel(Constants.ModelCogView)
                .build(), RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    public ZhipuAiImageClient(ClientV4 zhipuClient, ZhipuAiImageOptions defaultOptions,
                              RetryTemplate retryTemplate) {
        Assert.notNull(zhipuClient, "ClientV4 must not be null");
        Assert.notNull(defaultOptions, "defaultOptions must not be null");
        Assert.notNull(retryTemplate, "retryTemplate must not be null");
        this.zhipuClient = zhipuClient;
        this.defaultOptions = defaultOptions;
        this.retryTemplate = retryTemplate;
    }

    public ZhipuAiImageOptions getDefaultOptions() {
        return this.defaultOptions;
    }

    @Override
    public ImageResponse call(ImagePrompt imagePrompt) {
        return this.retryTemplate.execute(ctx -> {

            var inputContent = CollectionUtils.firstElement(imagePrompt.getInstructions());
            CreateImageRequest imageRequest = new CreateImageRequest();
            imageRequest.setPrompt(inputContent.getText());

            if (this.defaultOptions != null) {
                imageRequest = ModelOptionsUtils.merge(this.defaultOptions, imageRequest, CreateImageRequest.class);
            }

            if (imagePrompt.getOptions() != null) {
                imageRequest = ModelOptionsUtils.merge(toZhipuAiImageOptions(imagePrompt.getOptions()), imageRequest,
                        CreateImageRequest.class);
            }

            ImageApiResponse imageApiResponse = zhipuClient.createImage(imageRequest);

            // Convert to org.springframework.ai.model derived ImageResponse data type
            return convertResponse(imageApiResponse, imageRequest);
        });
    }

    private ImageResponse convertResponse(ImageApiResponse imageApiResponse,
                                          CreateImageRequest imageRequest) {
        if (imageApiResponse == null) {
            logger.warn("No image response returned for request: {}", imageRequest);
            return new ImageResponse(List.of());
        }
        if (!imageApiResponse.isSuccess()) {
            logger.error("Create Image error for request，code : {}，error : {}", imageApiResponse.getCode(), imageApiResponse.getMsg());
            return new ImageResponse(List.of());
        }
        ImageResult imageResult = imageApiResponse.getData();
        if (imageResult == null) {
            logger.warn("No image response returned for request: {}", imageRequest);
            return new ImageResponse(List.of());
        }
        List<ImageGeneration> imageGenerationList = imageResult.getData().stream()
                .map(entry -> new ImageGeneration(new Image(entry.getUrl(), entry.getB64Json()), new ZhipuAiImageGenerationMetadata(entry.getRevisedPrompt())))
                .toList();

        ImageResponseMetadata imageResponseMetadata = ZhipuAiImageResponseMetadata.from(imageResult);
        return new ImageResponse(imageGenerationList, imageResponseMetadata);
    }

    /**
     * Convert the {@link ImageOptions} into {@link ZhipuAiImageOptions}.
     * @param runtimeImageOptions the image options to use.
     * @return the converted {@link ZhipuAiImageOptions}.
     */
    private ZhipuAiImageOptions toZhipuAiImageOptions(ImageOptions runtimeImageOptions) {
        ZhipuAiImageOptions.Builder builder = ZhipuAiImageOptions.builder();
        if (runtimeImageOptions != null) {
            // Handle portable image options
            if (runtimeImageOptions.getN() != null) {
                builder.withN(runtimeImageOptions.getN());
            }
            if (runtimeImageOptions.getModel() != null) {
                builder.withModel(runtimeImageOptions.getModel());
            }
            if (runtimeImageOptions.getResponseFormat() != null) {
                builder.withResponseFormat(runtimeImageOptions.getResponseFormat());
            }
            if (runtimeImageOptions.getWidth() != null) {
                builder.withWidth(runtimeImageOptions.getWidth());
            }
            if (runtimeImageOptions.getHeight() != null) {
                builder.withHeight(runtimeImageOptions.getHeight());
            }
            // Handle ZhipuAI specific image options
            if (runtimeImageOptions instanceof ZhipuAiImageOptions) {
                ZhipuAiImageOptions runtimeZhipuAiImageOptions = (ZhipuAiImageOptions) runtimeImageOptions;
                if (runtimeZhipuAiImageOptions.getQuality() != null) {
                    builder.withQuality(runtimeZhipuAiImageOptions.getQuality());
                }
                if (runtimeZhipuAiImageOptions.getStyle() != null) {
                    builder.withStyle(runtimeZhipuAiImageOptions.getStyle());
                }
                if (runtimeZhipuAiImageOptions.getUser() != null) {
                    builder.withUser(runtimeZhipuAiImageOptions.getUser());
                }
            }
        }
        return builder.build();
    }

}
