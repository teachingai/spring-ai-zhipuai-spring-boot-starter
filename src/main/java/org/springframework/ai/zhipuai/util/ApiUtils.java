package org.springframework.ai.zhipuai.util;

import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ApiUtils {
    public final static String DEFAULT_BASE_URL = "https://open.bigmodel.cn";

    public static final Integer DEFAULT_MAX_TOKENS = 1024;

    public static final Float DEFAULT_TEMPERATURE = 0.95f;

    public static final Float DEFAULT_TOP_P = 0.7f;

    public static Consumer<HttpHeaders> getJsonContentHeaders(String apiKey) {
        return (headers) -> {
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            // headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        };
    };

    public static ChatResponse toChatResponse(String callBackId, LLMResp llmResp, boolean completion) {
        List<Generation> generations = Arrays.asList(new Generation(llmResp.getAnswer(), ApiUtils.toMap(callBackId, llmResp))
                .withGenerationMetadata( completion ? ChatGenerationMetadata.from("chat.completion", null) : ChatGenerationMetadata.NULL));
        return new ChatResponse(generations);
    }

}
