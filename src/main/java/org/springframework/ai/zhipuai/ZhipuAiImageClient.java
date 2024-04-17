package org.springframework.ai.zhipuai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.*;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.zhipuai.api.ZhipuAiImageApi;
import org.springframework.ai.zhipuai.api.ZhipuAiImageOptions;
import org.springframework.ai.zhipuai.metadata.ZhipuAiImageGenerationMetadata;
import org.springframework.ai.zhipuai.metadata.ZhipuAiImageResponseMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.List;

public class ZhipuAiImageClient implements ImageClient {

    private final static Logger logger = LoggerFactory.getLogger(ZhipuAiImageClient.class);
    private final static ZhipuAiImageGenerationMetadata DEFAULT_METADATA =  new ZhipuAiImageGenerationMetadata("");

    private ZhipuAiImageOptions defaultOptions;

    private final ZhipuAiImageApi zhipuAiImageApi;

    public final RetryTemplate retryTemplate;

    public ZhipuAiImageClient(ZhipuAiImageApi zhipuAiImageApi) {
        this(zhipuAiImageApi, ZhipuAiImageOptions.builder()
                .withModel(ZhipuAiImageApi.DEFAULT_IMAGE_MODEL)
                .build(), RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    public ZhipuAiImageClient(ZhipuAiImageApi zhipuAiImageApi, ZhipuAiImageOptions defaultOptions,
                              RetryTemplate retryTemplate) {
        Assert.notNull(zhipuAiImageApi, "ZhipuAiImageApi must not be null");
        Assert.notNull(defaultOptions, "defaultOptions must not be null");
        Assert.notNull(retryTemplate, "retryTemplate must not be null");
        this.zhipuAiImageApi = zhipuAiImageApi;
        this.defaultOptions = defaultOptions;
        this.retryTemplate = retryTemplate;
    }

    public ZhipuAiImageOptions getDefaultOptions() {
        return this.defaultOptions;
    }

    @Override
    public ImageResponse call(ImagePrompt imagePrompt) {
        return this.retryTemplate.execute(ctx -> {

            String instructions = imagePrompt.getInstructions().get(0).getText();
            ZhipuAiImageApi.ZhipuAiImageRequest imageRequest = new ZhipuAiImageApi.ZhipuAiImageRequest(instructions,
                    ZhipuAiImageApi.DEFAULT_IMAGE_MODEL);

            if (this.defaultOptions != null) {
                imageRequest = ModelOptionsUtils.merge(this.defaultOptions, imageRequest,
                        ZhipuAiImageApi.ZhipuAiImageRequest.class);
            }

            if (imagePrompt.getOptions() != null) {
                imageRequest = ModelOptionsUtils.merge(toZhipuAiImageOptions(imagePrompt.getOptions()), imageRequest,
                        ZhipuAiImageApi.ZhipuAiImageRequest.class);
            }

            // Make the request
            ResponseEntity<ZhipuAiImageApi.ZhipuAiImageResponse> imageResponseEntity = this.zhipuAiImageApi
                    .createImage(imageRequest);

            // Convert to org.springframework.ai.model derived ImageResponse data type
            return convertResponse(imageResponseEntity, imageRequest);
        });
    }

    private ImageResponse convertResponse(ResponseEntity<ZhipuAiImageApi.ZhipuAiImageResponse> imageResponseEntity,
                                          ZhipuAiImageApi.ZhipuAiImageRequest ZhipuAiImageRequest) {
        ZhipuAiImageApi.ZhipuAiImageResponse imageApiResponse = imageResponseEntity.getBody();
        if (imageApiResponse == null) {
            logger.warn("No image response returned for request: {}", ZhipuAiImageRequest);
            return new ImageResponse(List.of());
        }

        List<ImageGeneration> imageGenerationList = imageApiResponse.data().stream().map(entry -> {
            return new ImageGeneration(new Image(entry.url(), null), DEFAULT_METADATA);
        }).toList();

        ImageResponseMetadata imageResponseMetadata = ZhipuAiImageResponseMetadata.from(imageApiResponse);
        return new ImageResponse(imageGenerationList, imageResponseMetadata);
    }

    /**
     * Convert the {@link ImageOptions} into {@link ZhipuAiImageOptions}.
     * @param runtimeImageOptions the image options to use.
     * @return the converted {@link ZhipuAiImageOptions}.
     */
    private ZhipuAiImageOptions toZhipuAiImageOptions(ImageOptions runtimeImageOptions) {
        ZhipuAiImageOptions.Builder ZhipuAiImageOptionsBuilder = ZhipuAiImageOptions.builder();
        if (runtimeImageOptions != null) {
            // Handle portable image options
            if (runtimeImageOptions.getN() != null) {
                ZhipuAiImageOptionsBuilder.withN(runtimeImageOptions.getN());
            }
            if (runtimeImageOptions.getModel() != null) {
                ZhipuAiImageOptionsBuilder.withModel(runtimeImageOptions.getModel());
            }
            if (runtimeImageOptions.getResponseFormat() != null) {
                ZhipuAiImageOptionsBuilder.withResponseFormat(runtimeImageOptions.getResponseFormat());
            }
            if (runtimeImageOptions.getWidth() != null) {
                ZhipuAiImageOptionsBuilder.withWidth(runtimeImageOptions.getWidth());
            }
            if (runtimeImageOptions.getHeight() != null) {
                ZhipuAiImageOptionsBuilder.withHeight(runtimeImageOptions.getHeight());
            }
            // Handle ZhipuAI specific image options
            if (runtimeImageOptions instanceof ZhipuAiImageOptions) {
                ZhipuAiImageOptions runtimeZhipuAiImageOptions = (ZhipuAiImageOptions) runtimeImageOptions;
                if (runtimeZhipuAiImageOptions.getQuality() != null) {
                    ZhipuAiImageOptionsBuilder.withQuality(runtimeZhipuAiImageOptions.getQuality());
                }
                if (runtimeZhipuAiImageOptions.getStyle() != null) {
                    ZhipuAiImageOptionsBuilder.withStyle(runtimeZhipuAiImageOptions.getStyle());
                }
                if (runtimeZhipuAiImageOptions.getUser() != null) {
                    ZhipuAiImageOptionsBuilder.withUser(runtimeZhipuAiImageOptions.getUser());
                }
            }
        }
        return ZhipuAiImageOptionsBuilder.build();
    }

}
